package com.kontakt1.tmonitor.ui.activities.mainActivityFragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kontakt1.tmonitor.ApplicationData
import com.kontakt1.tmonitor.R
import com.kontakt1.tmonitor.ui.activities.MainActivity
import com.kontakt1.tmonitor.dataClasses.Silabus
import com.kontakt1.tmonitor.dataClasses.Silo
import com.kontakt1.tmonitor.ui.visualAdapters.ParamAdapter
import kotlinx.android.synthetic.main.fragment_silo.*
import java.lang.ref.WeakReference

/**
 * Класс фрагмента для отображения содержимого силоса (списка параметров). Этот фрагмент встраивается в MainActivity.
 * @author Makarov V.G.
 */
class SiloFragment : Fragment() {
    var onChangeFragment: ((MainActivity.FragmentEnum) -> Unit)? = null

    private var indicationsAllReadListenerUI = object : Silabus.EventListenerForInterfaceReadAllStates {
        override fun onPostExecuteReadAllStates(isNeedNotification: Boolean) {
            paramsList.invalidate()
            paramsList.invalidateViews()
        }
        override fun onPreExecuteReadAllStates() {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View? {
        return inflater.inflate(R.layout.fragment_silo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvSelectedSiloNameUpdate(ApplicationData.system.selectedSilo)
        // Если есть выбранный павраметр, то устанавливаем адаптер
        ApplicationData.system.selectedSilo?.let {
            val paramAdapter = context?.let { context ->
                ParamAdapter(
                    context,
                    R.layout.lv_item_param,
                    it.params,
                    myOnParamAdapterItemClick
                )
            } // Создаем адаптер
            paramsList.adapter = paramAdapter // устанавливаем адаптер
            //paramAdapter.notifyDataSetChanged()
        }
        // Назначаем обработчик для чтения состояний, чтобы иметь возможность обновить содержимое силкорпуса на экране
        ApplicationData.indicationsAllReadListenerUI = WeakReference(indicationsAllReadListenerUI)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ApplicationData.indicationsAllReadListenerUI = WeakReference<Silabus.EventListenerForInterfaceReadAllStates>(null)
    }

    private fun tvSelectedSiloNameUpdate(selectedSilo: Silo?) {
        tvSelectedSiloName?.text = selectedSilo?.name ?: "Данных о параметрах силоса не обнаружено"
    }

    /**
     * Обработчик нажатий на элементы списка (слушатель выбора в списке)
     */
    private val myOnParamAdapterItemClick = View.OnClickListener { selectedParam ->
        val silo = ApplicationData.system.selectedSilo
        if(silo == null) return@OnClickListener
        // Получаем выбранный пункт
        val selectedCompleteParam = silo.params.get(selectedParam.tag as Int)
        ApplicationData.system.selectedParam = when(selectedParam.id) {
            R.id.tvTParamLayout -> selectedCompleteParam.tParam
            R.id.tvLParamLayout -> selectedCompleteParam.lParam
            R.id.tvLDUpParamLayout -> selectedCompleteParam.lParam //ldUpParam
            R.id.tvLDDownParamLayout -> selectedCompleteParam.lParam //ldDownParam
            else -> null
        }
        ApplicationData.system.selectedCompleteParam = selectedCompleteParam
        onChangeFragment?.invoke(MainActivity.FragmentEnum.CHART) // Переходим на график
    }

    companion object {
        fun newInstance(): SiloFragment {
            return SiloFragment()
        }
    }
}