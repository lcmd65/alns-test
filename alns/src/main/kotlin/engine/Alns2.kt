package com.ft.aio.template.adapter.output.web.scrippt.engine

import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.rule.RuleViolation
import kotlin.math.exp
import kotlin.random.Random

class Alns2(var data: InputData, var schedulesIter1: MutableMap<String, MutableMap<Int, String>>) {
    private var numberIterations: Int = 1000
    private var temperature: Double = 1000.0
    private var alpha: Double = 0.9
    private var limit: Double = 1e-3
    private var deltaE: Double = 0.0
    var score: Double = 0.0
    var penalty: Double = Int.MAX_VALUE.toDouble()
    var probabilitiesOfOperator: MutableMap<Int, Double> = mutableMapOf()
    var operatorScore: MutableMap<Int, Double> = mutableMapOf()
    var operatorWeight: MutableMap<Int, Double> = mutableMapOf()
    var operatorTimes: MutableMap<Int, Double> = mutableMapOf()
    var bestSolution: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()
    var constrainScore: Double = 0.0
    var coverageScore: Double = 0.0
    var horizontalCoverageScore: Double = 0.0
    var patternConstrainScore: Double = 0.0
    var calculate = CommonCaculate(data)
    var rule = RuleViolation(data)

    private fun deepCopySolution(solution: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val newSolution = mutableMapOf<String, MutableMap<Int, String>>()
        for ((key, innerMap) in solution) {
            val copiedInnerMap = innerMap.toMutableMap()
            newSolution[key] = copiedInnerMap
        }
        return newSolution
    }

    private fun createWeightOperators() {
        for (index in 0..2) {
            if (this.operatorWeight.get(index) != null){
                this.operatorWeight.set(
                    index,
                    this.operatorWeight.get(index)!! * 0.2 + 0.8 * this.operatorScore?.get(index)!! / this.operatorTimes?.get(index)!!)
            }
            else{
                this.operatorWeight.set(
                    index,
                    0.2 + 0.8 * this.operatorScore?.get(index)!! / this.operatorTimes?.get(index)!!)
            }
        }
    }

    private fun resetWeightOperators(){
        for(index in 0..2) {
            this.operatorWeight.set(
                index,
                0.4 + 0.6 * this.operatorScore?.get(index)!! / this.operatorTimes?.get(index)!!
            )
        }
    }

    private fun createScoreOperator() {
        this.operatorScore[0] = 0.2
        this.operatorScore[1] = 0.2
        this.operatorScore[2] = 0.2
    }

    private fun createOperatorTimes() {
        for (index in 0..2) {
            this.operatorTimes.set(index, 1.0)
        }
    }

    private fun calculateSimulatedAnealing(
        currentScheduled: MutableMap<String, MutableMap<Int, String>>,
        nextScheduled:MutableMap<String, MutableMap<Int, String>>
    ): MutableMap<String, MutableMap<Int, String>> {
        deltaE = calculate.totalScore(currentScheduled) - calculate.totalScore(nextScheduled)
        if (deltaE < 0){
            return nextScheduled
        }
        else {
            if (temperature < limit) {
                return currentScheduled
            }
            val probability = exp(deltaE / temperature)
            val acceptanceVariable = Random.nextDouble(0.0, 1.0)

            temperature *= alpha
            if (probability < acceptanceVariable) {
                return nextScheduled
            }
        }
        return currentScheduled
    }

    private fun scoring(){
        this.score = calculate.totalScore(this.bestSolution)
        this.constrainScore = calculate.constrainScore(this.bestSolution)
        this.horizontalCoverageScore = calculate.horizontalCoverageScore(this.bestSolution)
        this.coverageScore = calculate.coverageScore(this.bestSolution)
        this.patternConstrainScore = calculate.patternConstrainScore(this.bestSolution)
    }

