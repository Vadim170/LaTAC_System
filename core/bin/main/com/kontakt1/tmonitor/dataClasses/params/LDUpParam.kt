package com.kontakt1.tmonitor.dataClasses.params

import com.kontakt1.tmonitor.dataClasses.params.interfaces.DiscreteParams

/**
 * Класс параметра верхнего дискретного датчика.
 * Имеет suspend метод для загрузки последних паказаний. Имеет поля из базы данных, поле с последним показанием
 * из базы данных и состояние с учетом включенных уставок.
 * @author Makarov V.G.
 */
class LDUpParam(
    id:Int,
    alias:String,
    name:String,
    parent:Int
) : DiscreteParams(id, alias, name, parent) {
    override val nameColumnStateInDB: String = "ld_up"
}