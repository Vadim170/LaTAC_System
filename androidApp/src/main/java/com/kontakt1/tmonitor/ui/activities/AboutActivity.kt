/*
 * Разработка мобильного приложения для системы АСКТ-01
 * Макаров В.Г. ст.гр.644 направление: 09.03.03
 * Жулева С.Ю. ст. преподаватель РГРТУ
 * MySQL Front
 * В этом файле описан класс активности со сведениями о программе.
 * Дата разработки: 16.04.2020
 */
package com.kontakt1.tmonitor.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.kontakt1.tmonitor.R
import kotlinx.android.synthetic.main.activity_about.*

/**
 * Класс активности для отображения сведений о приложении.
 * @author Makarov V.G.
 */
class AboutActivity : AppCompatActivity() {

    /**
     * Метод вызываемый после создания активности.
     * @param savedInstanceState сохраненное состояние
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about) // Задаем ресурс описывающий верстку объетков на активности.
        // Назначение обработчиков нажатий по кнопкам
        ivAboutLogo.setOnClickListener(::btnAboutLogoOnClick)
    }

    /**
     * Обработка нажатия на логотип компании
     * @param view элемент формы
     */
    private fun btnAboutLogoOnClick(view: View) {
        val browseIntent  = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.site_address)))
        startActivity(browseIntent)
    }

    /**
     * Обработка закрытия активности через кнопку "Назад"
     */
    override fun onBackPressed() {
        super.onBackPressed()
        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
    }
}