package yhsb.base
package json

import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializer
import com.google.gson.{JsonElement, JsonSerializationContext}
import java.lang.reflect.Type
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonPrimitive
import com.google.gson.JsonParseException
import com.google.gson.GsonBuilder
import scala.reflect.ClassTag
import scala.reflect.classTag
import com.google.gson.annotations.SerializedName

import struct.{MapField, ListField}
import java.lang.reflect.ParameterizedType
import com.google.gson.JsonArray
import com.google.gson.JsonObject

trait JsonAdapter[T] extends JsonSerializer[T] with JsonDeserializer[T]

class MapFieldAdapter extends JsonAdapter[MapField] {
  def serialize(
      src: MapField,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement = new JsonPrimitive(src._value)

  def deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): MapField = {
    val clazz = typeOfT.asInstanceOf[Class[_]]
    val field = clazz.getConstructor().newInstance().asInstanceOf[MapField]
    field._value = json.getAsString()
    field
  }
}

class ListFieldAdapter extends JsonAdapter[ListField[_]] {
  def serialize(
      src: ListField[_],
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement = context.serialize(src.items)

  def deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): ListField[_] = {
    val paramType = typeOfT.asInstanceOf[ParameterizedType]
    val rawClass = paramType.getRawType().asInstanceOf[Class[_]]
    val argClass = paramType.getActualTypeArguments().asInstanceOf[Class[_]]

    val field = rawClass.getConstructor().newInstance().asInstanceOf[ListField[_]]

    json match {
      case array: JsonArray => {
        array.forEach {
          _ match {
            case obj: JsonObject if obj.size() > 0 => field.items.addOne(Json.fromJson(obj, argClass))
          }
        }
      }
    }

    field
  }
}

object Json {
  private[json] val gson = new GsonBuilder()
    .serializeNulls()
    .registerTypeHierarchyAdapter(classOf[MapField], new MapFieldAdapter)
    .registerTypeHierarchyAdapter(classOf[ListField[_]], new ListFieldAdapter)
    .create()

  def fromJson[T: ClassTag](json: String): T =
    gson.fromJson(json, classTag[T].runtimeClass)

  def fromJson[T](json: String, typeOfT: Type): T = gson.fromJson(json, typeOfT)

  def fromJson[T](elem: JsonElement, typeOfT: Type) = gson.fromJson(elem, typeOfT)

  def toJson[T](obj: T): String = gson.toJson(obj)

  type JsonName = SerializedName @scala.annotation.meta.field
}

trait Jsonable {
  def toJson: String = Json.toJson(this)

  override def toString(): String = toJson
}
