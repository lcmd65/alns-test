package com.ft.aio.template.adapter.output.web.scrippt.input

import com.ft.aio.template.adapter.output.web.scrippt.coverage.Coverage
import com.ft.aio.template.adapter.output.web.scrippt.shift.Shift
import com.ft.aio.template.adapter.output.web.scrippt.staff.Staff
import com.ft.aio.template.adapter.output.web.scrippt.staff.StaffGroup

import kotlin.random.Random
@Serializable
data class InputData (
    var staffs: List<Staff>,
    var staffsGroup: StaffGroup,
    var shifts: List<Shift>,
    var coverage: List<Coverage>,
)










