package yhsb.base
package json

import com.google.gson.annotations.SerializedName
import com.google.gson._
import yhsb.base.struct.{ListField, MapField}

import java.lang.reflect.{ParameterizedType, Type}
import scala.reflect.{ClassTag, classTag}

trait JsonAdapter[T] extends JsonSerializer[T] with JsonDeserializer[T]

class BigDecimalAdapter extends JsonAdapter[BigDecimal] {
  def serialize(
      src: BigDecimal,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement = new JsonPrimitive(src.bigDecimal)

  def deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): BigDecimal = BigDecimal(json.getAsBigDecimal())
}

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
    field._value = json.getAsString
    field
  }
}

class ListFieldAdapter extends JsonAdapter[ListField[_]] {
  def serialize(
      src: ListField[_],
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement = context.serialize(src.items.toArray)

  def deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): ListField[_] = {
    val paramType = typeOfT.asInstanceOf[ParameterizedType]
    val rawClass = paramType.getRawType.asInstanceOf[Class[_]]
    val argClass = paramType.getActualTypeArguments()(0).asInstanceOf[Class[_]]
    // println(s"$paramType $rawClass $argClass")

    val field = rawClass.getConstructor().newInstance().asInstanceOf[ListField[_]]

    json match {
      case array: JsonArray =>
        array.forEach {
          case obj: JsonObject if obj.size() > 0 =>
            try {
              field.items.addOne(Json.fromJson(obj, argClass))
            } catch {
              case e: JsonSyntaxException => field.addUnknownOne(obj)
            }
          case _ =>
        }
      case _ =>
    }

    field
  }
}

object Json {
  private[json] lazy val gson = new GsonBuilder()
    .serializeNulls()
    .registerTypeHierarchyAdapter(classOf[BigDecimal], new BigDecimalAdapter)
    .registerTypeHierarchyAdapter(classOf[MapField], new MapFieldAdapter)
    .registerTypeHierarchyAdapter(classOf[ListField[_]], new ListFieldAdapter)
    .create()

  private[json] lazy val gsonNoNulls = new GsonBuilder()
    .registerTypeHierarchyAdapter(classOf[BigDecimal], new BigDecimalAdapter)
    .registerTypeHierarchyAdapter(classOf[MapField], new MapFieldAdapter)
    .registerTypeHierarchyAdapter(classOf[ListField[_]], new ListFieldAdapter)
    .create()

  def fromJson[T: ClassTag](json: String): T =
    gson.fromJson(json, classTag[T].runtimeClass)

  def fromJson[T](json: String, typeOfT: Type): T = gson.fromJson(json, typeOfT)

  def fromJson[T](elem: JsonElement, typeOfT: Type): T = gson.fromJson(elem, typeOfT)

  def toJson[T](obj: T): String = gson.toJson(obj)

  def toJsonNoNulls[T](obj: T): String = gsonNoNulls.toJson(obj)

  type JsonName = SerializedName @scala.annotation.meta.field
}

trait Jsonable {
  def toJson: String = Json.toJson(this)

  def toJsonNoNulls: String = Json.toJsonNoNulls(this)

  override def toString: String = toJson
}
