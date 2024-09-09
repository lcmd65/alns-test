package com.ft.aio.template.adapter.output.web.scrippt.utils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class DumpJson {

    fun dumpToJsonFile(schedules: MutableMap<String, MutableMap<Int, String>>, filePath: String) {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(schedules)
        File(filePath).writeText(jsonString)
        println("Data has been written to $filePath")
    }
}