package com.ft.aio.template.adapter.output.web.scrippt

import com.ft.aio.template.adapter.output.web.script.engine.Alns.Alns
import com.ft.aio.template.adapter.output.web.scrippt.engine.PreProcess
import com.ft.aio.template.adapter.output.web.scrippt.utils.DumpJson
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    var data = PreProcess().dataPreprocessing()

    data.validateInputData()
    var optimizer = Alns(data)
    optimizer.runAlns()
    println("Optimize score " + optimizer.score)
    println("Penalty " + optimizer.penalty)
    println(optimizer.solution)
    DumpJson().dumpToJsonFile(optimizer.solution, "alns/src/main/kotlin/output/output.json")
}