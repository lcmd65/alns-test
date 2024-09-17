package com.ft.aio.template.adapter.output.web.scrippt.constrain

import com.ft.aio.template.adapter.output.web.scrippt.engine.CreateConObjs
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import kotlin.math.abs
import kotlin.math.max

class CommonCaculate (var data: InputData, var schedule: MutableMap<String, MutableMap<Int, String>>) {

    fun createConstrainScore(){
        for (constrain in data.constrains){
            when(constrain.id) {

                "exactly-staff-working-time" -> {
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in data.staffs) {
                            var staffWokringTime : Double = 0.0
                            for (day in 1 .. 7){
                                staffWokringTime += data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                            }
                            input.set(staff.id + week.toString(), staffWokringTime)
                        }
                    }
                    constrain.caculateScore(input)
                }

                "archive-0.5-day" -> {
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in data.staffs) {
                            var staffWorkingTime : Double = 0.0
                            for (day in 1 .. 7){
                                var temp = data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                                if (temp != 4){
                                    staffWorkingTime += 1
                                }
                                else {
                                    staffWorkingTime += 0.5
                                }
                            }
                            input.set(staff.id + week.toString(), staffWorkingTime)
                        }                    }
                    constrain.caculateScore(input)
                }

                "un-archive-0.5-day" -> {
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in data.staffs) {
                            var staffWorkingTime : Double = 0.0
                            for (day in 1 .. 7){
                                var temp = data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                                if (temp != 4){
                                    staffWorkingTime += 1
                                }
                                else {
                                    staffWorkingTime += 0.5
                                }
                            }
                            input.set(staff.id + week.toString(), staffWorkingTime)
                        }
                    }
                    constrain.caculateScore(input)
                }
            }
        }
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

    private fun caculateCoverageFullfillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: String,
        dayId: Int,
        week: Int
    ): Int {
        val coverage = data.coverages.find { it.id == coverageId && it.day == dayId }
        var temp = 0
        if (coverage != null){
            for (staff in data.staffs){
                if (schedules[staff.id]?.get(dayId + 7*( week - 1)) in coverage.shift && checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
                    temp += 1
                }
            }
        }

        return temp
    }

    private fun caculateHorizontalCoverageFullfillment(
        schedules: MutableMap<String, MutableMap<Int, String>>,
        coverageId: Int
    ): MutableMap<Int, MutableMap<String, Int>>{
        var horizontalMap : MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()
        for (week in 1 .. data.schedulePeriod){
            var temp: MutableMap<String, Int> = mutableMapOf()
            val coverage = data.horizontalCoverages.find { it.id == coverageId }
            for (staff in data.staffs){
                temp.set(staff.id, 0)
                if (coverage != null) {
                    for (day in coverage.days) {
                        if (coverage != null) {
                            if (schedules[staff.id]?.get(day + 7*(week-1))!! in coverage.shifts && day in coverage.days) {
                                temp.set(staff.id, temp.get(staff.id)!! + 1)
                            }
                        }
                    }
                }
            }
            horizontalMap.set(week, temp)
        }
        return horizontalMap
    }

    private fun caculateScore(schedules: MutableMap<String, MutableMap<Int, String>>): Double {
        var scores = 0

        // coverage
        for (week in 1.. data.schedulePeriod) {
            for (coverage in data.coverages) {
                if (coverage.type.contains("hard") && coverage.type.contains("equal to")) {
                    scores += abs(
                        coverage.desireValue -
                                caculateCoverageFullfillment(
                                    schedules,
                                    coverage.id,
                                    coverage.day,
                                    week
                                )
                    ) * coverage.penalty
                } else if (coverage.type.contains("hard") && coverage.type.contains("at least")) {
                    scores += max(
                        0,
                        coverage.desireValue - caculateCoverageFullfillment(
                            schedules,
                            coverage.id,
                            coverage.day,
                            week
                        )
                    ) * coverage.penalty
                } else if (coverage.type.contains("soft") && coverage.type.contains("at least")) {
                    scores += max(
                        0,
                        coverage.desireValue - caculateCoverageFullfillment(
                            schedules,
                            coverage.id,
                            coverage.day,
                            week
                        )
                    ) * coverage.penalty
                }
            }
        }

        // horizontal coverage
        for (coverage in data.horizontalCoverages) {
            var fullHorizontalCoverage = caculateHorizontalCoverageFullfillment(schedules, coverage.id)
            for (horizontalCoverage in fullHorizontalCoverage.values) {
                if (coverage.type.contains("hard") && coverage.type.contains("equal to")) {
                    for (map in horizontalCoverage) {
                        scores += abs(coverage.desireValue - map.value) * coverage.penalty
                    }
                } else if (coverage.type.contains("soft") && coverage.type.contains("at least")) {
                    for (map in horizontalCoverage) {
                        scores += max(0, coverage.desireValue - map.value) * coverage.penalty
                    }
                }
            }
        }

        return scores.toDouble()
    }

    fun coverageScore():Double{
        var score = -caculateScore(schedule)
        return score
    }

    fun constrainScore(): Double{
        createConstrainScore()
        var score = 0.0
        for (constrain in data.constrains){
            score += constrain.score
        }
        return score
    }

    fun totalScore(): Double{
        var score = 0.0
        score += constrainScore()
        score += coverageScore()
        return score
    }
}