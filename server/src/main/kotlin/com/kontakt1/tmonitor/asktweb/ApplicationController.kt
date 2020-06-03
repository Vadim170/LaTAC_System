package com.kontakt1.tmonitor.asktweb

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.kontakt1.tmonitor.systems.System
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import java.io.FileInputStream
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
        initFCM()
        runBlocking {
            val connection = dataSource.connection
            system.readStruct(connection)
            connection.close()
        }
        backgroundUpdater.launchBackgroundRefresh()
    }
}

