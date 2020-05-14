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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        // Назначение обработчиков нажатий по кнопкам
        ivAboutLogo.setOnClickListener(::btnAboutLogoOnClick)
    }

    /**
     * Обработка нажатия на логотип компании
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