package com.kontakt1.tmonitor.systems.askt01

import com.kontak1.tmonitor.asktweb.dataClasses.Mnemoscheme
import com.kontakt1.tmonitor.dataClasses.*
import com.kontakt1.tmonitor.dataClasses.params.LDDownParam
import com.kontakt1.tmonitor.dataClasses.params.LDUpParam
import com.kontakt1.tmonitor.dataClasses.params.LParam
import com.kontakt1.tmonitor.dataClasses.params.TParam
import java.lang.StringBuilder
import java.sql.*

/**
 * Класс для загрузки структуры хранилища системы АСКТ-01.
 * @author Makarov V.G.
 */
class StructASKT01Reader {
    companion object {
        // TODO Упрости, сократи, разбей. Этот метод ужасен
        suspend fun read(connection: Connection, numberReadAttempts: Int = 5): List<Silo> {
            for (i in 1..numberReadAttempts) { // Делаем numberReadAttempts попыток
                val stmt = connection.createStatement()
                try {
                    val constraintsList = readConstraints(stmt)
                    val lConstraintsUsageList = readLConstraintsUsageList(stmt)
                    val tConstraintsUsageList = readTConstraintsUsageList(stmt)
                    val lParamList = readLParams(stmt, lConstraintsUsageList, constraintsList)
                    val tParamList = readTParams(stmt, tConstraintsUsageList, constraintsList)
                    val ldUpParamList = readLDUpParams(stmt)
                    val ldDownParamList = readLDDownParams(stmt)
                    val completeParamList =
                            getCompleteParamsList(tParamList, lParamList, ldUpParamList, ldDownParamList)
                    // Считаем количество родителей параметров
                    // Так как парамеры разных силосов лежат в раздых "папках", то их родители разные
                    // Таким образом узнаем количество силосов
                    val parents = getAllParentsParams(tParamList, lParamList, ldUpParamList, ldDownParamList)
                    return parents.map { parent ->
                        val pair = readNameAndDirSilo(stmt, parent)
                        val nodeName = pair.first
                        val dir = pair.second
                        val params = completeParamList.filter { completeParam ->
                            completeParam.lParam?.parent == parent ||
                                    completeParam.tParam?.parent == parent ||
                                    completeParam.ldUpParam?.parent == parent ||
                                    completeParam.ldDownParam?.parent == parent
                        }
                        Silo(nodeName, dir, params)
                    }
                } catch (sqlEx: SQLException) {
                    sqlEx.printStackTrace()
                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close()
                        } catch (sqlEx: SQLException) {
                        }
                    }
                }
            }
            return listOf<Silo>()
        }

        private suspend fun readNameAndDirSilo(
                stmt: Statement,
                parent: Int
        ): Pair<String, String> {
            // Считываем имя группы по id
            var request = "SELECT * FROM tree WHERE id = $parent"
            var resultset = stmt.executeQuery(request)
            resultset.next()
            val nodeName = resultset.getString("node_name")
            val leftIx = resultset.getString("left_ix")
            val rightIx = resultset.getString("right_ix")

            val dir = StringBuilder("/")
            // Считываем имя parent группы
            request =
                    "SELECT node_name FROM tree WHERE left_ix < $leftIx AND right_ix > $rightIx ORDER BY left_ix ASC"
            resultset = stmt.executeQuery(request)
            try {
                while (resultset.next()) {
                    dir.append("${resultset.getString("node_name")}/")
                }
            } catch (e: SQLException) {
            }
            return Pair(nodeName, dir.toString())
        }

        private suspend fun getAllParentsParams(
                tParamList: List<TParam>,
                lParamList: List<LParam>,
                ldUpParamList: List<LDUpParam>,
                ldDownParamList: List<LDDownParam>
        ) = lParamList.map { it.parent }
                .union(tParamList.map { it.parent })
                .union(ldUpParamList.map { it.parent })
                .union(ldDownParamList.map { it.parent })

        private suspend fun getCompleteParamsList(
                tParamList: List<TParam>,
                lParamList: List<LParam>,
                ldUpParamList: List<LDUpParam>,
                ldDownParamList: List<LDDownParam>
        ): List<CompleteParam> {
            val paramsNames =
                    getAllParamsNamesAndParents(
                            tParamList,
                            lParamList,
                            ldUpParamList,
                            ldDownParamList
                    )
            return paramsNames.map { (name, parent) ->
                CompleteParam(
                        name,
                        lParamList.find { it.name == name && it.parent == parent },
                        tParamList.find { it.name == name && it.parent == parent },
                        ldUpParamList.find { it.name == name && it.parent == parent },
                        ldDownParamList.find { it.name == name && it.parent == parent })
            }
        }

        private suspend fun getAllParamsNamesAndParents(
                tParamList: List<TParam>,
                lParamList: List<LParam>,
                ldUpParamList: List<LDUpParam>,
                ldDownParamList: List<LDDownParam>
        ) = lParamList.map { Pair(it.name, it.parent) }
                .union(tParamList.map { Pair(it.name, it.parent) })
                .union(ldUpParamList.map { Pair(it.name, it.parent) })
                .union(ldDownParamList.map { Pair(it.name, it.parent) })

        private suspend fun readLConstraintsUsageList(stmt: Statement): List<Pair<Int, Int>> {
            val lConstraintsUsageList = mutableListOf<Pair<Int, Int>>()
            val resultset = stmt.executeQuery(MYSQL_GET_LCONSTRAINTS_USAGE)
            while (resultset.next()) {
                // val id = resultset.getInt("id")
                val prm = resultset.getInt("prm_id")
                val cnstr = resultset.getInt("cnstr_id")
                lConstraintsUsageList.add(prm to cnstr)
            }
            return lConstraintsUsageList
        }

        private suspend fun readTConstraintsUsageList(stmt: Statement): List<Pair<Int, Int>> {
            val tConstraintsUsageList = mutableListOf<Pair<Int, Int>>()
            val resultset = stmt.executeQuery(MYSQL_GET_TCONSTRAINTS_USAGE)
            while (resultset.next()) {
                // val id = resultset.getInt("id")
                val prm = resultset.getInt("prm_id")
                val cnstr = resultset.getInt("cnstr_id")
                tConstraintsUsageList.add(prm to cnstr)
            }
            return tConstraintsUsageList
        }

        private suspend fun readConstraints(stmt: Statement): List<Constraint> {
            val resultset = stmt.executeQuery(MYSQL_GET_CONSTRAINTS)
            val constraintsList = mutableListOf<Constraint>()
            while (resultset.next()) {
                val id = resultset.getInt("id")
                val name = resultset.getString("cnstr_name")
                val using = resultset.getInt("cnstr_using")
                val value = resultset.getFloat("cnstr_value")
                val insens = resultset.getFloat("cnstr_insens")
                val direction = resultset.getInt("cnstr_direction")
                constraintsList.add(
                        Constraint(
                                id,
                                name,
                                Using.getByInt(using),
                                value,
                                insens,
                                Direction.getByInt(direction)
                        )
                )
            }
            return constraintsList
        }

        private suspend fun readLParams(
                stmt: Statement,
                lConstraintsUsageList: List<Pair<Int, Int>>,
                constraintsList: List<Constraint>
        ): List<LParam> {
            val lParamList = mutableListOf<LParam>()
            val resultset = stmt.executeQuery(MYSQL_GET_LPARAMS)
            while (resultset.next()) {
                val id = resultset.getInt("id")
                val alias = resultset.getString("l_alias")
                val name = resultset.getString("l_name")
                val parent = resultset.getInt("l_parent")
                val range = resultset.getInt("l_range")
                val constraintsListForParam = mutableListOf<ConstraintApplication>()
                lConstraintsUsageList.filter { it.first == id }
                        .forEach { pair ->
                            constraintsListForParam.addAll(
                                    constraintsList.filter { it.id == pair.second }
                                            .map { ConstraintApplication(it) })
                        }
                val lParam = LParam(
                        id,
                        alias,
                        name,
                        parent,
                        constraintsListForParam,
                        range
                )
                lParamList.add(lParam)
            }
            return lParamList
        }

        private suspend fun readTParams(
                stmt: Statement,
                tConstraintsUsageList: List<Pair<Int, Int>>,
                constraintsList: List<Constraint>
        ): List<TParam> {
            val tParamList = mutableListOf<TParam>()
            val resultset = stmt.executeQuery(MYSQL_GET_TPARAMS)
            while (resultset.next()) {
                val id = resultset.getInt("id")
                val alias = resultset.getString("t_alias")
                val name = resultset.getString("t_name")
                val parent = resultset.getInt("t_parent")
                val sensors = resultset.getInt("t_sensors")
                val constraintsListForParam = mutableListOf<ConstraintApplication>()
                tConstraintsUsageList.filter { it.first == id } // Смотрим пары, где id параметра совпадает
                        .forEach { pair ->
                            constraintsListForParam.addAll(
                                    constraintsList.filter { it.id == pair.second }
                                            .map { ConstraintApplication(it) })
                        }
                val tParam = TParam(
                        id,
                        alias,
                        name,
                        parent,
                        constraintsListForParam,
                        sensors
                )
                tParamList.add(tParam)
            }
            return tParamList
        }

        private suspend fun readLDUpParams(stmt: Statement): List<LDUpParam> {
            val ldUpParamList = mutableListOf<LDUpParam>()
            val resultset = stmt.executeQuery(MYSQL_GET_LDUPPARAMS)
            while (resultset.next()) {
                val id = resultset.getInt("id")
                val alias = resultset.getString("ld_alias")
                val name = resultset.getString("ld_name")
                val parent = resultset.getInt("ld_parent")
                val ldUpParam = LDUpParam(
                        id,
                        alias,
                        name,
                        parent
                )
                ldUpParamList.add(ldUpParam)
            }
            return ldUpParamList
        }

        private suspend fun readLDDownParams(stmt: Statement): List<LDDownParam> {
            val ldDownParamList = mutableListOf<LDDownParam>()
            val resultset = stmt.executeQuery(MYSQL_GET_LDDOWNPARAMS)
            while (resultset.next()) {
                val id = resultset.getInt("id")
                val alias = resultset.getString("ld_alias")
                val name = resultset.getString("ld_name")
                val parent = resultset.getInt("ld_parent")
                val ldDownParam = LDDownParam(
                        id,
                        alias,
                        name,
                        parent
                )
                ldDownParamList.add(ldDownParam)
            }
            return ldDownParamList
        }

        fun readMnemoschems(connection: Connection): List<Mnemoscheme> {
            val stmt = connection.createStatement()
            val resultset = stmt.executeQuery(MYSQL_GET_MNEMOSCHEMS)
            val mnemoschemsList = mutableListOf<Mnemoscheme>()
            while (resultset.next()) {
                val name = resultset.getString("name")
                val data = resultset.getString("data")
                val mnemoscheme = Mnemoscheme(name, data)
                mnemoschemsList.add(mnemoscheme)
            }
            return mnemoschemsList
        }

        val MYSQL_GET_MNEMOSCHEMS = "SELECT * FROM mnemoscheme ORDER BY name"
        //const val MYSQL_GET_TREE = "SELECT * FROM tmonitor.tree ORDER BY left_ix"
        val MYSQL_GET_LPARAMS = "SELECT * FROM lparam ORDER BY l_parent"
        val MYSQL_GET_TPARAMS = "SELECT * FROM tparam ORDER BY t_parent"
        val MYSQL_GET_LDUPPARAMS = "SELECT * FROM ldupparam ORDER BY ld_parent"
        val MYSQL_GET_LDDOWNPARAMS = "SELECT * FROM lddownparam ORDER BY ld_parent"
        val MYSQL_GET_CONSTRAINTS = "SELECT * FROM constraints ORDER BY id"
        val MYSQL_GET_LCONSTRAINTS_USAGE = "SELECT * FROM lcnstr ORDER BY prm_id"
        val MYSQL_GET_TCONSTRAINTS_USAGE = "SELECT * FROM tcnstr ORDER BY prm_id"
    }
}
