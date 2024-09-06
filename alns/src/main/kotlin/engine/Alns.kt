package com.ft.aio.template.adapter.output.web.script.engine.Alns

import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData

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

    open fun inititalSolution(): MutableMap<Int, MutableList<Staff>>{
        val schedule : MutableMap<Int, MutableList<Staff>>
        schedule = mutableMapOf()
        var temp = 0
        for (shift in data.shifts) {
            schedule[shift.id] = mutableListOf()

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
            }
            temp += 1
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
