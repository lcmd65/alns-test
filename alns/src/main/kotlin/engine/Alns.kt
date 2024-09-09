package com.ft.aio.template.adapter.output.web.script.engine.Alns

import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift
import com.ft.aio.template.adapter.output.web.scrippt.staff.StaffGroup
import java.nio.DoubleBuffer

import kotlin.random.Random
import kotlin.math.exp

open class Alns(val data: InputData) {
    var numberIterations: Int = 100
    var temperature: Double = 100.0
    var alpha: Double = 0.9
    var limit: Double = 1e-3
    var deltaE: Double = 0.0
    var score: Double = 0.0
    var probabilitiesOfOperator: MutableMap<String, MutableMap<Int, Double>> = mutableMapOf()
    var operatorScore: MutableMap<String, MutableMap<Int, Double>> = mutableMapOf()
    var operatorTimes: MutableMap<String, MutableMap<Int, Double>> = mutableMapOf()
    var operatorSelection: MutableMap<String, MutableMap<Int, Int>> = mutableMapOf()
    var solution: MutableMap<String, MutableMap<Int, String>> = mutableMapOf()

    private fun caculateScore(schedules: MutableMap<String, MutableMap<Int, String>>): Double {
        var score: Int = Int.MAX_VALUE

        for (coverage in data.coverages) {
            score -= caculateCoverageFulllillment(schedules, coverage.id, coverage.day) * coverage.penalty
        }
        return score.toDouble()
    }

    private fun createWeightOperators(){
        for (staff in data.staffs) {
            for (day in 1..7){
                if(day != 6 && day != 7) {
                    this.operatorScore[staff.id]?.set(day, 1.0)
                }
                else{
                    this.operatorScore[staff.id]?.set(day, 0.5)
                }
            }
        }
    }

    private fun createOperatorTimes(){
        for (staff in data.staffs) {
            for (day in 1..7) {
                this.operatorTimes[staff.id]?.set(day, 1.0)
            }
        }
    }

    private fun createProbabilitiesOfOperator(){
        for (staff in data.staffs) {
            for (day in 1..7) {
                val probabilities = this.probabilitiesOfOperator[staff.id]?.get(day)
                if (probabilities != null) {
                    this.probabilitiesOfOperator[staff.id]?.set(day,
                        probabilities * 0.6 + 0.4 * this.operatorScore[staff.id]?.get(day)!! / this.operatorTimes[staff.id]?.get(day)!!
                    )
                }
                else {
                    this.probabilitiesOfOperator[staff.id]?.set(day, 0.6 + 0.4 * this.operatorScore[staff.id]?.get(day)!! / this.operatorTimes[staff.id]?.get(day)!!)
                }
            }
        }
    }

    private fun createOperatorSelection(){
        for (staff in data.staffs) {
            for (day in 1..7) {
                this.operatorSelection[staff.id]?.set(day, 0)
                val acceptanceVariable = Random.nextDouble(0.0, 1.0)
                if (acceptanceVariable <= this.probabilitiesOfOperator[staff.id]?.get(day)!!){
                    this.operatorSelection[staff.id]?.set(day, 1)
                    val temp = this.operatorTimes[staff.id]?.get(day)?.toDouble()?.plus(1.0)
                    if (temp != null) {
                        this.operatorTimes[staff.id]?.set(day, temp)
                    }
                }
                else{
                    this.operatorSelection[staff.id]?.set(day, 0)
                }
            }
        }
    }

    private fun caculateSimulatedAnealing(
        currentScheduled: MutableMap<String, MutableMap<Int, String>>,
        nextScheduled:MutableMap<String, MutableMap<Int, String>>
    ): MutableMap<String, MutableMap<Int, String>> {
        deltaE = caculateScore(currentScheduled)- caculateScore(nextScheduled)
        if (deltaE < 0){
            return nextScheduled
        }
        else {
            if (temperature < limit) {
                return currentScheduled
            }
            val probability = exp(deltaE / temperature)
            val acceptanceVariable = Random.nextDouble(0.0, 1.0)

            temperature = temperature * alpha
            if (probability > acceptanceVariable) {
                return currentScheduled
            } else {
                return nextScheduled
            }
        }
    }

    private fun getShiftInfoFromCoverage(coverageId: String): String {
        return coverageId.take(2)
    }

