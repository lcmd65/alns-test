package com.ft.aio.template.adapter.output.web.script.engine.Alns

import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift
import com.ft.aio.template.adapter.output.web.scrippt.staff.StaffGroup

import kotlin.random.Random
import kotlin.math.exp

open class Alns(val data: InputData) {

    var numberIterations: Int = 100
    var temperature: Double = 100.0
    var alpha: Double = 0.9
    var limit: Double = 1e-3
    var deltaE: Double = 0.0

    //score 𝑔(𝑖,𝑗+1)  := 𝑔(i,j)(1-ρ) + ρ Score_i/Time_i

    open fun caculateScore(scheduled: MutableMap<Int, MutableList<Staff>>): Double{
        //TODO

        return -1.0
    }

    open fun caculateSimulatedAnealing(currentScheduled: MutableMap<Int, MutableList<Staff>>, nextScheduled:MutableMap<Int, MutableList<Staff>>): MutableMap<Int, MutableList<Staff>>{
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

    fun getShiftInfoFromCoverage(coverageId: String): String{
        return coverageId.take(2)
    }

    fun checkIfStaffInStaffGroup(staff: Staff, staffGroups: List<String>): Boolean{
        var result: Boolean = false
        for (staffGroupId in staffGroups) {
            for (staffInfo in data.staffsGroup.find{ it.id == staffGroupId }?.staffList!!) {
                if (staff.id == staffInfo.id) {
                    result == true
                }
            }
        }
        return result
    }

    fun caculateCoverageFulllillment(schedules: MutableMap<String, MutableMap<Int, String>>, coverageId: String, dayId:Int): Int{
        val coverage = data.coverages.find { it.id == coverageId && it.day == dayId }
        var temp =0
        if (coverage != null){
            for (staff in data.staffs){
                if (schedules[staff.id]?.get(dayId) == getShiftInfoFromCoverage(coverageId) && checkIfStaffInStaffGroup(staff, coverage.staffGroup)) {
                    temp += 1
                }
            }
        }

        return temp
    }

    open fun inititalSolution(): MutableMap<String, MutableMap<Int, String>>{
        val schedule : MutableMap<String, MutableMap<Int, String>>
        schedule = mutableMapOf()

        // create blank schedule for caculating
        for (staff in data.staffs) {
            schedule[staff.id] = mutableMapOf()
            for (day in 1..7){
                schedule[staff.id]?.set(day, "")
            }
        }
        var temp = 0

        for (coverage in data.coverages) {
            for (staff in data.staffs) {
                if(caculateCoverageFulllillment(schedule, coverage.id, coverage.day) < coverage.desireValue && staff.)
            }
        }
            if (shift.employeesNeeded > schedule[shift.id]!!.size && temp <= data.employees.size) {
                    for (staff in data.employees) {
                        if (schedule[shift.id]?.none { it.id == staff.id } == true) {
                            // add this staff to list
                            schedule[shift.id]?.add(staff)

                            if (schedule[shift.id]!!.size >= shift.employeesNeeded) {
                                break
                            }
                        }
                    }

            temp += 1
            }
        }
        return schedule
    }

    open fun destroySolution(schedule: MutableMap<Int, MutableList<Staff>>): MutableMap<Int, MutableList<Staff>> {
        val mutableSchedule = schedule.toMutableMap()
        if (mutableSchedule.isNotEmpty()) {
            val randomScheduleShift = mutableSchedule.keys.random()
            mutableSchedule[randomScheduleShift] = mutableListOf()
        }
        return mutableSchedule
    }

    open fun repairSolution(schedule: MutableMap<Int, MutableList<Staff>>, originalSet: Set<Int>): MutableMap<Int, MutableList<Staff>> {
        var repairedSchedule = schedule.toMutableMap()

        for (shift_id in repairedSchedule.keys) {
            var requiredEmployees = data.shifts.find { it.id == shift_id }?.employeesNeeded ?: 0
            var currentEmployees = repairedSchedule[shift_id]?.size ?: 0

            if (currentEmployees < requiredEmployees) {
                var availableEmployees = data.employees.filter { it.id in originalSet && it.skill == data.shifts[shift_id].requiredSkill }
                var employeesToAdd = availableEmployees.take(requiredEmployees - currentEmployees)
                repairedSchedule[shift_id] = (repairedSchedule[shift_id]?.toMutableList() ?: mutableListOf()).apply {
                    addAll(employeesToAdd)
                }
            }
        }
        return repairedSchedule
    }

    open fun runAlns(): MutableMap<Int, MutableList<Staff>>{
        var initialSolution = inititalSolution()
        for (item in initialSolution){
            //println(item.value)
        }
        var currentSolution = initialSolution
        try {
            for (i in 1..numberIterations) {
                var tempSolution = currentSolution
                currentSolution = destroySolution(currentSolution)

                val employeeIds = data.employees.map { it.id }.toSet()
                currentSolution = repairSolution(currentSolution, employeeIds)
                currentSolution = caculateSimulatedAnealing(tempSolution, currentSolution)
            }
        }
        catch (e: Exception){}

        return currentSolution
    }
}
