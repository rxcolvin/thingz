package meta

import lang.Email
import lang.JSON
import java.util.*
import kotlin.reflect.KClass

interface Type<T: Any> {
  val typeName: String
}

interface AtomicType<T : Any> : Type<T> {
  val tType: KClass<T>
}

//interface ComplexType : Type {
//}

interface AtomicListType<T> : Type<List<T>> {
  val minSize : Int
  val maxSize : Int
}


interface AtomicMapType<K : Any, E : Any>: Type<Map<K, E>> {
  val keyType: AtomicType<K>
  val valueType: AtomicType<E>
}

//interface ComplexMapType: Type
//
//interface AtomicListMapType : Type
//
//interface ComplexListMapType : Type

// Probably not allow this
// interface GenericMapType : Type


//Use in association objects
//interface RefType: Type

// Is this needed?
//interface RefList: Type

class FStringType(
    val length: Int
) : AtomicType<String> {
  override val typeName = "FString-" + length
  override val tType = String::class
}

object StringType
  : AtomicType<String> {
  override val typeName = "String"
  override val tType = String::class
}

object IntType : AtomicType<Int> {
  override val tType = Int::class
  override val typeName = "Int"
}

object EmailType : AtomicType<Email> {
  override val tType = Email::class
  override val typeName = "Email"
}

object JSONType : AtomicType<JSON> {
  override val tType = JSON::class
  override val typeName = "Json"
}


object UUIDType : AtomicType<UUID> {
  override val tType = UUID::class
  override val typeName = "UUID"
}

class Field<X:Any,T: Type<X>>(
  val fieldName: String,
  val type: T
)

class EntityType<E:Any, K: Any>  (
    override val typeName: String,
    val identityField: Field<K, out AtomicType<K>>,
    val fields: List<Field<*, *>>
): Type<E>

open class ComplexType<E:Any>  (
  override val typeName: String,
  val fields: List<Field<*, *>>
): Type<E> {
  fun
}

interface EntityListType<E> : Type<List<E>> {

}

