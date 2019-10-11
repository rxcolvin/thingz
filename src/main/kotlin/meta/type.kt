package meta

import lang.Email
import lang.JSON
import logging.configToLogger
import java.util.*
import kotlin.reflect.KClass

interface Type<T : Any> {
    val typeName: String
}

enum class AtomicJsonType {
    STRING, NUMBER, BOOLEAN

}

class  AtomicType<T : Any> (
    override val typeName: String,
    val tType: KClass<T>,
    val jsonType: AtomicJsonType,
    val asString : (T) -> String,
    val fromString: (String) -> T

) : Type<T> {

}

//interface ComplexType : Type {
//}

interface AtomicListType<T> : Type<List<T>> {
    val minSize: Int
    val maxSize: Int
}


interface AtomicMapType<K : Any, E : Any> : Type<Map<K, E>> {
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

//class FStringType(
//    val length: Int
//) : AtomicType<String> {
//    override val typeName = "FString-" + length
//    override val tType = String::class
//    override fun asString(item: String): String = item
//    override fun fromString(s: String) = if (s.length <= this.length) s else throw Exception("Too Long")
//    override val jsonType: AtomicJsonType = AtomicJsonType.STRING
//}


val StringType = AtomicType(
    typeName = "String",
    tType = String::class,
    asString = {it},
    fromString = {it},
    jsonType =  AtomicJsonType.STRING
)


val IntType = AtomicType(
    typeName = "Int",
    tType = Int::class,
    asString = {it.toString()},
    fromString = {it.toInt()
    },
    jsonType =  AtomicJsonType.STRING
)

val EmailType = AtomicType(
    typeName = "Email",
    tType = Email::class,
    asString = {it.toString()},
    fromString = {
        val ss = it.split(Regex("@"), 2)
        Email(ss[0], ss[1])
    },
    jsonType =  AtomicJsonType.STRING
)

val JSONType = AtomicType(
    typeName = "JSON",
    tType = JSON::class,
    asString = {it.toString()},
    fromString = {JSON(it)},
    jsonType =  AtomicJsonType.STRING
)

val UUIDType = AtomicType<UUID>(
    typeName = "UUID",
    tType = UUID::class,
    asString = {it.toString()},
    fromString = {UUID.fromString(it)},
    jsonType =  AtomicJsonType.STRING
)

class Field<X : Any, T : Type<X>>(
    val fieldName: String,
    val type: T
)

open class EntityField<E : Any, E_ : Any, X : Any, T : Type<X>>(
    val field: Field<X, T>,
    val getter: (E) -> X,
    val getter_: (E_) -> X,
    val setter_: (E_, X) -> Unit
)

class EntityAtomicField<E : Any, E_ : Any, X : Any>(
    field: Field<X, AtomicType<X>>,
    getter: (E) -> X,
    getter_: (E_) -> X,
    setter_: (E_, X) -> Unit,
    val isSearchable: Boolean = true,
    val isKey: Boolean = false
) : EntityField<E, E_, X, AtomicType<X>>(field, getter, getter_, setter_)

class EntityComplexField<E : Any, E_ : Any, X : Any, X_:Any> (
    field: Field<X, ComplexType<X, X_>>,
    getter: (E) -> X,
    getter_: (E_) -> X,
     setter_: (E_, X) -> Unit
): EntityField<E, E_, X, ComplexType<X, X_>>(field, getter, getter_, setter_)

open class ComplexType<E : Any, E_ : Any>(
    override val typeName: String,
    val fields: List<EntityField<E, E_, *, *>>,
    val buildNew: () -> E_,
    val buildCopy: (E) -> E_,
    val build: (E_) -> E
) : Type<E>

class EntityType<E : Any, E_ : Any, K : Any>(
    override val typeName: String,
    val identityField: EntityAtomicField<E, E_, K>,
    fields: List<EntityField<E, E_, *, *>>,
    buildNew: () -> E_,
    buildCopy: (E) -> E_,
    build: (E_) -> E
) : ComplexType<E, E_>(typeName, fields, buildNew, buildCopy, build)




