package com.kontakt1.tmonitor.asktweb

import com.kontakt1.tmonitor.systems.System
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.annotation.PostConstruct
import javax.sql.DataSource

/**
 * После сборки бинов спрингом запускает фоновый опрос и считывает структуру.
 */
@Controller
class ApplicationController {
    @Autowired
    lateinit var dataSource: DataSource
    @Autowired
    lateinit var system: System
    @Autowired
    private lateinit var backgroundUpdater: BackgroundUpdater

    @PostConstruct
    fun postConstruct() {
        runBlocking {
            val connection = dataSource.connection
            system.readStruct(connection)
            connection.close()
            println("PostConstruct: Is closed: ${connection.isClosed}")
        }
        backgroundUpdater.launchBackgroundRefresh()
    }
}

