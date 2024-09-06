package com.ft.aio.template.adapter.output.web.script.engine.SimulateAnealing

import kotlin.math.exp
import kotlin.random.Random


class SimulatedAnealing {
    open fun init(ostFunction: (List<Int>) -> Int,
                           initialSolution: List<Int>,
                           initialTemperature: Double,
                           alpha: Double,
                           stoppingTemperature: Double,
                           maxIterations: Int): List< Int>{

        var currentSoluiton = initialSolution
        var bestSolution =  currentSoluiton
        var currentCost = null
        var bestCost = null


        return currentSoluiton
    }

    open fun GetNeighbor(): Int{
        return -1
    }

    fun costFunction(solution: List<Int>): Int {
        return solution.sumOf { it * it } // Sum of squares as the cost function
    }

}



