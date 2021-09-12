package com.example.aurora

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MakeVisibleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_visible)
        initListeners()
        makeDeviceVisible()
    }

    private fun initListeners(){
        val backButton: Button = findViewById(R.id.make_visible_back_button)
        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun makeDeviceVisible(){

    }
}