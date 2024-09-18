package com.ft.aio.template.adapter.output.web.scrippt


import com.ft.aio.template.adapter.output.web.scrippt.engine.Engine
import com.ft.aio.template.adapter.output.web.scrippt.engine.PreProcess

//import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.boot.runApplication

//@SpringBootApplication
//class SpringBootTemplateApplication

//fun main(args: Array<String>) {
    //runApplication<SpringBootTemplateApplication>(*args)
//}
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {

    var data = PreProcess().dataPreprocessing()
    data.validateInputData()

    var engine = Engine(data)
    engine.init()
    engine.printSolution()
    engine.saveSolution()
}