package json

import common.json.JsonTokenSerializer
import org.junit.jupiter.api.Test

internal class JsonTokenSerializerTest {

    val target = JsonTokenSerializer()

    @Test
    fun one() {
        target.openMap()
        target.key("foo")
        target.mapPropertySeparator()
        target.atomicValue("Bar")

        target.closeMap()
        println(target.asString())
    }
}

