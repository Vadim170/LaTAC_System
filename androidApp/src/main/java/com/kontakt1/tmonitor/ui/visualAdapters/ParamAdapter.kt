package com.kontakt1.tmonitor.ui.visualAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.kontakt1.tmonitor.*
import com.kontakt1.tmonitor.dataClasses.CompleteParam
import com.kontakt1.tmonitor.dataClasses.params.interfaces.State

/**
 * Класс-адаптер для отрисовки комплексных параметров в ListView.
 * @author Makarov V.G.
 * @param context текущий контекст
 * @param layout id ListView
 * @param params список комплексных параметров
 * @param onClickListener обработчик нажатия на комплексный параметр. Обрабатывает нажатия на различные параметры
 */
class ParamAdapter(
    context: Context,
    private val layout: Int,
    private val params: List<CompleteParam>,
    private val onClickListener: View.OnClickListener
) : ArrayAdapter<CompleteParam>(context, layout, params) {

    private val inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = inflater.inflate(this.layout, parent, false)

        val ivLIcon = view.findViewById<ImageView>(R.id.ivLParamIcon)
        ivLIcon.setImageResource(R.drawable.ic_icon_lvl)
        val ivTIcon = view.findViewById<ImageView>(R.id.ivTParamIcon)
        ivTIcon.setImageResource(R.drawable.ic_icon_tmp)
        val ivLDUpIcon = view.findViewById<ImageView>(R.id.ivLDUpParamIcon)
        ivLDUpIcon.setImageResource(R.drawable.ic_icon_ldup)
        val ivLDDownIcon = view.findViewById<ImageView>(R.id.ivLDDownParamIcon)
        ivLDDownIcon.setImageResource(R.drawable.ic_icon_lddown)

        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val tvLName = view.findViewById<TextView>(R.id.tvLParamName)
        val tvTName = view.findViewById<TextView>(R.id.tvTParamName)
        val tvLDUpName = view.findViewById<TextView>(R.id.tvLDUpParamName)
        val tvLDDownName = view.findViewById<TextView>(R.id.tvLDDownParamName)

        val tvLConstraints = view.findViewById<TextView>(R.id.tvLParamConstraints)
        val tvTConstraints = view.findViewById<TextView>(R.id.tvTParamConstraints)

        val tvLParamLayout = view.findViewById<LinearLayout>(R.id.tvLParamLayout)
        tvLParamLayout.tag = position // Необходим чтобы определить номер параметра в onClick
        val tvTParamLayout = view.findViewById<LinearLayout>(R.id.tvTParamLayout)
        tvTParamLayout.tag = position // Необходим чтобы определить номер параметра в onClick
        val tvLDUpParamLayout = view.findViewById<LinearLayout>(R.id.tvLDUpParamLayout)
        tvLDUpParamLayout.tag = position // Необходим чтобы определить номер параметра в onClick
        val tvLDDownParamLayout = view.findViewById<LinearLayout>(R.id.tvLDDownParamLayout)
        tvLDDownParamLayout.tag = position // Необходим чтобы определить номер параметра в onClick

        val param = params[position]
        val lParam = param.lParam
        val tParam = param.tParam
        val ldUpParam = param.ldUpParam
        val ldDownParam = param.ldDownParam

        tvDescription.text = param.description
        if (lParam != null) {
            tvLParamLayout.visibility = VISIBLE
            val color = getColor(lParam.state)
            tvLParamLayout.setBackgroundColor(color)
            tvLParamLayout.setOnClickListener(onClickListener)
            tvLName.text = lParam.alias
            tvLConstraints.text = (lParam.constraintsList.size.toString() + " уставок")
        } else tvLParamLayout.visibility = INVISIBLE
        if (tParam != null) {
            tvTParamLayout.visibility = VISIBLE
            val color = getColor(tParam.state)
            tvTParamLayout.setBackgroundColor(color)
            tvTParamLayout.setOnClickListener(onClickListener)
            tvTName.text = tParam.alias
            tvTConstraints.text = (tParam.constraintsList.size.toString() + " уставок")
        } else tvTParamLayout.visibility = INVISIBLE
        if (ldUpParam != null) {
            tvLDUpParamLayout.visibility = VISIBLE
            val color = getColor(ldUpParam.state)
            tvLDUpParamLayout.setBackgroundColor(color)
            tvLDUpParamLayout.setOnClickListener(onClickListener)
            tvLDUpName.text = ldUpParam.alias
        } else tvLDUpParamLayout.visibility = INVISIBLE
        if (ldDownParam != null) {
            tvLDDownParamLayout.visibility = VISIBLE
            val color = getColor(ldDownParam.state)
            tvLDDownParamLayout.setBackgroundColor(color)
            tvLDDownParamLayout.setOnClickListener(onClickListener)
            tvLDDownName.text = ldDownParam.alias
        } else tvLDDownParamLayout.visibility = INVISIBLE
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