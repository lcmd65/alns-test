package com.ft.aio.template.adapter.output.web.scrippt.rule
import com.ft.aio.template.adapter.output.web.scrippt.constrain.Constraint
import com.ft.aio.template.adapter.output.web.scrippt.constrain.PatternConstrain
import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
class RuleViolation(var data: InputData){

    var patternConstrainViolations: MutableMap<Int, MutableMap<String, Int>> = mutableMapOf()
    var constrainViolations:MutableMap<String, Int> = mutableMapOf()

    fun checkConstrainViolation(constrain: Constraint, schedule: MutableMap<String, MutableMap<Int, String>>, week: Int, staff:String):Boolean{
        when (constrain.id){
            "exactly-staff-working-time" -> {
                var number = 0.0
                for ( day in 1.. 7){
                    number += data.shifts.find { it.id == schedule[staff]?.get(day + 7*(week -1))!!}?.duration!!
                }
                if (number == 44.0) {
                    return true
                }
                return false
            }
            "archive-0.5-day" -> {
                if (staff in constrain.staffGroup) {
                    var number = 0.0
                    for (day in 1..7) {
                        if (data.shifts.find { it.id == schedule[staff]?.get(day + 7 * (week - 1))!! }?.duration!! == 8 || data.shifts.find {
                                it.id == schedule[staff]?.get(
                                    day + 7 * (week - 1)
                                )!!
                            }?.duration!! == 7) {
                            number += 1
                        } else if (data.shifts.find { it.id == schedule[staff]?.get(day + 7 * (week - 1))!! }?.duration!! == 4) {
                            number += 0.5
                        }
                    }
                    if (number == 5.5) {
                        return true
                    }
                    return false
                }
            }
            "un-archive-0.5-day" -> {
                if (staff in constrain.staffGroup) {
                    var number = 0.0
                    for (day in 1..7) {
                        if (data.shifts.find { it.id == schedule[staff]?.get(day + 7 * (week - 1))!! }?.duration!! == 8 || data.shifts.find { it.id == schedule[staff]?.get(day + 7 * (week - 1))!! }?.duration!! == 7) {
                            number += 1.0
                        } else if (data.shifts.find { it.id == schedule[staff]?.get(day + 7 * (week - 1))!! }?.duration!! == 4) {
                            number += 0.5
                        }
                    }
                    if (number == 6.0) {
                        return true
                    }
                    return false
                }
            }
        }
        return false
    }

    fun checkPatternConstrainViolation(constrain: PatternConstrain, schedule: MutableMap<String, MutableMap<Int, String>>, week: Int, day: Int, staff: String):Boolean{
        var violation: Boolean = false
        constrain.parsingPattern()
        for (pattern in constrain.patternLists.values) {
            if (schedule[staff]?.get(day + 7 * (week - 1))!! == pattern[0]) {
                violation = true
                for (index in 1..pattern.size - 1) {
                    if (schedule[staff]?.get(day + 7 * (week - 1) + index)!! != pattern[0 + index]) {
                        violation = false
                        break
                    }
                }
                if(violation){
                    break
                }
            }
        }
        return violation
    }

    fun calculateNumberPatternViolation(schedule: MutableMap<String, MutableMap<Int, String>>) {
        for (constrain in data.patternConstrains) {
            constrain.parsingPattern()
            var numberViolation = 0
            for (pattern in constrain.patternLists.values) {
                if (constrain.staffGroup.contains("all_staffs")) {
                    for (staff in data.staffs) {
                        for (week in 1..data.schedulePeriod) {
                            var day = 1
                            while (day <= 7 - pattern.size + 1) {
                                var violation: Boolean
                                if (schedule[staff.id]?.get(day + 7 * (week - 1))!! == pattern[0]) {
                                    violation = true
                                    for (index in 1..pattern.size - 1) {
                                        if (schedule[staff.id]?.get(day + 7 * (week - 1) + index)!! != pattern[0 + index]) {
                                            violation = false
                                            break
                                        }
                                    }
                                    if (violation == true) {
                                        numberViolation += 1
                                    }
                                }
                                day += 1
                            }
                        }
                    }
                }
                else{
                    for (staffGroup in constrain.staffGroup){
                        for (staff in data.staffGroups.find { it.id == staffGroup }?.staffList!!){
                            for (week in 1..data.schedulePeriod) {
                                var day = 1
                                while (day <= 7 - pattern.size + 1) {
                                    var violation: Boolean
                                    if (schedule[staff]?.get(day + 7 * (week - 1))!! == pattern[0]) {
                                        violation = true
                                        for (index in 1..pattern.size - 1) {
                                            if (schedule[staff]?.get(day + 7 * (week - 1) + index)!! != pattern[0 + index]) {
                                                violation = false
                                                break
                                            }
                                        }
                                        if (violation == true) {
                                            numberViolation += 1
                                        }
                                    }
                                    day += 1
                                }
                            }
                        }
                    }
                }
            }
            var pair = mutableMapOf(Pair(
                first = constrain.id,
                second = numberViolation
            ))
            patternConstrainViolations.set(constrain.priority, pair)
        }

    }
}