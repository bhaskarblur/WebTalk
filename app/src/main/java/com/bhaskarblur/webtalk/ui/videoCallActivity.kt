package com.bhaskarblur.webtalk.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import com.bhaskarblur.webtalk.databinding.ActivityMakeCallBinding
import com.bhaskarblur.webtalk.databinding.ActivityVideoCallBinding
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

class videoCallActivity : AppCompatActivity(), callHandler {
    private lateinit var binding: ActivityVideoCallBinding;
    private lateinit var userRef : DatabaseReference;
    private lateinit var receiverEmail :String;
    private lateinit var receiverName :String;
    private lateinit var email :String;
    private var prefs: SharedPreferences? = null;
    lateinit var service: mainService;
    lateinit var userName : String;
    lateinit var firebaseWebRTCHandler: firebaseWebRTCHandler;
    lateinit var firebaseHandler: firebaseHandler;
    lateinit var rtcHandler: webRTCHandler;
    private var gson  = Gson();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater);
        setContentView(binding.root);
        val database = FirebaseDatabase.getInstance("https://webtalk-72d64-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("Users");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        loadData();
        manageLogic();
    }

    private fun manageLogic() {


        binding.cutCall.setOnClickListener {
            firebaseWebRTCHandler.endCall();
            finish();
        }

    }

    private fun loadData() {
        email = prefs!!.getString("userEmail","")!!;
        userName = prefs!!.getString("userName","")!!;

        var intent = intent;
        receiverEmail = intent.getStringExtra("userEmail").toString();
        receiverName = intent.getStringExtra("userName").toString();
        firebaseHandler = firebaseHandler(this, userRef, email, userName);
        firebaseWebRTCHandler = firebaseWebRTCHandler(this,userRef, email, userName
            , firebaseHandler);

        firebaseWebRTCHandler.setTarget(receiverEmail);
        firebaseWebRTCHandler.initWebRTCClient(email);


        service = mainService(this, firebaseWebRTCHandler).getInstance()
        service.setCallHandler(this, firebaseHandler);

        rtcHandler = webRTCHandler(this, Gson(), firebaseHandler);
            service.localSurfaceView = binding.userCamera;
        service.remoteSurfaceView = binding.otherUserCamera;
        firebaseWebRTCHandler.initLocalSurfaceView(binding.userCamera, true);
        firebaseWebRTCHandler.initRemoteSurfaceView(binding.otherUserCamera);


    }

    override fun onBackPressed() {
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
        firebaseHandler.changeMyStatus("Online");
        finish();

    }

    override fun onUserAdded(message: callModel) {
//        Toast.makeText(this, "user added!", Toast.LENGTH_SHORT).show()
        val candidate : IceCandidate? = try {
            gson.fromJson(message.callData.toString(),IceCandidate::class.java);

        } catch (e:Exception) {
          null;
        }

        candidate?.let {
            rtcHandler.sendIceCandidate(receiverEmail, it);
        }
    }
}