package common.meta

import common.json.JsonMap
import common.json.jsonListUnparser
import common.json.jsonMapParser
import common.json.jsonMapUnparser
import common.lang.Email
import common.lang.Uuid
import kotlin.reflect.KClass

interface Type<T : Any> {
    val typeName: String
}


class AtomicType<T : Any, J>(
    override val typeName: String,
    val tType: KClass<T>,
    val asString: (T) -> String,
    val fromString: (String) -> T,
    val fromJson: (jsonValue: J) -> T,
    val toJson: (value: T) -> J
) : Type<T> {

}

//interface ComplexType : Type {
//}

interface AtomicListType<T> : Type<List<T>> {
    val minSize: Int
    val maxSize: Int
}


interface AtomicMapType<K : Any, E : Any> : Type<Map<K, E>> {
    val keyType: AtomicType<K, *>
    val valueType: AtomicType<E, *>
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


val StringType = AtomicType<String, String>(
    typeName = "String",
    tType = String::class,
    asString = { it },
    fromString = { it },
    fromJson = { it },
    toJson = { it }
)


val IntType = AtomicType<Int, Number>(
    typeName = "Int",
    tType = Int::class,
    asString = { it.toString() },
    fromString = { it.toInt() },
    fromJson = { it.toInt() },
    toJson = { it }
)


val EmailType = AtomicType<Email, String>(
    typeName = "Email",
    tType = Email::class,
    asString = { it.name+"@"+it.domain },
    fromString = {
        val ss = it.split(Regex("@"), 2)
        Email(ss[0], ss[1])
    },
    fromJson = {
        val ss = it.split(Regex("@"), 2)
        Email(ss[0], ss[1])
    },
    toJson = { it.name+"@"+it.domain }
)

//val JSONType = AtomicType(
//    typeName = "JSON",
//    tType = JSON::class,
//    asString = { it.toString() },
//    fromString = { JSON(it) },
//    jsonType = AtomicJsonType.STRING
//)

val UUIDType = AtomicType<Uuid, String>(
    typeName = "UUID",
    tType = Uuid::class,
    asString = { it.uuid },
    fromString = { Uuid(it) },
    fromJson = {
        Uuid(it)
    },
    toJson = { it.uuid }
)


class Field<X : Any, T : Type<X>>(
    val fieldName: String,
    val type: T
)

open class EntityField<E : Any, E_ : Any, X : Any, T : Type<X>>(
    val field: Field<X, T>,
    val getter: (E) -> X,
    val getter_: (E_) -> X,
    val setter_: (E_, X) -> Unit,
    val nullable: Boolean = false
)

class EntityAtomicField<E : Any, E_ : Any, X : Any, J>(
    field: Field<X, AtomicType<X, J>>,
    getter: (E) -> X,
    getter_: (E_) -> X,
    setter_: (E_, X) -> Unit,
    val isSearchable: Boolean = true,
    val isKey: Boolean = false
) : EntityField<E, E_, X, AtomicType<X, J>>(field, getter, getter_, setter_)

class EntityComplexField<E : Any, E_ : Any, X : Any, X_ : Any>(
    field: Field<X, ComplexType<X, X_>>,
    getter: (E) -> X,
    getter_: (E_) -> X,
    setter_: (E_, X) -> Unit
) : EntityField<E, E_, X, ComplexType<X, X_>>(field, getter, getter_, setter_)

open class ComplexType<E : Any, E_ : Any>(
    override val typeName: String,
    val fields: List<EntityField<E, E_, *, *>>,
    val buildNew: () -> E_,
    val buildCopy: (E) -> E_,
    val build: (E_) -> E
) : Type<E>

class EntityType<E : Any, E_ : Any, K : Any>(
    override val typeName: String,
    val identityField: EntityAtomicField<E, E_, K, *>,
    fields: List<EntityField<E, E_, *, *>>,
    buildNew: () -> E_,
    buildCopy: (E) -> E_,
    build: (E_) -> E
) : ComplexType<E, E_>(typeName, fields, buildNew, buildCopy, build)


// to Json
fun <E:Any, E_:Any, K: Any> entityToJson(
    item: E,
    type: EntityType<E, E_, K>
) : String =
    jsonMapUnparser(entityToJSonMap(item, type))

fun <E:Any, E_:Any> complexTypeToJson(
    item: E,
    type: ComplexType<E, E_>
) : String =
    jsonMapUnparser(complexTypeToJsonMap(item, type))

fun <E:Any, E_:Any> complexListToJson(
    items: List<E>,
    type: ComplexType<E, E_>
) : String  =
    jsonListUnparser(items.map { complexTypeToJsonMap(it, type) })

fun <E:Any, E_:Any, K: Any> entityListToJson(
    items: List<E>,
    type: EntityType<E, E_, K>
) : String  =
    jsonListUnparser(items.map { entityToJSonMap(it, type) })


//from Json
fun <E:Any, E_:Any, K: Any> entityFromJSon(
    json: CharSequence,
    type: EntityType<E, E_, K>
): E =
    entityFromJsonMap(jsonMapParser(json.toString()), type)

fun <E:Any, E_:Any> complexTypeFromJSon(
    json: CharSequence,
    type: ComplexType<E, E_>
): E =
    complexTypeFromJsonMap(jsonMapParser(json.toString()), type)

// toJson Maps, List
fun <E:Any, E_:Any, K: Any> entityToJSonMap(
    item: E,
    type: EntityType<E, E_, K>
): JsonMap {
    val builder = LinkedHashMap<String, Any?>()

    with(type.identityField) {
        val value = (getter as (Any) -> Any)(item)
        builder.put(field.fieldName, (field.type.toJson as (Any) -> Any)(value))
    }
    type.fields.forEach {
        val value = (it.getter as (Any) -> Any)(item)
        when (it.field.type) {
            is AtomicType<*, *> -> {
                builder.put(it.field.fieldName, (it.field.type.toJson as (Any) -> Any)(value))
            }
            is ComplexType<*,*> -> {
                builder.put(it.field.fieldName, complexTypeToJsonMap(value, it.field.type as ComplexType<Any, Any>))
            }
        }
    }
    return builder.toMap()
}


fun <E:Any, E_:Any> complexTypeToJsonMap(
    item: E,
    type: ComplexType<E, E_>
): JsonMap {
    val builder = LinkedHashMap<String, Any?>()

    type.fields.forEach {
        val value = (it.getter as (Any) -> Any)(item)
        when (it.field.type) {
            is AtomicType<*, *> -> {
                builder.put(it.field.fieldName, (it.field.type.toJson as (Any) -> Any)(value))
            }
            is ComplexType<*,*> -> {
                builder.put(it.field.fieldName, complexTypeToJsonMap(value, it.field.type as ComplexType<Any, Any>))
            }
        }
    }
    return builder.toMap()
}


// Json Map etc -> Entity

fun <E:Any, E_:Any, K:Any> entityFromJsonMap(
    map: JsonMap,
    type: EntityType<E, E_, K>
) : E {
    val builder = type.buildNew()
    with (type.identityField) {
        val setter_ = setter_ as (Any, Any?) -> Unit
        val jsonValue = map.get(field.fieldName)
        if (jsonValue == null && !nullable) {
            throw Exception("Value for field ${field.fieldName} ")
        }
        if (jsonValue == null) {
            setter_(builder, null)
        } else {
            val value = (field.type.fromJson as (Any) -> Any)(jsonValue)
            setter_(builder, value)
        }
    }

    type.fields.forEach {
        val setter_ = it.setter_ as (Any, Any?) -> Unit
        val jsonValue  = map.get(it.field.fieldName)
        if (jsonValue == null && !it.nullable) {
            throw Exception("Value for field ${it.field.fieldName} ")
        }
        if (jsonValue == null) {
            setter_(builder, null)
        } else {
            val value =  when (it.field.type) {
                is AtomicType<*,*> -> (it.field.type.fromJson as (Any) -> Any)(jsonValue)
                is ComplexType<*,*> -> complexTypeFromJsonMap(jsonValue as JsonMap, it.field.type as ComplexType<Any, Any>)
                else -> TODO()
            }
            setter_(builder, value)
        }
    }
    return type.build(builder)
}

fun <E:Any, E_:Any> complexTypeFromJsonMap(
    map: JsonMap,
    type: ComplexType<E, E_>
) : E {
    val builder = type.buildNew()
    type.fields.forEach {
        val setter_ = it.setter_ as (Any, Any?) -> Unit
        val jsonValue  = map.get(it.field.fieldName)
        if (jsonValue == null && !it.nullable) {
            throw Exception("Value for field ${it.field.fieldName} Required")
        }
        if (jsonValue == null) {
            setter_(builder, null)
        } else {
            val value =  when (it.field.type) {
                is AtomicType<*,*> -> (it.field.type.fromJson as (Any) -> Any)(jsonValue)
                is ComplexType<*,*> -> complexTypeFromJsonMap(jsonValue as JsonMap, it.field.type)
                else -> TODO()
            }
            setter_(builder, value)
        }
    }
    return type.build(builder)
}
