package yhsb.util.json

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

class JsonField(val value: String) {
  def name = s"未知值: $value"
  override def toString(): String = name
}

trait JsonAdapter[T] extends JsonSerializer[T] with JsonDeserializer[T]

class JsonFieldAdapter extends JsonAdapter[JsonField] {
  def serialize(
      src: JsonField,
      typeOfSrc: Type,
      context: JsonSerializationContext
  ): JsonElement = new JsonPrimitive(src.value)

  def deserialize(
      json: JsonElement,
      typeOfT: Type,
      context: JsonDeserializationContext
  ): JsonField = new JsonField(json.getAsString())
}

object Json {
  private[json] val gson = new GsonBuilder()
    .serializeNulls()
    .registerTypeAdapter(classOf[JsonField], new JsonFieldAdapter)
    .create()

  def fromJson[T: ClassTag](json: String): T =
    gson.fromJson(json, classTag[T].runtimeClass)

  def fromJson[T](json: String, typeOfT: Type): T = gson.fromJson(json, typeOfT)

  def toJson[T](obj: T): String = gson.toJson(obj)
}

class Jsonable {
  def toJson: String = Json.toJson(this)
  override def toString(): String = toJson
}
