package yhsb.base.json

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

class JsonField {
  private[base] var _value: String = null

  def value = _value

  def name = {
    if (valueMap.isDefinedAt(value)) {
      valueMap(value)
    } else {
      s"未知值: $value"
    }
  }

  def valueMap: PartialFunction[String, String] = PartialFunction.empty
  
  override def toString(): String = name
}

trait JsonAdapter[T] extends JsonSerializer[T] with JsonDeserializer[T]

class JsonFieldAdapter extends JsonAdapter[JsonField] {
  def serialize(
      src: JsonField,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement = new JsonPrimitive(src._value)

  def deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): JsonField = {
    val clazz = typeOfT.asInstanceOf[Class[_]]
    val field = clazz.getConstructor().newInstance().asInstanceOf[JsonField]
    field._value = json.getAsString()
    field
  }
}

object Json {
  private[json] val gson = new GsonBuilder()
    .serializeNulls()
    .registerTypeHierarchyAdapter(classOf[JsonField], new JsonFieldAdapter)
    .create()

  def fromJson[T: ClassTag](json: String): T =
    gson.fromJson(json, classTag[T].runtimeClass)

  def fromJson[T](json: String, typeOfT: Type): T = gson.fromJson(json, typeOfT)

  def toJson[T](obj: T): String = gson.toJson(obj)

  type JsonName = SerializedName @scala.annotation.meta.field
}

trait Jsonable {
  def toJson: String = Json.toJson(this)

  override def toString(): String = toJson
}
