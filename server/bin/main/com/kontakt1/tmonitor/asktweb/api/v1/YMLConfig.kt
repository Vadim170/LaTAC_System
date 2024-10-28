package com.kontakt1.tmonitor.asktweb.api.v1

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
class YMLConfig {
    lateinit var fcmkey: String
}
