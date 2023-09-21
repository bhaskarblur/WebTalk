package com.bhaskarblur.webtalk.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.bhaskarblur.webtalk.databinding.ActivityMakeCallBinding
import com.bhaskarblur.webtalk.databinding.ActivityVideoCallBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class videoCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoCallBinding;
    private lateinit var userRef : DatabaseReference;
    private lateinit var receiverEmail :String;
    private lateinit var receiverName :String;
    private lateinit var email :String;
    private var prefs: SharedPreferences? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater);
        setContentView(binding.root);
        val database = FirebaseDatabase.getInstance("https://webtalk-72d64-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("Users");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadData();
    }

    private fun loadData() {

    }
}