package myapplication

import java.util.*

fun main(args: Array<String>) {

    val count = if (args.size > 1) args[0].toInt() else 10

    for (i in 1..10) {
        println(UUID.randomUUID())
    }
}

