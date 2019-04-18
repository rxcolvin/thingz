package myapplication

import expression.Const
import expression.Eq
import expression.Parameter
import jdbc.DriverManager
import jdbcstorage.JdbcStorageManager
import lang.Email
import meta.*
import sql.GenericSqlHelper
import java.util.*


data class Person(
    val uuid: UUID,
    val name: String,
    val age: Int,
    val email: Email
)

data class Person_(
    var uuid: UUID,
    var name: String,
    var age: Int,
    var email: Email
) : Builder<Person> {
    override fun create() = Person(
        uuid = uuid,
        name = this.name,
        age = this.age,
        email = this.email
    )

}

object DataDictionary {

}

val nameField = Field("name", StringType)
val ageField = Field("age", IntType)
val emailField = Field("email", EmailType)
val uuidField = Field("uuid", UUIDType)

val personType = EntityType<Person, UUID>(
    typeName = "Person",
    fields = listOf(
        nameField,
        ageField,
        emailField
    ),
    identityField = uuidField
)



fun main() {
    System.getProperty("java.class.path").split(";").forEach {
        println(it)
    }

    try {

        val personSqlEntityMeta = SqlEntityMeta<Person, UUID>(
            entityType = personType,
            fields = personType.fields.map { sqlFieldMeta(it) },
            identitySqlField = sqlFieldMeta(personType.identityField) as SqlFieldMeta<UUID, AtomicType<UUID>>
        )

        val personEntityMapper = EntityMapper<Person, Person_>(
            fieldMappers = listOf(
                FieldMapper(
                    field = uuidField,
                    getter = { it.uuid },
                    setter_ = { x, it -> x.uuid = it },
                    getter_ = { it.uuid }
                ),
                FieldMapper(
                    field = nameField,
                    getter = { it.name },
                    setter_ = { x, it -> x.name = it },
                    getter_ = { it.name }
                ),
                FieldMapper(
                    field = ageField,
                    getter = { it.age },
                    setter_ = { x, it -> x.age = it },
                    getter_ = { it.age }
                ),
                FieldMapper(
                    field = emailField,
                    getter = { it.email },
                    setter_ = { x, it -> x.email = it },
                    getter_ = { it.email }
                )

            ),
            builder = {
                Person_(
                    uuid = UUID.randomUUID(),
                    name = "",
                    age = 0,
                    email = Email("", "")
                )
            }
        )

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
        val cxn = DriverManager.getConnection("jdbc:derby:/derbydatabases/testone;create=true")

        val storageManager = JdbcStorageManager<UUID, Person, Person_>(
            sqlHelperFactory = GenericSqlHelper.Factory,
            sqlEntityMeta = personSqlEntityMeta,
            entityMapper = personEntityMapper,
            connection = cxn
        )

        storageManager.describeSchema()
        storageManager.sqlHelper.insertSql()

        storageManager.sqlHelper.selectSql(
            where = Eq(
                Parameter("name"), Const("Bob")
            )
        )

        try {
            storageManager.dropSchema()
        } catch (ex:Exception) {

        }
        storageManager.createSchema()
        val person = Person(UUID.randomUUID(), "Fred", 24, Email("Fred", "Bloggs.com"))
        storageManager.create(person)
        println(storageManager.getById(person.uuid))


    } catch (e: Exception) {
        e.printStackTrace()
    }
}

