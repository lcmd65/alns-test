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
    println("Penalty: " + optimizer.penalty)
    println("Best Solution: " + optimizer.bestSolution)

    var dump: MutableMap<String, Any> = mutableMapOf()
    dump.set("score", optimizer.score)
    dump.set("penalty", optimizer.penalty)
    dump.set("solution", optimizer.bestSolution)
    dump.set("horizontalCoverage", optimizer.horizontalCoverageFullFill)

    DumpJson().dumpToJsonFile(dump,"alns/src/main/kotlin/output/output.json")
}