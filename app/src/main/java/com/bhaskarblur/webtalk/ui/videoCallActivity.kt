package com.bhaskarblur.webtalk.ui

import android.R.attr.width
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.databinding.ActivityVideoCallBinding
import com.bhaskarblur.webtalk.model.callModel
import com.bhaskarblur.webtalk.model.isValid
import com.bhaskarblur.webtalk.services.mainService
import com.bhaskarblur.webtalk.utils.callHandler
import com.bhaskarblur.webtalk.utils.firebaseHandler
import com.bhaskarblur.webtalk.utils.firebaseWebRTCHandler
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
    private var gson  = Gson();
    private var offerMade = false;
    private var accepted = false;
    private var videoHide = false;
    private var micMute = false;
    private var speaker = false;
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
        firebaseWebRTCHandler.initLocalSurfaceView(binding.userCamera, true);
        firebaseWebRTCHandler.initRemoteSurfaceView(binding.otherUserCamera);
        service = mainService(this, firebaseWebRTCHandler).getInstance()
        service.setCallHandler(this, firebaseHandler);
        ;
            service.localSurfaceView = binding.userCamera;
        service.remoteSurfaceView = binding.otherUserCamera;

        binding.swapbtn.setOnClickListener{
            firebaseWebRTCHandler.switchCamera();
        }

        binding.videobtn.setOnClickListener {
            if(!videoHide) {
                binding.videobtn.setImageResource(R.drawable.videooff);
                firebaseWebRTCHandler.toggleVideo(true);
                videoHide = true;
            }
            else {
                binding.videobtn.setImageResource(R.drawable.videoon);
                firebaseWebRTCHandler.toggleVideo(false);
                videoHide=false;
            }
        }

        binding.micbtn.setOnClickListener {
            if(!micMute) {
                binding.micbtn.setImageResource(R.drawable.microphoneoff);
                firebaseWebRTCHandler.toggleAudio(true);
                micMute = true;
            }
            else {
                binding.micbtn.setImageResource(R.drawable.microphone);
                firebaseWebRTCHandler.toggleAudio(false);
                micMute=false;
            }
        }

        binding.speakericon.setOnClickListener {
            if(!speaker) {
                binding.speakericon.setImageResource(R.drawable.speakericon);
                speaker = true;
            }
            else {
                speaker = false;
                binding.speakericon.setImageResource(R.drawable.mobileicon);
            }
        }
    }

    override fun onBackPressed() {
    }

    override fun onCallReceived(message: callModel) {

    }

    override fun onInitOffer(message: callModel) {
        if(message.isValid() && !offerMade) {
            offerMade = true
//            Toast.makeText(this, "Offered " + message.senderEmail, Toast.LENGTH_SHORT).show()
//            Log.d("offered", message.senderEmail.toString());
            firebaseWebRTCHandler.webRTCHandler.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.OFFER,
                    message.callData.toString()
                ),
                message.senderEmail!!
            )

            firebaseWebRTCHandler.acceptCall(message.senderEmail!!);
        }
    }

    override fun onCallAccepted(message: callModel) {

        firebaseWebRTCHandler.startCall(receiverEmail, "Offer")
    }

    override fun onCallRejected(message: callModel) {
    }

    override fun onCallCut(message: callModel) {
        Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show()
        firebaseWebRTCHandler.webRTCHandler.closeConnection();
        firebaseHandler.changeMyStatus("Online");
        finish();

    }

    override fun onUserAdded(message: callModel) {
        Log.d("usedAdded", message.senderEmail.toString());
        val candidate : IceCandidate? = try {
            gson.fromJson(message.callData.toString(),IceCandidate::class.java);

        } catch (e:Exception) {
          null;
        }

        candidate?.let {
            firebaseWebRTCHandler.webRTCHandler.addIceCandidateToPeer(candidate);
//            firebaseWebRTCHandler.webRTCHandler.sendIceCandidate(receiverEmail, it);
        }
    }

    override fun finalCallAccepted(message: callModel) {
        if(message.isValid() && !accepted) {
            accepted = true;
            Log.d("acceptedFinal", message.senderEmail.toString());
            firebaseWebRTCHandler.webRTCHandler.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.callData.toString()
                ),
                message.senderEmail!!
            )

//            Toast.makeText(this, "Starting Call", Toast.LENGTH_SHORT).show()
        }
    }
}