package com.kontakt1.tmonitor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.DriverManager

/**
 * Example local unit test, which will execute on the development machine (host).
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        val url = "jdbc:mysql://localhost:3306/mysql"
        val con = DriverManager.getConnection(url, "root", "")
        println(con.isClosed)
    }
}
