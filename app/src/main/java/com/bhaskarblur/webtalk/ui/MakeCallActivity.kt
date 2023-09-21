package com.bhaskarblur.webtalk.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.databinding.ActivityMakeCallBinding
import com.bhaskarblur.webtalk.databinding.ActivityReceiveCallScreenBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class makeCall : AppCompatActivity() {

    private lateinit var binding: ActivityMakeCallBinding;
    private lateinit var userRef : DatabaseReference;
    private lateinit var receiverEmail :String;
    private lateinit var receiverName :String;
    private lateinit var callType :String;
    private lateinit var email :String;
    private var prefs: SharedPreferences? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeCallBinding.inflate(layoutInflater);
        setContentView(binding.root);
        val database = FirebaseDatabase.getInstance("https://webtalk-72d64-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("Users");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadData();
    }

    private fun loadData() {
        email = prefs!!.getString("userEmail","")!!;

        var intent = intent;
        receiverEmail = intent.getStringExtra("userEmail").toString();
        receiverName = intent.getStringExtra("userName").toString();
        callType = intent.getStringExtra("callType").toString();

        if(callType.toString().lowercase().contains("video")) {
            binding.callType.setText("Video");
        }
        else if(callType.toString().lowercase().contains("audio")) {
            binding.callType.setText("Audiol");
        }

        binding.userNameText.setText("Calling "+receiverName);
    }
}