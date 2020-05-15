package com.kontakt1.tmonitor.asktweb

import com.kontakt1.tmonitor.systems.askt01.Askt01
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

/**
 * Задает систему и параметры подключения по умолчанию.
 */
@Configuration
class ApplicationConfiguration {
    @Bean
    fun system() = Askt01()

    /**
     * Настройка конфигурации подключения к бд перенесена в файл конфигурации,
     * аддресс которого передается спрингу через параметр запуска
     */
//    @Bean
//    @Primary
//    fun dataSource(): DataSource {
//        return DataSourceBuilder.create()
//                .username("root")
//                .password("")
//                .url("jdbc:mysql://127.0.0.1:3306/tmonitor_test?useLegacyDatetimeCode=false")
//                .build()
//    }
//spring.datasource.url=jdbc:mysql://127.0.0.1:3306/tmonitor_test?useLegacyDatetimeCode=false
//spring.datasource.username=root
//spring.datasource.password=
}