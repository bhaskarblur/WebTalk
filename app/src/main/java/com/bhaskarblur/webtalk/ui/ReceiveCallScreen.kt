package com.bhaskarblur.webtalk.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.databinding.ActivityReceiveCallScreenBinding
import com.bhaskarblur.webtalk.utils.helper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class callScreen : AppCompatActivity() {

    private lateinit var binding: ActivityReceiveCallScreenBinding;
    private lateinit var userRef : DatabaseReference;
    private lateinit var senderEmail :String;
    private lateinit var senderName :String;
    private lateinit var callType :String;
    private lateinit var email :String;
    private var prefs: SharedPreferences? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveCallScreenBinding.inflate(layoutInflater);
        setContentView(binding.root);
        val database = FirebaseDatabase.getInstance("https://webtalk-72d64-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("Users");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadData();
    }

    private fun loadData() {
        email = prefs!!.getString("userEmail","")!!;

        var intent = intent;
        senderEmail = intent.getStringExtra("userEmail").toString();
        senderName = intent.getStringExtra("userName").toString();
        callType = intent.getStringExtra("callType").toString();

        if(callType.toString().lowercase().contains("video")) {
            binding.callType.setText("Video call");
        }
        else if(callType.toString().lowercase().contains("audio")) {
            binding.callType.setText("Audio call");
        }

        binding.userNameText.setText("From "+senderName);

        binding.rejectCall.setOnClickListener{
            userRef.child(helper().cleanWord(email.toString())).child("status").setValue("Online");
            userRef.child(helper().cleanWord(senderEmail)).child("status").setValue("Online");
            userRef.child(helper().cleanWord(email.toString())).child("latestEvents").removeValue()
            finish()
            overridePendingTransition(R.anim.fade_2, R.anim.fade);
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        // no action here
    }

    override fun onDestroy() {
        super.onDestroy()
        userRef.child(helper().cleanWord(email.toString())).child("status").setValue("Online");
        userRef.child(helper().cleanWord(senderEmail)).child("status").setValue("Online");
    }
}