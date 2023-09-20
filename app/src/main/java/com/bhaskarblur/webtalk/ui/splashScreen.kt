package com.bhaskarblur.webtalk.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bhaskarblur.webtalk.R

class splashScreen : AppCompatActivity() {
    lateinit var prefs: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)


        //      getActionBar().hide();
        Handler().postDelayed({
            prefs = PreferenceManager.getDefaultSharedPreferences(this@splashScreen)
            Log.d("logged", prefs.getBoolean("loggedStatus", false).toString())
            if (prefs.getBoolean("loggedStatus", false)) {
                startActivity(Intent(this@splashScreen, mainActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this@splashScreen, loginScreen::class.java))
                finish()
            }
        }, 1500)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
    }
}