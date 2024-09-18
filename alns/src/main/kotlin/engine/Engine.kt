package com.ft.aio.template.adapter.output.web.scrippt.engine
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.utils.DumpJson
import com.ft.aio.template.adapter.output.web.scrippt.utils.ToExcel

class Engine(var data: InputData) {

    var optimizer = Alns(data)

    fun init() {
        optimizer.runIteration()
        optimizer.bestSolution = optimizer.adjustScheduleToConstrain(optimizer.bestSolution)

        optimizer.penalty = optimizer.calculate.totalScore(optimizer.bestSolution)
        optimizer.score = Int.MAX_VALUE.toDouble() + optimizer.penalty
    }

    fun printSolution() {
        println("Optimize score " + optimizer.score)
        println("Penalty: " + optimizer.penalty)
        println("Best Solution: " + optimizer.bestSolution)
    }

    fun saveSolution(){
        var dump: MutableMap<String, Any> = mutableMapOf()
        dump.set("score", optimizer.score)
        dump.set("penalty", optimizer.penalty)
        dump.set("solution", optimizer.bestSolution)

        DumpJson().dumpToJsonFile(dump,"alns/src/main/kotlin/output/output.json")
        ToExcel().exportToExcel(optimizer.bestSolution, "alns/src/main/kotlin/output/output.xlsx")
    }
}