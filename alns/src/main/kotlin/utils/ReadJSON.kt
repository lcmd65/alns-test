package com.ft.aio.template.adapter.output.web.scrippt.utils

import java.io.File

class ReadJSON{

    fun readJson(path: String): String {
        val identifier = "[ReadJSON]"
        try {
            val file = File(path)
            if (!file.exists()) {
                println("$identifier File not found: $path")
                return ""
            }

            val jsonString = file.readText()
            return jsonString
        } catch (e: Exception) {
            println("$identifier Error reading JSON: $e")
            e.printStackTrace()
            return ""
        }
    }
}
