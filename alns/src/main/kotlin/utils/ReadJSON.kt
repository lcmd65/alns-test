package com.ft.aio.template.adapter.output.web.scrippt.utils

import java.io.File

open class ReadJSON{

    fun readJson(path: String): String {
        val identifier = "[ReadJSON]"
        try {
            val file = File(path)
            if (!file.exists()) {
                println("$identifier File not found: $path")
                return ""
            }

            println("$identifier Found File: $file")

            val jsonString = file.readText()  // Sử dụng readText để đọc toàn bộ nội dung file
            println("$identifier JSON as String: $jsonString")
            return jsonString
        } catch (e: Exception) {
            println("$identifier Error reading JSON: $e")
            e.printStackTrace()
            return ""
        }
    }
}
