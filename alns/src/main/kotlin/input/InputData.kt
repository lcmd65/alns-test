package com.ft.aio.template.adapter.output.web.scrippt.input

import com.ft.aio.template.adapter.output.web.scrippt.coverage.Coverage
import com.ft.aio.template.adapter.output.web.scrippt.coverage.HorizontalCoverage
import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.staff.StaffGroup

import kotlin.random.Random


data class InputData (
    var staffs: List<Staff>,
    var staffGroups: List<StaffGroup>,
    var shifts: List<Shift>,
    var coverages: List<Coverage>,
    var horizontalCoverages: List<HorizontalCoverage>
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










