package myapplication

import jdbc.DriverManager
import jdbcstorage.JdbcStorageManager
import lang.Email
import meta.*
import sql.GenericSqlHelper
import java.net.URLClassLoader




data class Person(
    val name: String,
    val age: Int,
    val email: Email
)

data class Person_ (
    var name: String,
    var age: Int,
    var email: Email
) : Builder<Person> {
    override fun create() = Person (
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

val personType = EntityType<Person>(
    typeName = "Person",
    fields = listOf(
        nameField,
        ageField,
        emailField
    )
)

val emailType =  EntityType<Person>(
    typeName = "Address",
    fields = listOf()
)

fun main() {
    System.getProperty("java.class.path").split(";").forEach {
        println(it)
    }

    try {

        val personSqlEntityMeta = SqlEntityMeta(
        entityType = personType,
        fields = personType.fields.map { sqlFieldMeta(it)},
        identityField = nameField
    )

    val personEntityMapper = EntityMapper<Person, Person_>(
        fieldMappers = listOf(
            FieldMapper(
                field = nameField,
                getter = {it.name},
                setter_ = {x, it -> x.name = it},
                getter_ = {it.name}
                ),
            FieldMapper(
                field = ageField,
                getter = {it.age},
                setter_ = {x, it -> x.age = it},
                getter_ = {it.age}
            ),
            FieldMapper(
                field = emailField,
                getter = {it.email},
                setter_ = {x, it -> x.email = it},
                getter_ = {it.email}
            )

        ),
        builder = {Person_(
            name = "",
            age = 0,
            email = Email("","")
        )}
    )

    Class.forName("org.apache.derby.jdbc.EmbeddedDriver")
    val cxn = DriverManager.getConnection("jdbc:derby:/derbydatabases/testone;create=true")

    val storageManager = JdbcStorageManager<String, Person, Person_>(
       sqlHelperFactory = GenericSqlHelper.Factory,
        sqlEntityMeta = personSqlEntityMeta,
        entityMapper = personEntityMapper,
        connection = cxn
    )
         storageManager.createSchema()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

//C:\jdk\zulu10.1+11-jdk10-win_x64\bin\java.exe -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:63298,suspend=y,server=n -javaagent:C:\Users\richard.colvin\.IdeaIC2018.3\system\groovyHotSwap\gragent.jar -javaagent:C:\Users\richard.colvin\.IdeaIC2018.3\system\captureAgent\debugger-agent.jar -Dfile.encoding=UTF-8 -classpath "C:\github\thingz\out\production\classes;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk8\1.3.11\dd9bff00d6cfca58b6c1fe89be8e0678e35cf35f\kotlin-stdlib-jdk8-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk7\1.3.11\4839661cf6ce3c14b65ed7dcf5b9249b44ecca16\kotlin-stdlib-jdk7-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib\1.3.11\4cbc5922a54376018307a731162ccaf3ef851a39\kotlin-stdlib-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-common\1.3.11\d8b8e746e279f1c4f5e08bc14a96b82e6bb1de02\kotlin-stdlib-common-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains\annotations\13.0\919f0dfe192fb4e063e7dacadee7f8bb9a2672a9\annotations-13.0.jar;C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2018.2.5\lib\idea_rt.jar" myapplication.MyAppKt
//C:\jdk\jdk1.8.0_131-32\bin\java.exe -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:63130,suspend=y,server=n -javaagent:C:\Users\richard.colvin\.IdeaIC2018.3\system\groovyHotSwap\gragent.jar -javaagent:C:\Users\richard.colvin\.IdeaIC2018.3\system\captureAgent\debugger-agent.jar -Dfile.encoding=UTF-8 -classpath "C:\jdk\jdk1.8.0_131-32\jre\lib\charsets.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\deploy.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\access-bridge-32.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\cldrdata.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\dnsns.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\jaccess.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\jfxrt.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\localedata.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\nashorn.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\sunec.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\sunjce_provider.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\sunmscapi.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\sunpkcs11.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\ext\zipfs.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\javaws.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\jce.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\jfr.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\jfxswt.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\jsse.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\management-agent.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\plugin.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\resources.jar;C:\jdk\jdk1.8.0_131-32\jre\lib\rt.jar;C:\github\thingz\out\production\classes;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk8\1.3.11\dd9bff00d6cfca58b6c1fe89be8e0678e35cf35f\kotlin-stdlib-jdk8-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-jdk7\1.3.11\4839661cf6ce3c14b65ed7dcf5b9249b44ecca16\kotlin-stdlib-jdk7-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib\1.3.11\4cbc5922a54376018307a731162ccaf3ef851a39\kotlin-stdlib-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin\kotlin-stdlib-common\1.3.11\d8b8e746e279f1c4f5e08bc14a96b82e6bb1de02\kotlin-stdlib-common-1.3.11.jar;C:\Users\richard.colvin\.gradle\caches\modules-2\files-2.1\org.jetbrains\annotations\13.0\919f0dfe192fb4e063e7dacadee7f8bb9a2672a9\annotations-13.0.jar;C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2018.2.5\lib\idea_rt.jar" myapplication.MyAppKt