    private fun checkIfStaffInStaffGroup(
        staff: Staff,
        staffGroups: List<String>
    ): Boolean {
        var result = false
        for (staffGroupId in staffGroups) {
            val staffGroup = data.staffGroups.find { it.id == staffGroupId }

            if (staffGroup != null) {
                for (staffInfo in staffGroup.staffList) {
                    if (staff.id == staffInfo) {
                        result = true
                        break
                    }
                }
            }

            if (result) {
                break
            }
        }
        return result
    }

    private fun caculateCoverageFulllillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: String, dayId: Int
    ): Int {
        val coverage = data.coverages.find { it.id == coverageId && it.day == dayId }
        var temp = 0
        if (coverage != null){
            for (staff in data.staffs){
                if (schedules[staff.id]?.get(dayId) == getShiftInfoFromCoverage(coverageId) && checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                    temp += 1
                }
            }
        }

        return temp
    }

    private fun inititalSolution(): MutableMap<String, MutableMap<Int, String>> {
        val schedule : MutableMap<String, MutableMap<Int, String>>
        schedule = mutableMapOf()

        // create blank schedule for caculating
        for (staff in data.staffs) {
            schedule[staff.id] = mutableMapOf()
            for (day in 1..7){
                schedule[staff.id]?.set(day, "")
            }
        }

        for (coverage in data.coverages) {
            for (staff in data.staffs) {
                if(caculateCoverageFulllillment(schedule, coverage.id, coverage.day) < coverage.desireValue &&
                    checkIfStaffInStaffGroup(staff, coverage.staffGroups) &&
                    checkIfStaffInStaffGroup(staff, coverage.staffGroups) &&
                    schedule[staff.id]?.get(coverage.day) == ""){
                    schedule[staff.id]?.set(coverage.day, coverage.shift.random())
                }
            }
        }
        for (coverage in data.coverages){
            for (staff in data.staffs){
                if(schedule[staff.id]?.get(coverage.day) == ""){
                    schedule[staff.id]?.set(coverage.day, data.shifts.random().id)
                }
            }
        }

        return schedule
    }

    private fun routewheel(){
        createProbabilitiesOfOperator()
        createOperatorSelection()
    }

    private fun randomDestroySolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        val mutableSchedule = schedules.toMutableMap()
        if (mutableSchedule.isNotEmpty()) {

            val randomScheduleStaff = mutableSchedule.keys.random()
            val randomScheduleDay = mutableSchedule[randomScheduleStaff]?.keys?.random()
            if (randomScheduleDay != null) {
                mutableSchedule[randomScheduleStaff]?.set(randomScheduleDay.toInt(), "")
            }
        }
        return mutableSchedule
    }

    private fun routewheelDestroySolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        routewheel()
        val mutableSchedule = schedules.toMutableMap()
        if (mutableSchedule.isNotEmpty()) {

            for (staff in data.staffs){
                for (day in 1..7){
                    if (this.operatorSelection[staff.id]?.get(day) == 1){
                        mutableSchedule[staff.id]?.set(day, "")
                    }
                }
            }
        }
        return mutableSchedule
    }

    private fun repairSolution(schedules: MutableMap<String, MutableMap<Int, String>>): MutableMap<String, MutableMap<Int, String>> {
        var repairedSchedule = schedules.toMutableMap()

        for (staffId in repairedSchedule.keys) {
            for (dayId in repairedSchedule[staffId]!!.keys) {
                if (repairedSchedule[staffId]?.get(dayId) == ""){
                    repairedSchedule[staffId]?.set(dayId, data.shifts.random().id)
                }
            }
        }
        return repairedSchedule
    }

    open fun runAlns(){
        var initialSolution = inititalSolution()
        var currentSolution = initialSolution
        createWeightOperators()
        createOperatorTimes()

        try {
            for (i in 1..numberIterations) {
                var tempSolution = currentSolution
                //currentSolution = randomDestroySolution(currentSolution)
                currentSolution = routewheelDestroySolution(currentSolution)
                currentSolution = repairSolution(currentSolution)
                currentSolution = caculateSimulatedAnealing(tempSolution, currentSolution)
            }
        }
        catch (e: Exception){}

        this.solution = currentSolution
        this.score = caculateScore(this.solution)
    }
}