package meta

import jdbc.DbMap
import jdbc.DbMapBuilder
import jdbc.DbValue
import lang.Email
import lang.JSON
import myapplication.Address
import myapplication.AddressType
import myapplication.Address_
import sql.ColumnDef
import sql.Primary
import sql.int
import sql.varchar
import java.nio.charset.CharsetEncoder
import java.util.*


class SqlFieldMeta<X : Any, T : Type<X>>(
    val toDbMap: (X) -> DbMap,
    val fromDbMap: (DbMap) -> X,
    val columnDefs: List<ColumnDef>
)

fun columnDefsFromSqlFieldMetas(sqlFieldMetas: List<SqlFieldMeta<*, *>>) = sqlFieldMetas.flatMap { it.columnDefs }


fun <E : Any, E_ : Any> sqlFieldMeta(entityField: EntityField<E, E_, *, *>): SqlFieldMeta<*, *> =
    if (entityField is EntityAtomicField<*, *, *, *>) {
        val field = entityField.field
        when (entityField.field.type) {
            StringType -> {
                val columnDef = varchar(
                    name = field.fieldName,
                    length = 255,
                    isPrimary = if (entityField.isKey) Primary.FIRST else Primary.NONE
                )
                SqlFieldMeta(
                    toDbMap = { mapOf(columnDef.name to DbValue(it)) },
                    fromDbMap = { m -> m.get(columnDef.name)?.asString() ?: throw Exception() },
                    columnDefs = listOf(
                        columnDef
                    )
                )
            }
            IntType -> {
                val columnDef = int(
                    name = field.fieldName
                )
                SqlFieldMeta(
                    toDbMap = { mapOf(columnDef.name to DbValue(it)) },
                    fromDbMap = { m -> m.get(columnDef.name)?.asInt() ?: throw Exception() },
                    columnDefs = listOf(columnDef)
                )
            }
            UUIDType -> {
                val columnDef = varchar(
                    name = field.fieldName,
                    length = 36,
                    isPrimary = if (entityField.isKey) Primary.FIRST else Primary.NONE
                )
                SqlFieldMeta(
                    toDbMap = { mapOf(columnDef.name to DbValue(it.toString())) },
                    fromDbMap = { m -> UUID.fromString(m.get(columnDef.name)?.asString() ?: throw Exception()) },
                    columnDefs = listOf(columnDef)
                )
            }

            EmailType -> {
                val columnDef1 = varchar(field.fieldName + "_NAME", 50)
                val columnDef2 = varchar(field.fieldName + "_DOMAIN", 253)

                SqlFieldMeta(
                    toDbMap = {
                        mapOf(
                            columnDef1.name to DbValue(it.name),
                            columnDef2.name to DbValue(it.domain)
                        )
                    },
                    fromDbMap = { m ->
                        Email(
                            m.get(columnDef1.name)?.asString() ?: throw Exception(),
                            m.get(columnDef2.name)?.asString() ?: throw Exception()
                        )
                    },
                    columnDefs = listOf(
                        columnDef1, columnDef2
                    )
                )

            }
            else -> throw Exception()

        }
    } else if (entityField is EntityComplexField<*, *, *, *>) {
        val field = entityField.field

        val columnDef = varchar(field.fieldName, 255)

        SqlFieldMeta(
            toDbMap = {
                mapOf(
                    columnDef.name to DbValue(
                        toJSon(it, field.type as ComplexType<*, *>)
                    )
                )
            },
            fromDbMap = {
                fromJSon(
                    it.get(columnDef.name)?.asString() ?: throw Exception(),
                    field.type as ComplexType<*, *>
                )
            },
            columnDefs = listOf(varchar(field.fieldName, 255))
        )
    } else {
        throw java.lang.Exception()
    }


fun toJSon(item: Any, type: ComplexType<*, *>): String {
    val t = json.JsonTokenSerializer()
    t.openMap()
    type.fields.forEach {
        val value = (it.getter as (Any) -> Any)(item)
        when (it.field.type) {
            is AtomicType<*, *> -> {
                t.atomicValue( (it.field.type.toJson as (Any) -> Any)(value))
            }
        }
    }
    t.closeMap()
    return t.asString()
}


fun fromJSon(json: CharSequence, type: ComplexType<*, *>): Any {
    throw Exception()
}











