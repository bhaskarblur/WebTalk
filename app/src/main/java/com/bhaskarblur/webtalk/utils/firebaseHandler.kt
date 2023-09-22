package com.bhaskarblur.webtalk.utils

import android.content.Context
import android.renderscript.Sampler.Value
import android.util.Log
import android.widget.Toast
import com.bhaskarblur.webtalk.model.callModel
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import java.util.concurrent.Callable


class firebaseHandler {

    private lateinit var context: Context;
    private lateinit var currentUser : String;
    private lateinit var currentUserName : String;
    private lateinit var dbRef: DatabaseReference;
    private lateinit var gsonObject : Gson;
    private var acceptCall = true;
     var target ="";


    constructor(context: Context, dbRef: DatabaseReference,currentUser : String, currentUserName:String) {
        this.context = context
        this.dbRef = dbRef
        gsonObject = Gson();
        this.currentUser = currentUser;
        this.currentUserName = currentUserName;
    }
    constructor()

    fun setAcceptCall(acceptCall : Boolean) {
        this.acceptCall = acceptCall;
    }
    public fun checkIncomingCall(respond : callHandler) {

        dbRef.child(helper().cleanWord(currentUser))
                    .child("latestEvents").addValueEventListener(
                        object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (acceptCall) {
                                    try {
                                        var event =
                                            gsonObject.fromJson(
                                                snapshot.value.toString(),
                                                callModel::class.java
                                            )

                                        when(event.callType) {

                                            callTypes.Offer.name -> {
                                                respond.onInitOffer(event);
                                            }

                                                callTypes.Answer.name -> {
                                                    respond.onCallAccepted(event);
                                            }

                                            callTypes.EndCall.name -> {
                                                respond.onCallCut(event);
                                            }

                                            callTypes.ICECandidate.name -> {
                                                respond.onUserAdded(event);

                                            }

                                            callTypes.StartedAudioCall.name -> {
                                                respond.onCallReceived(event);
//                                                respond.onInitOffer(event);

                                            }

                                            callTypes.StartedVideoCall.name -> {
                                                respond.onCallReceived(event);
//                                                respond.onInitOffer(event);

                                            }
                                        }


                                    } catch (e: Exception) {
                                        Log.d("err", e.message.toString());
                                    }
                                }

                            }


                            override fun onCancelled(error: DatabaseError) {
                                Log.d("Error receiving updates", error.message.toString());
                            }
                        }
                    )


    }

    public fun callUser(message : callModel): Boolean {
        var success = false;
        val serMessage = gsonObject.toJson(message);
        Log.d("fbCall", serMessage.toString());
        dbRef.child(helper().cleanWord(message.targetEmail!!))
            .child("latestEvents").setValue(serMessage)
            .addOnSuccessListener({
                success = true;
                target = message.targetEmail!!;
                // adding user to call so that no one else can call!
                changeMyStatus("OnCall")
            })
            .addOnFailureListener({
                success = false;
            })

        Log.d("isFirebaseCall", success.toString());
        return success;

    }


    fun changeMyStatus(status: String) {
        dbRef.child(helper().cleanWord(currentUser)).child("status").setValue(status);
    }
    public fun answerUser(message : callModel): Boolean {
        var success = false;
        val serMessage = gsonObject.toJson(message);
        dbRef.child(helper().cleanWord(message.targetEmail!!))
            .child("latestEvents").setValue(serMessage)
            .addOnSuccessListener({
                success = true;

                changeMyStatus("OnCall")

            })
            .addOnFailureListener({
                success = false;
            })

        return success;

    }


}
class firebaseWebRTCHandler {

    private lateinit var context: Context;
    private lateinit var currentUser : String;
    private lateinit var currentUserName : String;
    private lateinit var dbRef: DatabaseReference;
    private lateinit var gsonObject : Gson;
    private var acceptCall = true;
    private lateinit var webRTCHandler : webRTCHandler;
    private lateinit var firebaseHandler: firebaseHandler;
    private lateinit var remoteView : SurfaceViewRenderer;
    private lateinit var target: String;
    constructor(context: Context, dbRef: DatabaseReference,currentUser : String, currentUserName:String, firebaseHandler: firebaseHandler) {
        this.context = context
        this.dbRef = dbRef
        gsonObject = Gson();
        this.currentUser = currentUser;
        this.currentUserName = currentUserName;
        this.firebaseHandler = firebaseHandler;
    }

    fun setTarget(target: String) {
        firebaseHandler.target = target;
        this.target = target;
    }
    fun initWebRTCClient(email: String) {
        webRTCHandler = webRTCHandler(context, gsonObject, firebaseHandler);
        webRTCHandler.initializeWebRTCClient(email, object : myPeerObserver() {
            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                Log.d("adstream", p0.toString());
                Toast.makeText(context,
                    p0!!.id.toString(), Toast.LENGTH_SHORT).show()
                try {
                    p0!!.videoTracks?.get(0)?.addSink(remoteView);
                    Toast.makeText(context,
                        p0.id.toString(), Toast.LENGTH_SHORT).show()

                }
                catch (e : Exception) {
                    Log.d("errorAddStream", e.message.toString());
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0.let {
                   Log.d("ice__", it!!.sdp.toString());
                    webRTCHandler.sendIceCandidate(firebaseHandler.target, it!!);
                }

            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
               Log.d("newState", newState.toString());
                if (newState!!.equals(PeerConnection.PeerConnectionState.CONNECTED)) {
                    dbRef.child(helper().cleanWord(target)).child("status").setValue("OnCall");
                    dbRef.child(helper().cleanWord(currentUser)).child("latestEvents").child("callType").setValue("EndCall")
                }
                else if(newState.equals(PeerConnection.PeerConnectionState.CLOSED)) {
                    dbRef.child(helper().cleanWord(target)).child("status").setValue("OnCall");
                    dbRef.child(helper().cleanWord(currentUser)).child("latestEvents").setValue("EndCall")
                }
            }
        }, currentUserName);

    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall : Boolean) {
        webRTCHandler.initLocalSurfaceView(view, true);
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCHandler.initRemoteSurfaceView(view);
        this.remoteView = view;
    }

    fun startCall(target:String, callType: String?) {
        webRTCHandler.call(target, callType!!);
        firebaseHandler.target = target;
    }

    fun acceptCall(target:String) {
//        webRTCHandler.answer(target);
    }

    fun pickCall(target:String) {
        firebaseHandler.answerUser(  callModel(
            currentUser, currentUser, target,
            callTypes.ICECandidate.name));
    }
    fun endCall() {
        webRTCHandler.closeConnection();
        firebaseHandler.changeMyStatus("Online");
        dbRef.child(helper().cleanWord(firebaseHandler.target))
            .child("latestEvents").setValue(
                gsonObject.toJson(callModel(
                currentUser, currentUserName, firebaseHandler.target, null,
                callTypes.EndCall.name
            )));

        dbRef.child(helper().cleanWord(firebaseHandler.target))
            .child("latestEvents").setValue(null);

//        dbRef.child(helper().cleanWord(currentUser))
//            .child("latestEvents").setValue(null);
    }

    fun toggleVideo(shouldHide : Boolean) {
        webRTCHandler.toggleVideo(shouldHide);
    }
    fun toggleAudio(shouldBeMuted : Boolean) {
        webRTCHandler.toggleVideo(shouldBeMuted);
    }

    fun switchCamera() {
        webRTCHandler.switchCamera();
    }


}

