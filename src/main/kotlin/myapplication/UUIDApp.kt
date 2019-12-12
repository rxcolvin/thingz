package myapplication

import java.io.Console
import java.util.*

fun main(args: Array<String>) {

    com.ibm.icu.util.TimeZone.getAvailableIDs(com.ibm.icu.util.TimeZone.SystemTimeZoneType.CANONICAL, "FR", null).forEach {
        println(it)
    }

    val count = if (args.size > 1) args[0].toInt() else 10

    for (i in 1..10) {
        println(UUID.randomUUID())
    }

}

