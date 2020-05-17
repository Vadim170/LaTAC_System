package com.kontakt1.tmonitor.asktweb.webui

import com.kontakt1.tmonitor.dataClasses.CompleteParam
import com.kontakt1.tmonitor.systems.System
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.sql.DataSource

/**
 * Контроллер для веб интерфейса.
 */
@Controller
class WebRequestController {
    @Autowired
    lateinit var dataSource: DataSource
    @Autowired
    lateinit var system: System

    @GetMapping
    fun index(model: MutableMap<String, Any>) : String {
        model["groups"] = system.silabus.listSilo
        model["mnemoschems"] = system.mnemoschems
        return "index"
    }


    @GetMapping(value = ["/mnemoscheme"])
    fun mnemoscheme(
            @RequestParam(value = "mnemoscheme", required = false, defaultValue = "null") name: String,
            model: MutableMap<String, Any>) : String {
        model["mnemo"] = system.findDataMnemoschemeOrEmpty(name)
        return "mnemoscheme"
    }

    @GetMapping(value = ["/params"])
    fun params(
            @RequestParam(value = "silo", required = false, defaultValue = "null") siloName: String,
            model: MutableMap<String, Any>
    ) : String {
        model["siloName"] = siloName
        model["completeParams"] = system.silabus.listSilo.find { it.name == siloName }?.params ?: emptyList<CompleteParam>()
        return "params"
    }

    @GetMapping(value = ["/indications"])
    fun lindications(
            @RequestParam(value = "paramType", required = true, defaultValue = "") paramType: String,
            @RequestParam(value = "paramId", required = true) paramId: Int?,
            model: MutableMap<String, Any>
    ) : String {
        val param = paramId?.let { system.silabus.findLParamById(it) }
        model["paramType"] = paramType
        model["paramName"] = param?.name ?: "Параметр не найден"
        model["paramId"] = param?.id ?: "null"
        return "indications"
    }
}