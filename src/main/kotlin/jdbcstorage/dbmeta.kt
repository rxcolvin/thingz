package jdbcstorage

import jdbc.DbMap
import jdbc.DbValue
import common.lang.Email
import common.meta.*
import sql.ColumnDef
import sql.Primary
import sql.int
import sql.varchar
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
                        complexTypeToJson(it as Any, field.type as ComplexType<Any, Any>)
                    )
                )
            },
            fromDbMap = {
                complexTypeFromJSon(
                    it.get(columnDef.name)?.asString() ?: throw Exception(),
                    field.type as ComplexType<*, *>
                )
            },
            columnDefs = listOf(varchar(field.fieldName, 255))
        )
    } else {
        throw java.lang.Exception()
    }












