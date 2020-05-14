package com.kontakt1.tmonitor.systems.askubuk01

import com.kontakt1.tmonitor.dataClasses.CompleteParam
import com.kontakt1.tmonitor.dataClasses.Silo
import com.kontakt1.tmonitor.dataClasses.params.LParam
import java.sql.Connection

class SilosAskuBUK01Reader {
    companion object {
        suspend fun read(connection: Connection, numberReadAttempts: Int = 5): List<Silo> {
            for (i in 1..numberReadAttempts) { // Делаем numberReadAttempts попыток
                val silos = mutableListOf<Silo>()
                val stmt = connection.createStatement()
                val resultset = stmt.executeQuery(MYSQL_GET_GROUPS)
                while (resultset.next()) {
                    val id = resultset.getInt("RezGroupID")
                    val name = resultset.getString("Name")

                    val params = getCompleteParams(connection, id)
                    val silo = Silo(
                        dir = "",
                        name = name,
                        params = params
                    )
                    silos.add(silo)
                }
                return silos
            }
            return listOf<Silo>()
        }

        private fun getCompleteParams(
            connection: Connection,
            parentId: Int
        ): MutableList<CompleteParam> {
            val params = mutableListOf<CompleteParam>()
            val stmt = connection.createStatement()
            val query = "SELECT * FROM Rez LEFT JOIN RezValues ON Rez.RezID = RezValues.RezID " +
                    "WHERE RezGroupID = $parentId AND SUBSTRING_INDEX(RezValues.ValueID,'_',-1) = 'LevelMetr' ORDER BY RezGroupID; "
            val resultSet = stmt.executeQuery(query)
            while (resultSet.next()) {
                val id = resultSet.getInt("RezID")
                val name = resultSet.getString("Name")
                val enabled = resultSet.getBoolean("Enabled")
                val description = resultSet.getString("Description")
                val rezType = resultSet.getString("RezType")
                val maxLevel = resultSet.getInt("MaxVal")/1000
                params.add(
                    CompleteParam(
                        name = name,
                        lParam = LParam(
                            id = id,
                            alias = name,
                            name = name,
                            parent = parentId,
                            constraintsList = listOf(),
                            range = maxLevel
                        ),
                        description = description,
                        enabled = enabled,
                        rezType = rezType
                    )
                )
            }
            return params
        }

        val MYSQL_GET_GROUPS = "SELECT * FROM RezGroups ORDER BY RezGroupID"
    }
}