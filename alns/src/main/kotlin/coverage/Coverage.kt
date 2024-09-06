package com.ft.aio.template.adapter.output.web.scrippt.coverage

import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift
import com.ft.aio.template.adapter.output.web.scrippt.staff.StaffGroup
import com.ft.aio.template.adapter.output.web.scrippt.operator.CompareOperators
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff


data class Coverage(
    var staffGroups: List<String>,
    var shift: MutableList<String>,
    var day: Int,
    var type: List<String>,
    var desireValue: Int,
    var penalty: Int,
    var dayTypes: String,
    var id: String,
)