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
import sql.int
import sql.varchar
import java.nio.charset.CharsetEncoder
import java.util.*


class SqlFieldMeta<X : Any, T : Type<X>>(
    val toDbMap: (X) -> DbMap,
    val fromDbMap: (DbMap) -> X,
    val columnDefs: List<ColumnDef<*, *>>
)

fun columnDefsFromSqlFieldMetas(sqlFieldMetas: List<SqlFieldMeta<*, *>>) = sqlFieldMetas.flatMap { it.columnDefs }



fun  <E:Any, E_:Any> sqlFieldMeta(entityField: EntityField<E, E_, *,*>): SqlFieldMeta<*, *> =
    if (entityField is EntityAtomicField) {
        val field = entityField.field
        when (entityField.field.type) {
            StringType -> {
                val columnDef = varchar(field.fieldName, 255)
                SqlFieldMeta(
                    toDbMap = { mapOf(columnDef.name to DbValue(it)) },
                    fromDbMap = { m -> m.get(columnDef.name)?.asString() ?: throw Exception() },
                    columnDefs = listOf(
                        varchar(
                            name = field.fieldName,
                            length = 255,
                            isPrimary = entityField.isKey
                        )
                    )
                )
            }
            IntType -> {
                val columnDef = varchar(field.fieldName, 255)
                SqlFieldMeta(
                    toDbMap = { mapOf(columnDef.name to DbValue(it)) },
                    fromDbMap = { m -> m.get(columnDef.name)?.asInt() ?: throw Exception() },
                    columnDefs = listOf(int(field.fieldName))
                )
            }
            UUIDType -> {
                val columnDef = varchar(field.fieldName, 255)
                SqlFieldMeta(
                    toDbMap = { mapOf(columnDef.name to DbValue(it.toString())) },
                    fromDbMap = { m -> UUID.fromString(m.get(columnDef.name)?.asString() ?: throw Exception()) },
                    columnDefs = listOf(varchar(field.fieldName, 255))
                )
            }

            EmailType -> {
                val columnDef1 = varchar(field.fieldName + "_NAME", 255)
                val columnDef2 = varchar(field.fieldName + "_DOMAIN", 255)

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


fun toJSon(it: Any, type: ComplexType<*, *>): String {
    return "x=y"
}


fun fromJSon(json: CharSequence, type: ComplexType<*, *>): String {
    throw Exception()
}











