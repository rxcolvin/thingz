package meta

import lang.Email
import sql.ColumnDef
import sql.int
import sql.varchar
import java.util.*

//import lang.Email
//import lang.T2
//import kotlin.reflect.KClass
//
//
//
//interface DbField<T : Type> {
//  val name: String
//}
//
//
//
//interface AtomicDbField<T: Any> : DbField<AtomicType<T>> {
//  val type: T
//  val queryable: Boolean
//
//}
//
//interface ComplexDbField : DbField<ComplexType> {
//
//}
//
//
//
//interface DbEntity {
//  val name: String
//  val keyFields: List<DbField>
//  val otherFields: List<DbField>
//}
//
//interface EntityMetaHolder<E : Any> {
//  val kclass: KClass<E>
//}
//
//
//interface DbEntityMetaHolder<E : Any> : EntityMetaHolder<E> {
//  fun get(index: Int): DbFieldMeta
//}
//
//data class DbColumn<X>(
//    val colName: String
//)
//
//class EntityFieldMapper<T, E>(
//    val getter: (E) -> T
//)
//
//interface DbFieldMapper<T>
//
//interface DbFieldMapper1<T, C1> : DbFieldMapper<T> {
//  val dbColumn1: DbColumn<C1>
//  fun toDB(value: T): C1
//  fun fromDB(value: C1): T
//}
//
//interface DbFieldMapper2<T, C1, C2> : DbFieldMapper<T> {
//  val dbColumn1: DbColumn<C1>
//  val dbColumn2: DbColumn<C2>
//  fun toDB(value: T): T2<C1, C2>
//  fun fromDB(values: T2<C1, C2>): T
//}
//
//
//class DbEntityFieldMapper<T, E>(
//    val entityFieldMapper: EntityFieldMapper<T, E>,
//    val dbFieldMapper: DbFieldMapper<T>
//) {
//  fun set(entity: E, f: (String, Any?) -> Unit) {
//    val value = entityFieldMapper.getter(entity)
//    when (dbFieldMapper) {
//      is DbFieldMapper1<*, *> -> {
//        val m = dbFieldMapper as DbFieldMapper1<T, *>
//        f(m.dbColumn1.colName, m.toDB(value))
//      }
//      is DbFieldMapper2<*, *, *> -> {
//        val m = dbFieldMapper as DbFieldMapper2<T, *, *>
//        val t2 = m.toDB(value)
//        f(m.dbColumn1.colName, t2.v1)
//        f(m.dbColumn2.colName, t2.v2)
//      }
//    }
//  }
//}
//
//class AtomicTypeDbMapper<T : Any>(
//    atomicField: AtomicField<T>
//) : DbFieldMapper1<T, T> {
//
//  override val dbColumn1 = DbColumn<T>(atomicField.fieldName)
//
//  override fun toDB(value: T): T = value
//
//  override fun fromDB(value: T): T = value
//}
//
//
//class EmailTypeDbMapper(
//    atomicField: AtomicField<Email>
//) : DbFieldMapper2<Email, String, String> {
//
//  override val dbColumn1 = DbColumn<String>(atomicField.fieldName + "_name")
//  override val dbColumn2 = DbColumn<String>(atomicField.fieldName + "_domain")
//
//  override fun toDB(value: Email): T2<String, String> = T2(value.name, value.domain)
//
//  override fun fromDB(values: T2<String, String>): Email = Email(values.v1, values.v2)
//}
//
//

class SqlEntityMeta<T: Any, K: Any>(
    val entityType: EntityType<T, K>,
    val fields: List<SqlFieldMeta<*, *>>,
    val identitySqlField: SqlFieldMeta<K, AtomicType<K>>
) {
    val allFields = listOf(identitySqlField) + fields
}

class SqlFieldMeta<X:Any, T : Type<X>>(
    val field: Field<X, T>,
    val toDbMap: (X) -> List<Pair<String, Any>>,
    val fromDbMap: (Map<String, Any>) -> X,
    val columnDefs: List<ColumnDef<*, *>>
)




fun sqlFieldMeta(field: Field<*, *>): SqlFieldMeta<*, *> {
    return when (field.type) {
        is StringType ->
            SqlFieldMeta(
                field = field as Field<String, StringType>,
                toDbMap = { listOf(Pair(field.fieldName, it)) },
                fromDbMap = { m -> m.get(field.fieldName.toUpperCase()) as String },
                columnDefs = listOf(varchar(field.fieldName, 255))
            )
        is IntType ->
            SqlFieldMeta(
                field = field as Field<Int, IntType>,
                toDbMap = { listOf(Pair(field.fieldName, it)) },
                fromDbMap = { m -> m.get(field.fieldName.toUpperCase()) as Int },
                columnDefs = listOf(int(field.fieldName))
            )
        is UUIDType ->
            SqlFieldMeta(
                field = field as Field<UUID, UUIDType>,
                toDbMap = { listOf(Pair(field.fieldName, it.toString())) },
                fromDbMap = { m -> UUID.fromString(m.get(field.fieldName.toUpperCase()) as String) },
                columnDefs = listOf(varchar(field.fieldName, length = 50))
            )
        is EmailType -> {
            val fieldName1 = field.fieldName + "_NAME"
            val fieldName2 = field.fieldName + "_domain"
            SqlFieldMeta<Email, EmailType>(
                field = field as Field<Email, EmailType>,
                toDbMap = {
                    listOf(
                        Pair(fieldName1, it.name),
                        Pair(fieldName2, it.domain)
                    )
                },
                fromDbMap = { m ->
                    Email(
                        m.get(fieldName1.toUpperCase()) as String,
                        m.get(fieldName2.toUpperCase()) as String
                    )
                },
                columnDefs = listOf(
                    varchar(fieldName1, 255),
                    varchar(fieldName2, 255)                    )
            )
        }

        else -> throw Exception()
    }
}





