package com.bhaskarblur.webtalk.utils

import android.content.Context
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class webRTCHandler  {

    private lateinit var context : Context;
    private val gsonObject : Gson;
    private val eglBaseContext = EglBase.create().eglBaseContext;
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnectionInstance : PeerConnection? = null;
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
            .setUsername("83eebabf8b4cce9d5dbcb649")
            .setPassword("2D7JvfkOQtBdYW3R").createIceServer()
    )
    private lateinit var userEmail : String;
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints())}
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper: SurfaceTextureHelper?=null
    private lateinit var localSurfaceView : SurfaceViewRenderer;
    private lateinit var remoteSurfaceView: SurfaceViewRenderer;
    private var localStream : MediaStream? = null
    private var localTrackId = "";
    private var localStreamId = "";
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio","true"))
    }
    constructor(context: Context, gsonObject: Gson) {
        this.context = context
        this.gsonObject = gsonObject
    }

    init {
        initPeerConnectionFactory();
    }

    fun initializeWebRTCClient(
        userEmail : String, observer: PeerConnection.Observer
    ) {
        this.userEmail = userEmail;
        localTrackId = "${userEmail}_track"
        localStreamId = "${userEmail}_stram"
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
            init(eglBaseContext, null)

        }
    }

    fun initRemoteSurfaceView(localview : SurfaceViewRenderer ) {
        this.remoteSurfaceView = localSurfaceView;
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
        videoCapturer.startCapture(1080, 720, 30);

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
}