package json

import org.junit.jupiter.api.Test

internal class JsonMapParserTest {


    @Test
    fun one() {
        val m =
            jsonMapParser(
                """{
            "foo": "bar",
            "n":  23,
            "n1" : -23,
            "n3" : +23,
            "d": 34343.5,
            "bool1" : true,
            "bool2" : false,
            "NULL" : null,
            "list": ["rwrwerew", 23],
            "map": {
               "s" : "weada",
               "empyt list" : []
               }
        }"""
            )

        println(m)

        val m2 = jsonMapUnparser(m)
        println(m2)
    }
}
