package com.ft.aio.template.adapter.output.web.scrippt.engine

import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import kotlin.math.abs
import kotlin.math.max

class CalculateScore(var data: InputData, var schedule: MutableMap<String, MutableMap<Int, String>>) {

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
                if (schedules[staff.id]?.get(dayId + 7*( week - 1)) == getShiftInfoFromCoverage(coverageId) && checkIfStaffInStaffGroup(staff, coverage.staffGroups)) {
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


    fun score(): Double{
        var score = -caculateScore(schedule)
        CreateConObjs(data).createConstrainScore(schedule)
        for (constrain in data.constrains){
            score += constrain.score
        }
        return score
    }
}