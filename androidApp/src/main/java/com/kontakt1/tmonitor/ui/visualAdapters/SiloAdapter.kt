package com.kontakt1.tmonitor.ui.visualAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.dataClasses.Silo
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State

/**
 * Класс-адаптер для отрисовки силосов в GridView.
 * @author Makarov V.G.
 * @param context текущий контекст
 * @param layout id GridView
 * @param silos список силосов
 */
class SiloAdapter(context: Context, private val layout: Int, private val silos: List<Silo>)
        : ArrayAdapter<Silo>(context, layout, silos)  {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = inflater.inflate(this.layout, parent, false)

        val tvName = view.findViewById<TextView>(R.id.tvSelectedSiloName)
        val tvDir = view.findViewById<TextView>(R.id.tvSiloDir)
        val container = view.findViewById<ConstraintLayout>(R.id.containerSiloLayout)

        val silo = silos[position]
        val color = getColor(silo.state)
        container.setBackgroundColor(color)
        tvName.text = silo.name
        tvDir.text = silo.dir
        return view
    }

    private fun getColor(state: State): Int {
        return when (state) {
            State.OK -> ContextCompat.getColor(context, R.color.stateOk)
            State.ALARM -> ContextCompat.getColor(context, R.color.stateAlarm)
            State.OLD -> ContextCompat.getColor(context, R.color.stateOld)
        }
    }
}
