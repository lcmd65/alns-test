package com.ft.aio.template.adapter.output.web.scrippt

import com.ft.aio.template.adapter.output.web.scrippt.input.InputData
import com.ft.aio.template.adapter.output.web.script.engine.Alns.Alns
import java.io.File
import com.ft.aio.template.adapter.output.web.scrippt.engine.PreProcess
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    var data = PreProcess().dataPreprocessing()
    var optimizer = Alns(data)
    println(optimizer.runAlns())
}