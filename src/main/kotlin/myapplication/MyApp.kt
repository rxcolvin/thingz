package myapplication

import api.APIHttp4KEngine
import api.AssetAPI
import common.entities.Address
import common.entities.Person
import common.entities.PersonType
import jdbc.DriverManager
import jdbcstorage.SimpleJdbcStorageManager
import common.lang.Email
import common.lang.Uuid
import org.http4k.core.Method
import org.http4k.core.Request
import sql.DerbySqlHelper
import java.util.*
import kotlin.concurrent.thread



fun main() {

     System.getProperty("java.class.path").split(";").forEach {
        println(it)
    }

    try {
       Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
        val cxn = DriverManager.getConnection("jdbc:derby:/derbydatabases/testone;create=true")

        val storageManager = SimpleJdbcStorageManager(
            connection = cxn,
            entityType = PersonType,
            sqlHelper = DerbySqlHelper(),
            schemaName = ""
        )

        println(storageManager.createStatement.sqlStatement.sql)
        println(storageManager.getByIdStatement.sqlStatement.sql)

        try {
            storageManager.dropSchema(false)
        } catch (ex: Exception) {
            println(ex)
        }
        storageManager.createSchema()
        val person = Person(
            uuid = Uuid(UUID.randomUUID().toString()),
            name = "Fred",
            age = 24,
            email = Email("Fred", "Bloggs.com"),
            address = Address("Chez Moi")
        )
        storageManager.create(person)
        println(storageManager.getById(person.uuid))

        val assetAPI = AssetAPI(
            entityType = PersonType,
            storageManager = storageManager
        )

        val response = assetAPI.getById(person.uuid.toString())
        println(response.body)

        val engine = APIHttp4KEngine(
            assetAPIs = listOf(assetAPI),
            exiting = {
                thread(start = true) {
                    Thread.sleep(500)
                    System.exit(0)}
            }
        )

        println(engine.app(Request(Method.GET, "/person/${person.uuid}")))

        engine.start()


    } catch (e: Exception) {
        e.printStackTrace()
    }
}

