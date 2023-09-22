package com.bhaskarblur.webtalk.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.bhaskarblur.webtalk.model.callModel
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon.RendererEvents
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack

class webRTCHandler  {

    private lateinit var context : Context;
    private val gsonObject : Gson;
    private val eglBaseContext = EglBase.create().eglBaseContext;
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnectionInstance : PeerConnection? = null;
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:turn.anyfirewall.com:443?transport=tcp")
            .setUsername("webrtc")
            .setPassword("webrtc").createIceServer()
    )
    private lateinit var userEmail : String;
    private lateinit var userName : String;
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private lateinit var videoCapturer : CameraVideoCapturer;
    private var surfaceTextureHelper: SurfaceTextureHelper?=null
    private lateinit var localSurfaceView : SurfaceViewRenderer;
    private lateinit var remoteSurfaceView: SurfaceViewRenderer;
    private var localStream : MediaStream? = null
    private var localTrackId = "";
    private var localStreamId = "";
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var firebaseHandler : firebaseHandler ;

    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"))
    }
    constructor(context: Context, gsonObject: Gson, firebaseHandler: firebaseHandler) {
        this.context = context
        this.gsonObject = gsonObject
        this.firebaseHandler = firebaseHandler
        videoCapturer = getVideoCapturer(context)
        initPeerConnectionFactory();
    }


    fun initializeWebRTCClient(
        userEmail : String, observer: PeerConnection.Observer, userName: String
    ) {
        this.userName = userName;
        this.userEmail = userEmail;
        localTrackId = "${userEmail}_track"
        localStreamId = "${userEmail}_stream"
        peerConnectionInstance = createPeerConnection(observer)
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)

    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options);

    }

    private fun initSurfaceView(view : SurfaceViewRenderer) {
        view.run {
            setMirror(false);
            setEnableHardwareScaler(true)
            init(eglBaseContext, object : RendererEvents{
                override fun onFirstFrameRendered() {
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                }

            })

        }
    }

    fun initRemoteSurfaceView(localview : SurfaceViewRenderer ) {
        this.remoteSurfaceView = localview;
        initSurfaceView(localview)
    }
    fun initLocalSurfaceView(localview : SurfaceViewRenderer, isVideoCall : Boolean) {
        this.localSurfaceView = localview;
        initSurfaceView(localview)
        startLocalStraming(localview, isVideoCall)
    }

    private fun startLocalStraming(localview: SurfaceViewRenderer, videoCall: Boolean) {
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId);
        if(videoCall) {
            startCapturingCamera(localview)
        }

        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio", localAudioSource)
        localStream?.addTrack(localAudioTrack);
        peerConnectionInstance?.addStream(localStream);
    }

    private fun startCapturingCamera(localview: SurfaceViewRenderer) {
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )

        videoCapturer.initialize(
            surfaceTextureHelper, context, localVideoSource.capturerObserver

        )
        videoCapturer.startCapture(1080, 720, 50);

        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId+"_video"
        , localVideoSource);

        localVideoTrack?.addSink(localview)
        localStream?.addTrack(localVideoTrack);
    }

    private fun getVideoCapturer(context : Context) : CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)

            }?.let {
                createCapturer(it, null)
            }?: throw IllegalStateException()
        }

    private fun stopCapturingCamer(localview: SurfaceViewRenderer) {
        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage();
        localStream?.removeTrack(localVideoTrack);
        localVideoTrack?.dispose();
    }

    private fun createPeerConnectionFactory() : PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(   DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext,
                true, true)).setOptions(
                    PeerConnectionFactory.Options().apply {
                        disableNetworkMonitor = false
                        disableEncryption = false;
                    }
                ).createPeerConnectionFactory();
    }

    fun call(target: String, callType: String): Boolean {
        var success = false;
        Log.d("isCalling", "yes");
        peerConnectionInstance!!.createOffer(object: SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d("statusCall_2", "createdCallSuccess")
                peerConnectionInstance?.setLocalDescription(
                    object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {
                            Log.d("statusCall", "createdCallSuccess")
                        }

                        override fun onSetSuccess() {
                            Log.d("statusCall", "setCallSuccessthis")
                            success =
                                true;
                            Log.d("sdpnew", desc?.description.toString())
                            firebaseHandler.callUser(
                                callModel(userEmail, userName, target, desc?.description
                                    , callType)
                            )
                            answer(target);
                        }

                        override fun onCreateFailure(p0: String?) {
                            Log.d("statusCall", "createdCallFailed")
                        }

                        override fun onSetFailure(p0: String?) {
                            Log.d("statusCall", "createdSetFailure")
                        }
                    }, desc)
            }

            override fun onSetSuccess() {
                Log.d("statusCall_2", "createdSetSuccess")
            }

            override fun onCreateFailure(p0: String?) {
                Log.d("statusCall_2", "createdCallFailure")
            }

            override fun onSetFailure(p0: String?) {
                Log.d("statusCall_2", "createdSetFailure")
            }
        }, mediaConstraint)
        Log.d("callResult",success.toString());
        return success;
    }

    fun answer(target:String): Boolean {
        var success = false;
        peerConnectionInstance!!.createAnswer(object  : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnectionInstance?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {
                        TODO("Not yet implemented")
                    }

                    override fun onSetSuccess() {
                        firebaseHandler.answerUser(  callModel(
                            userEmail, userName, target, desc?. description
                            , callTypes.Answer.name));
                        success=true;
                    }

                    override fun onCreateFailure(p0: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onSetFailure(p0: String?) {
                        TODO("Not yet implemented")
                    }

                }, desc)
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                Log.d("statusFailed2", p0.toString());
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }
        }, mediaConstraint)
        Log.d("acceptresult",success.toString());
//        Toast.makeText(context, "Accepted: "+success.toString(),Toast.LENGTH_SHORT
//        ).show();
        return success;
    }

    fun onRemoteSessionReceived(sdp : SessionDescription)
    {
        peerConnectionInstance?.setRemoteDescription(sdpObserver(),sdp);

    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        Log.d("icepeer",iceCandidate.sdp.toString() );
        peerConnectionInstance?.addIceCandidate(iceCandidate);
    }

    fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {

        addIceCandidateToPeer(iceCandidate);
        firebaseHandler.answerUser(  callModel(
            userEmail, userName, target, gsonObject.toJson(iceCandidate)
            , callTypes.ICECandidate.name));
    }

    fun closeConnection() {
        try {
            videoCapturer?.dispose();
            localStream?.dispose();
            peerConnectionInstance?.close();
        } catch (e: Exception){

        }
    }

    fun switchCamera() {
        videoCapturer.switchCamera(null);
    }
    fun toggleAudio(shouldBeMuted : Boolean) {
        if(shouldBeMuted) {
            localStream?.removeTrack(localAudioTrack);
        }
        else {
            localStream?.addTrack(localAudioTrack);
        }

    }

    fun toggleVideo(shouldHide : Boolean) {
        try {
            if(shouldHide) {
                stopCapturingCamer(localSurfaceView)
            }
            else {
                startCapturingCamera(localSurfaceView)
            }
        } catch (e: Exception){

        }
    }

}