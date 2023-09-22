package com.bhaskarblur.webtalk.ui

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.databinding.ActivityReceiveCallScreenBinding
import com.bhaskarblur.webtalk.model.callModel
import com.bhaskarblur.webtalk.services.mainService
import com.bhaskarblur.webtalk.utils.callHandler
import com.bhaskarblur.webtalk.utils.callTypes
import com.bhaskarblur.webtalk.utils.firebaseHandler
import com.bhaskarblur.webtalk.utils.firebaseWebRTCHandler
import com.bhaskarblur.webtalk.utils.helper
import com.bhaskarblur.webtalk.utils.webRTCHandler
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class callScreen : AppCompatActivity(), callHandler {

    private lateinit var binding: ActivityReceiveCallScreenBinding;
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveCallScreenBinding.inflate(layoutInflater);
        setContentView(binding.root);
        val database = FirebaseDatabase.getInstance("https://webtalk-72d64-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("Users");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loadData();
        manageLogic();
    }

    private fun manageLogic() {

        binding.rejectCall.setOnClickListener{
            userRef.child(helper().cleanWord(email.toString())).child("status").setValue("Online");
            userRef.child(helper().cleanWord(receiverEmail)).child("status").setValue("Online");
            userRef.child(helper().cleanWord(email.toString())).child("latestEvents").removeValue()
            finish()
            overridePendingTransition(R.anim.fade_2, R.anim.fade);
        }

        binding.acceptCall.setOnClickListener {
            firebaseWebRTCHandler.setTarget(receiverEmail);
            firebaseWebRTCHandler.initWebRTCClient(email);
            firebaseWebRTCHandler.startCall(receiverEmail, "Offer")
            var intent = Intent(this@callScreen, videoCallActivity::class.java)
            intent.putExtra("userName", receiverName);
            intent.putExtra("userEmail",  receiverEmail);
            startActivity(intent);
            finish()

        }
    }

    private fun loadData() {
        email = prefs!!.getString("userEmail","")!!;

        receiverEmail = intent.getStringExtra("userEmail").toString();
        receiverName = intent.getStringExtra("userName").toString();
        var intent = intent;
        callType = intent.getStringExtra("callType").toString();

        if(callType.toString().lowercase().contains("video")) {
            binding.callType.setText("Video call");
        }
        else if(callType.toString().lowercase().contains("audio")) {
            binding.callType.setText("Audio call");
        }

        binding.userNameText.setText("From "+receiverName);


        email = prefs!!.getString("userEmail","")!!;
        userName = prefs!!.getString("userName","")!!;


        firebaseHandler = firebaseHandler(this, userRef, email, userName);
        firebaseWebRTCHandler = firebaseWebRTCHandler(this,userRef, email, userName
            , firebaseHandler);

        service = mainService(this, firebaseWebRTCHandler).getInstance()
        service.setCallHandler(this, firebaseHandler);
        rtcHandler = webRTCHandler(this, Gson(), firebaseHandler);
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        // no action here
    }

    override fun onDestroy() {
        super.onDestroy()
        userRef.child(helper().cleanWord(email)).child("status").setValue("Online");
    }

    override fun onCallReceived(message: callModel) {

    }

    override fun onInitOffer(message: callModel) {


    }

    override fun onCallAccepted(message: callModel) {

    }

    override fun onCallRejected(message: callModel) {

    }

    override fun onCallCut(message: callModel) {
    }

    override fun onUserAdded(message: callModel) {

        val candidate : IceCandidate? = try {
            Gson().fromJson(message.callData.toString(), IceCandidate::class.java);

        } catch (e:Exception) {
            null;
        }
        Toast.makeText(this, "user added "+candidate!!.sdp.toString() , Toast.LENGTH_SHORT).show()

        candidate?.let {
            rtcHandler.sendIceCandidate(receiverEmail, it);
        }
    }
}