    private fun randomDestroySolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val mutableSchedule = deepCopySolution(schedules)
        if (mutableSchedule.isNotEmpty()) {

            var randomScheduleStaff = mutableSchedule.keys.random()
            var randomScheduleDay = mutableSchedule[randomScheduleStaff]?.keys?.random()
            while(mutableSchedule[randomScheduleStaff]?.get(randomScheduleDay!!.toInt())!! == "PH"){
                randomScheduleStaff = mutableSchedule.keys.random()
                randomScheduleDay = mutableSchedule[randomScheduleStaff]?.keys?.random()
            }
            if (randomScheduleDay != null) {
                mutableSchedule[randomScheduleStaff]?.set(randomScheduleDay.toInt(), "")
            }

        }
        return mutableSchedule
    }

    private fun repairSolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var repairedSchedule = deepCopySolution(schedules)

        for (staffId in repairedSchedule.keys) {
            for (dayId in repairedSchedule[staffId]!!.keys) {
                if (repairedSchedule[staffId]?.get(dayId) == ""){
                    if(staffId == "Staff_1" || staffId == "Staff_3" || staffId == "Staff_6"){
                        repairedSchedule[staffId]?.set(dayId, data.shifts.filterNot {
                            it.id == "PH" ||
                                    it.id == "A2" ||
                                    it.id == "M2"}.random().id
                        )
                    }
                    else{
                        repairedSchedule[staffId]?.set(dayId, data.shifts.filterNot {
                            it.id == "PH" ||
                                    it.id == "M3" }.random().id
                        )
                    }
                }
            }
        }
        return repairedSchedule
    }

    private fun randomSwapStaffShift(currentSolution: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var newSolution = randomDestroySolution(currentSolution)
        newSolution = repairSolution(newSolution)
        return newSolution
    }

    private fun routeWheel(index: Int): Int{
        if (index % 400 == 0){
            resetWeightOperators()
        }
        else {
            createWeightOperators()
        }

        var rand = Random.nextDouble()
        var S = 0.0

        for (index in 0 until this.operatorWeight.size){
            S += this.operatorWeight.get(index)!!
        }
        this.probabilitiesOfOperator.set(0 , this.operatorWeight.get(0)!!/S)
        for (index in 1 until this.operatorWeight.size){
            this.probabilitiesOfOperator.set(index, this.probabilitiesOfOperator.get(index - 1)!! + this.operatorWeight.get(index)!!/S)
        }

        var choseValue = 0

        if (rand <= this.probabilitiesOfOperator.get(0)!!){
            choseValue = 0
        }
        else{
            for(index in 1 until this.operatorScore.size ){
                if (rand > this.probabilitiesOfOperator.get(index - 1)!! && rand <= this.probabilitiesOfOperator.get(index)!!){
                    choseValue = index
                }
            }
        }
        this.operatorTimes.set(choseValue, this.operatorTimes.get(choseValue)!! + 1)
        return choseValue
    }

    private fun shakeAndRepair(schedules: MutableMap<String, MutableMap<Int, String>>, number: Int): MutableMap<String, MutableMap<Int, String>>{
        when (number){
            0 -> return randomSwapStaffShift(schedules)
            1 -> return randomSwapStaffShift(schedules)
            2 -> return randomSwapStaffShift(schedules)
        }
        return schedules
    }

    fun runIteration(){
        var currentSolution = deepCopySolution(schedulesIter1)
        this.bestSolution = deepCopySolution(currentSolution)
        createScoreOperator()
        createOperatorTimes()

        for (index in 1..this.numberIterations) {
            val operatorIndex = routeWheel(index)
            var nextSolution = shakeAndRepair(currentSolution, operatorIndex)
            currentSolution = calculateSimulatedAnealing(currentSolution, nextSolution)
            if (calculate.totalScore(currentSolution) > calculate.totalScore(this.bestSolution)){
                this.bestSolution = deepCopySolution(currentSolution)
            }
        }
        scoring()
    }

}