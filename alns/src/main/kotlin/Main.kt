package com.ft.aio.template.adapter.output.web.scrippt

import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.script.engine.Alns.Alns
import java.io.File

import kotlinx.serialization.*
import kotlinx.serialization.json.Json


fun readJsonFromFile(filePath: String): InputData {
    val jsonContent = File(filePath).readText()
    return Json.decodeFromString(jsonContent)
}

fun getInput(): InputData{
    return readJsonFromFile("data_dummy.json")
}
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    var data = getInput()
    var optimizer = Alns(data)
    println(optimizer.runAlns())
}