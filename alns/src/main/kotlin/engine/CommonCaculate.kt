package com.ft.aio.template.adapter.output.web.scrippt.engine

import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.rule.RuleViolation
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import kotlin.math.abs
import kotlin.math.max

class CommonCaculate (var data: InputData) {

    fun createConstrainScore(schedule: MutableMap<String, MutableMap<Int, String>>){
        for (constrain in data.constrains){
            constrain.covertKotlinFlag = false
            constrain.midSearch = true
            when(constrain.id) {
                "exactly-staff-working-time" -> {
                    var scores = 0.0
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {
                        if (constrain.staffGroup.contains("all_staffs")){
                            for (staff in data.staffs){
                                var staffWokringTime : Double = 0.0
                                for (day in 1 .. 7){
                                    staffWokringTime += data.shifts.find { it.id == schedule[staff.id]?.get(day + 7*(week - 1))}?.duration!!
                                }
                                input[staff.id] = staffWokringTime
                            }
                            scores += constrain.caculateScore(input)
                        }
                        else {
                            for (staff in constrain.staffGroup) {
                                var staffWokringTime: Double = 0.0
                                for (day in 1..7) {
                                    staffWokringTime += data.shifts.find { it.id == schedule[staff]?.get(day + 7 * (week - 1)) }?.duration!!
                                }
                                input[staff] = staffWokringTime
                            }
                            scores += constrain.caculateScore(input)
                        }
                    }
                    constrain.score = scores
                }

                "archive-0.5-day" -> {
                    var scores = 0.0
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in constrain.staffGroup) {
                            var staffWorkingTime : Double = 0.0
                            for (day in 1 .. 7){
                                var temp = data.shifts.find { it.id == schedule[staff]?.get(day + 7*(week - 1))}?.duration!!
                                if (temp != 4 && temp != 0){
                                    staffWorkingTime += 1
                                }
                                else if (temp == 0){
                                    staffWorkingTime += 0
                                }
                                else if (temp == 4){
                                    staffWorkingTime += 0.5
                                }
                            }
                            input[staff] = staffWorkingTime
                        }
                        scores += constrain.caculateScore(input)
                    }
                    constrain.score = scores
                }

                "un-archive-0.5-day" -> {
                    var scores = 0.0
                    var input : MutableMap<String, Double> = mutableMapOf()
                    for (week in 1..  data.schedulePeriod) {

                        for (staff in constrain.staffGroup) {
                            var staffWorkingTime : Double = 0.0
                            for (day in 1 .. 7){
                                var temp = data.shifts.find { it.id == schedule[staff]?.get(day + 7*(week - 1))}?.duration!!
                                if (temp != 4 && temp != 0){
                                    staffWorkingTime += 1
                                }
                                else if (temp == 0){
                                    staffWorkingTime += 0
                                }
                                else if (temp == 4){
                                    staffWorkingTime += 0.5
                                }
                            }
                            input[staff] = staffWorkingTime
                        }
                        scores += constrain.caculateScore(input)
                    }
                    constrain.score = scores
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

    fun caculateCoverageFullfillment(
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

    fun caculateHorizontalCoverageFullfillment(
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

    private fun calculateCoverageScore(schedules: MutableMap<String, MutableMap<Int, String>>): Double {
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
                    ) * coverage.penalty * coverage.priority
                } else if (coverage.type.contains("hard") && coverage.type.contains("at least")) {
                    scores += max(
                        0,
                        coverage.desireValue - caculateCoverageFullfillment(
                            schedules,
                            coverage.id,
                            coverage.day,
                            week
                        )
                    ) * coverage.penalty * coverage.priority
                } else if (coverage.type.contains("soft") && coverage.type.contains("at least")) {
                    scores += max(
                        0,
                        coverage.desireValue - caculateCoverageFullfillment(
                            schedules,
                            coverage.id,
                            coverage.day,
                            week
                        )
                    ) * coverage.penalty * coverage.priority
                }
            }
        }

        return scores.toDouble()
    }

    private fun calculateHorizontalCoverageScore(schedules: MutableMap<String, MutableMap<Int, String>>): Double {
        var scores = 0
        // horizontal coverage
        for (coverage in data.horizontalCoverages) {
            var fullHorizontalCoverage = caculateHorizontalCoverageFullfillment(schedules, coverage.id)
            for (horizontalCoverage in fullHorizontalCoverage.values) {
                if (coverage.type.contains("hard") && coverage.type.contains("equal to")) {
                    for (map in horizontalCoverage) {
                        scores += abs(coverage.desireValue - map.value) * coverage.penalty * coverage.priority
                    }
                } else if (coverage.type.contains("soft") && coverage.type.contains("at least")) {
                    for (map in horizontalCoverage) {
                        scores += max(0, coverage.desireValue - map.value) * coverage.penalty * coverage.priority
                    }
                }
            }
        }

        return scores.toDouble()
    }

    fun patternConstrainScore(schedule: MutableMap<String, MutableMap<Int, String>>): Double{
        var ruleViolation = RuleViolation(data)
        ruleViolation.calculateNumberPatternViolation(schedule)
        var numberViolation = ruleViolation.patternConstrainViolations
        var score = 0.0
        for ((priority, item) in numberViolation){
            for((key, value) in item) {
                score -= data.patternConstrains.find { it.id == key }!!.penalty!! * value * priority
            }
        }
        return score
    }

    fun coverageScore(schedule: MutableMap<String, MutableMap<Int, String>>):Double{
        var score = - calculateCoverageScore(schedule)
        return score
    }

    fun horizontalCoverageScore(schedule: MutableMap<String, MutableMap<Int, String>>):Double{
        var score = - calculateHorizontalCoverageScore(schedule)
        return score
    }

    fun constrainScore(schedule: MutableMap<String, MutableMap<Int, String>>): Double{
        createConstrainScore(schedule)
        var score = 0.0
        for (constrain in data.constrains){
            score += constrain.score * constrain.priority
        }
        return score
    }

    fun totalScore(schedule: MutableMap<String, MutableMap<Int, String>>): Double{
        var score = 0.0
        score += constrainScore(schedule)
        score += coverageScore(schedule)
        score += horizontalCoverageScore(schedule)
        score += patternConstrainScore(schedule)
        return score
    }
}