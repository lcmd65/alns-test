package com.ft.aio.template.adapter.output.web.scrippt


import com.ft.aio.template.adapter.output.web.scrippt.engine.Alns
import com.ft.aio.template.adapter.output.web.scrippt.engine.PreProcess
import com.ft.aio.template.adapter.output.web.scrippt.utils.DumpJson
import com.ft.aio.template.adapter.output.web.scrippt.utils.ToExcel

//import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.boot.runApplication

//@SpringBootApplication
//class SpringBootTemplateApplication

//fun main(args: Array<String>) {
    //runApplication<SpringBootTemplateApplication>(*args)
//}
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    var data = PreProcess().dataPreprocessing()
    data.validateInputData()

    var optimizer = Alns(data)
    optimizer.runIteration()

    println("Optimize score " + optimizer.score)
    println("Penalty: " + optimizer.penalty)
    println("Best Solution: " + optimizer.bestSolution)

    var dump: MutableMap<String, Any> = mutableMapOf()
    dump.set("score", optimizer.score)
    dump.set("penalty", optimizer.penalty)
    dump.set("solution", optimizer.bestSolution)
    dump.set("horizontalCoverage", optimizer.horizontalCoverageFullFill)

    DumpJson().dumpToJsonFile(dump,"alns/src/main/kotlin/output/output.json")
    ToExcel().exportToExcel(optimizer.bestSolution, "alns/src/main/kotlin/output/output.xlsx")
}