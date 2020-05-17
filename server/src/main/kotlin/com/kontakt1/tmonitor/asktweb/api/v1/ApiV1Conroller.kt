package com.kontakt1.tmonitor.asktweb.api.v1

import com.kontakt1.tmonitor.asktweb.api.v1.DateTimeParser.Companion.dateTimeParse
import com.kontakt1.tmonitor.systems.System
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource

/**
 * Контроллер REST.
 */
@RestController
@RequestMapping("/api/v1")
class ApiV1Conroller {
    @Autowired
    lateinit var dataSource: DataSource
    @Autowired
    lateinit var system: System

    @GetMapping(value = ["/mnemoscheme"])
    fun mnemoscheme(
            @RequestParam(value = "name", required = true, defaultValue = "") name: String
    ) : String {
        return system.findDataMnemoschemeOrEmpty(name)
    }

    @GetMapping(value = ["/struct"], produces = ["application/json"])
    fun index() : String {
        return system.silabus.listSilo.toString()
    }

    @GetMapping(value = ["/indications"], produces = ["application/json"])
    fun getChartIndications(
            @RequestParam(value = "paramType", required = true, defaultValue = "") paramType: String,
            @RequestParam(value = "paramId", required = true) paramId: Int?,
            @RequestParam(value = "datetimefrom", required = false, defaultValue = "") dateTimeFromString: String,
            @RequestParam(value = "datetimeto", required = false, defaultValue = "") dateTimeToString: String
    ): String {
        val param = system.silabus.getParam(paramId, paramType)
        val dateTimeFrom = dateTimeParse(dateTimeFromString)
        val dateTimeTo = dateTimeParse(dateTimeToString)
        if (param != null) {
            val connection = dataSource.connection
            val series = system.getJSONListSeriesOfCharts(connection, dateTimeFrom, dateTimeTo, param)
            connection.close()
            println("API: Is closed: ${connection.isClosed}")
            return "{ \"name\": \"${param.alias}\",\"title\": \"Параметр ${param.alias}\", \"series\": $series}"
        }
        return ""
    }

    @GetMapping(value = ["/indicationsLast"], produces = ["application/json"])
    fun getChartIndicationsLast(
            @RequestParam(value = "paramType", required = true, defaultValue = "") paramType: String,
            @RequestParam(value = "paramId", required = true) paramId: Int?
    ): String {
        val param = system.silabus.getParam(paramId, paramType)
        if (param != null) {
            val connection = dataSource.connection
            val series = system.getJSONSeriesOfDiagram(connection, param)
            connection.close()
            println("API: Is closed: ${connection.isClosed}")
            return "{ \"name\": \"${param.alias}\",\"title\": \"Параметр ${param.alias}\", \"series\": $series}"
        }
        return ""
    }
}