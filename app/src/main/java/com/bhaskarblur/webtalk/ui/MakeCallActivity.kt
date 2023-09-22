package com.bhaskarblur.webtalk.ui

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.databinding.ActivityMakeCallBinding
import com.bhaskarblur.webtalk.databinding.ActivityReceiveCallScreenBinding
import com.bhaskarblur.webtalk.model.callModel
import com.bhaskarblur.webtalk.services.mainService
import com.bhaskarblur.webtalk.utils.callHandler
import com.bhaskarblur.webtalk.utils.firebaseHandler
import com.bhaskarblur.webtalk.utils.firebaseWebRTCHandler
import com.bhaskarblur.webtalk.utils.webRTCHandler
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class makeCall : AppCompatActivity(), callHandler {

    private lateinit var binding: ActivityMakeCallBinding;
    private lateinit var userRef : DatabaseReference;
    private lateinit var receiverEmail :String;
    private lateinit var receiverName :String;
    private lateinit var callType :String;
    private lateinit var email :String;
    private lateinit var userName :String;
    private var prefs: SharedPreferences? = null;
    lateinit var service: mainService;
    lateinit var firebaseWebRTCHandler: firebaseWebRTCHandler;
    lateinit var firebaseHandler: firebaseHandler;
    lateinit var rtcHandler: webRTCHandler;
    private var videoOpened = false;
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
        userName = prefs!!.getString("userName","")!!;
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


        firebaseHandler = firebaseHandler(this, userRef, email, userName);
        firebaseWebRTCHandler = firebaseWebRTCHandler(this,userRef, email, userName
            , firebaseHandler);

        service = mainService(this, firebaseWebRTCHandler).getInstance()
        service.setCallHandler(this, firebaseHandler);
        rtcHandler = webRTCHandler(this, Gson(), firebaseHandler);
        firebaseWebRTCHandler.setTarget(receiverEmail);
        firebaseWebRTCHandler.initWebRTCClient(email);
        firebaseWebRTCHandler.startCall(receiverEmail, "Offer");
        firebaseWebRTCHandler.acceptCall(receiverEmail);

    }

    override fun onCallReceived(message: callModel) {
    }

    override fun onInitOffer(message: callModel) {
    }

    override fun onCallAccepted(message: callModel) {
        rtcHandler.onRemoteSessionReceived(
            SessionDescription(SessionDescription.Type.ANSWER,
                message.callData.toString())
        )
        finish()
    }

    override fun onCallRejected(message: callModel) {

        firebaseHandler.changeMyStatus("Online");
        finish();
    }

    override fun onCallCut(message: callModel) {
    }

    override fun onUserAdded(message: callModel) {
        Log.d("1stmail", email);
        Log.d("2ndmail", message.targetEmail!!);
        if(!message.targetEmail!!.equals(receiverEmail) && !videoOpened) {
            videoOpened = true
            var intent = Intent(this@makeCall, videoCallActivity::class.java)
            intent.putExtra("userName", receiverName);
            intent.putExtra("userEmail", receiverEmail);
            startActivity(intent);
            finish()
        }

    }
}