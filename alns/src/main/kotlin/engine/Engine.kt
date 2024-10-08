package com.ft.aio.template.adapter.output.web.scrippt.engine
import com.ft.aio.template.adapter.output.web.scrippt.constrain.Constraint
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.utils.DumpJson
import com.ft.aio.template.adapter.output.web.scrippt.utils.ToExcel

class Engine(var data: InputData) {

    var optimizer = Alns(data)

    fun init() {
        optimizer.runIteration()
        optimizer.penalty = optimizer.calculate.totalScore(optimizer.bestSolution)
        optimizer.score = Int.MAX_VALUE.toDouble() + optimizer.penalty
    }

    fun printSolution() {
        println("Optimize score " + optimizer.score)
        println("Penalty: " + optimizer.penalty)
        println("Constraint score: " + optimizer.constrainScore)
        println("H Coverage score: " + optimizer.horizontalCoverageScore)
        println("Coverage score: " + optimizer.coverageScore)
        println("Pattern Constraint Score: " + optimizer.patternConstrainScore)
        println("Best Solution: " + optimizer.bestSolution)
    }

    fun saveSolution(){
        var dump: MutableMap<String, Any> = mutableMapOf()
        dump.set("score", optimizer.score)
        dump.set("penalty", optimizer.penalty)
        dump.set("solution", optimizer.bestSolution)
        dump.set("horizontal coverage", optimizer.horizontalCoverageScore)
        dump.set("coverage", optimizer.coverageScore)
        var conScore : MutableMap<String, Constraint> = mutableMapOf()
        CommonCaculate(data).createConstrainScore(optimizer.bestSolution)
        for (con in data.constrains){
            conScore.set(con.id, con)
        }
        dump.set("constraints", conScore.toMutableMap())
        DumpJson().dumpToJsonFile(dump,"alns/src/main/kotlin/output/output.json")
        ToExcel().exportToExcel(optimizer.bestSolution, "alns/src/main/kotlin/output/output.xlsx")
    }
}