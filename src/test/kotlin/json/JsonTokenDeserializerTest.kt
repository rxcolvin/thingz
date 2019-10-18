package json

import org.junit.jupiter.api.Test

internal class JsonTokenDeserializerTest {

    val target = JsonTokenDeserializer(
        """{
            "foo": "bar"
        }""")

    @Test
    fun one() {
        target.openMap()
        println(target.key())
        target.mapPropertySeparator()
        println(target.atomicValue())
        target.closeMap()
    }
}

