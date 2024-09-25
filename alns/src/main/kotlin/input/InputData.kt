package com.ft.aio.template.adapter.output.web.scrippt.input

import com.ft.aio.template.adapter.output.web.scrippt.coverage.Coverage
import com.ft.aio.template.adapter.output.web.scrippt.coverage.HorizontalCoverage
import com.ft.aio.template.adapter.output.web.scrippt.constrain.Constraint
import com.ft.aio.template.adapter.output.web.scrippt.constrain.Objective
import com.ft.aio.template.adapter.output.web.scrippt.constrain.PatternConstrain
import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift
import com.ft.aio.template.adapter.output.web.scrippt.shift.ShiftGroup
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.staff.StaffGroup
import com.ft.aio.template.adapter.output.web.scrippt.utils.Day
import com.ft.aio.template.adapter.output.web.scrippt.utils.Holiday


data class InputData (
    var schedulePeriod: Int,
    var startDate: Day,
    var publicHolidays: List<Holiday>,
    var staffs: List<Staff>,
    var staffGroups: List<StaffGroup>,
    var shifts: List<Shift>,
    var coverages: List<Coverage>,
    var horizontalCoverages: List<HorizontalCoverage>,
    var objs : List<Objective>,
    var constrains: List<Constraint>,
    var patternConstrains: List<PatternConstrain>,
    var shiftGroups: List<ShiftGroup>
)
{
    fun validateInputData(): Boolean {
        val identifier = "[ValidateInputData]"
        if (staffs.isEmpty()) {
            println("$identifier Staff list is empty")
            return false
        }

        if (staffGroups.isEmpty()) {
            println("$identifier StaffGroup list is empty")
            return false
        }

        if (shifts.isEmpty()) {
            println("$identifier Shift list is empty")
            return false
        }

        if (coverages.isEmpty()) {
            println("$identifier Coverage list is empty")
            return false
        }

        println("$identifier All input data lists are valid")
        return true
    }
}