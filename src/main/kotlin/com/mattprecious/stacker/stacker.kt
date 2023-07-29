package com.mattprecious.stacker

import com.github.ajalt.clikt.core.CliktCommand

class Stacker : CliktCommand(
    name = "st",
) {
    override fun run() {
        echo("Hello!")
    }
}

fun main(args: Array<String>) = Stacker().main(args)
