package meta

import jdbcstorage.JdbcStorageManager
import lang.Email
import myapplication.Person
import myapplication.ageField
import myapplication.emailField
import myapplication.nameField


fun main(args: Array<String>) {
//    val dbMapper = JdbcStorageManager<Person, String>(
//        entityTypeName = "Person",
//        fieldMapperPairs = listOf(
//            DbEntityFieldMapper(
//                EntityFieldMapper({ it.name }),
//                AtomicTypeDbMapper(nameField)
//            ),
//            DbEntityFieldMapper(
//                EntityFieldMapper({ it.age }),
//                AtomicTypeDbMapper(ageField)
//            ),
//            DbEntityFieldMapper(
//                EntityFieldMapper({ it.email }),
//                EmailTypeDbMapper(emailField)
//            )
//        )
//    )
//
//    dbMapper.createSchema()
//
//    val person = Person(name = "Fred", age = 10, email = Email("fred", domain = "blogs.com"))
//
//    dbMapper.put(person)
}

