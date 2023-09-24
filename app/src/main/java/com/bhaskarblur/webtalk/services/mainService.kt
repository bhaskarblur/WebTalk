package com.bhaskarblur.webtalk.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.utils.RTCAudioManager
import com.bhaskarblur.webtalk.utils.callHandler
import com.bhaskarblur.webtalk.utils.firebaseHandler
import com.bhaskarblur.webtalk.utils.firebaseWebRTCHandler
import com.bhaskarblur.webtalk.utils.webRTCHandler
import org.webrtc.SurfaceViewRenderer

class mainService : Service {


    private  var instance : mainService ?= null;
private var isRunning = false;
    private var userEmail : String? = null;
    private lateinit var callHandler : callHandler;
    private lateinit var notificationManager: NotificationManager;
    var localSurfaceView : SurfaceViewRenderer? = null;
    var remoteSurfaceView : SurfaceViewRenderer? = null;
    lateinit var webRTCHandler : webRTCHandler;
    lateinit var firebaseWebRTCHandler: firebaseWebRTCHandler;
    private lateinit var rtcAudioManager: RTCAudioManager
    private lateinit var audioManager : AudioManager
    private var isPreviousStateVideo = true;


    var screenPermissionIntent: Intent? = null;

    fun setscreenPermissionIntent(screenPermissionIntent : Intent) {
        this.screenPermissionIntent = screenPermissionIntent
    }
    fun getInstance(): mainService {
        if(instance == null) {
            instance = mainService();
        }
        return instance!!;
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent.let {
            when(it!!.action) {
                mainServiceActions.START_SERVICE.name -> {
                    handleStartService(intent);
                    Log.d("Service has been started", "true");
                }
                mainServiceActions.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION.name -> {
                    Log.d("Service has been started2", "true")
                    val notificationChannel = NotificationChannel(
                        "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(notificationChannel);
                    val notification = NotificationCompat.Builder(this, "channel1")
                        .setSmallIcon(R.mipmap.ic_launcher)
                    startForeground(1, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                    Log.d("Startedscreen_3", "yes")

                }
                else -> Unit
            }
        }
        return START_STICKY;

    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java);
        firebaseWebRTCHandler.initWebRTCClient(userEmail!!);

    }
    private fun handleStartService(intent: Intent?) {

        if(!isRunning) {
            isRunning = true;
            userEmail = intent!!.getStringExtra("userEmail")!!;
            Log.d("Service has been started2", "true2");
            startServiceWithNotification();

        }

    }

    private fun startServiceWithNotification() {

        val notificationChannel = NotificationChannel(
            "channel1", "foreground", NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel);
        val notification = NotificationCompat.Builder(this, "channel1")
            .setSmallIcon(R.mipmap.ic_launcher)
        startForeground(1, notification.build());
//        Log.d("Calling notification", "channel1");
    }


    fun setCallHandler(callHandler: callHandler?, handler: firebaseHandler) {
        this.callHandler = callHandler!!;

        handler.checkIncomingCall(this.callHandler);
    }
    private  var context: Context? = null;
    constructor(context : Context, webRTCHandler: firebaseWebRTCHandler) {
        this.firebaseWebRTCHandler = webRTCHandler;
        this.context = context;
    }

    fun setWebRtchandler(webRTCHandler: webRTCHandler) {
        this.webRTCHandler = webRTCHandler;
    }

    constructor()
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startServiceIntent(intent : Intent) {
        try {
            Log.d("serviceStarted", intent.getStringExtra("userEmail").toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context!!.startForegroundService(intent);
            } else {
                context!!.startService(intent);
            }
        }
        catch (e:Exception) {
            Log.d("err_", e.message.toString());
        }
    }

    fun startService(email: String, context: Context, action : mainServiceActions) {
        audioManager =  context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d("service",email);
        Thread {
            var intent = Intent(context, mainService::class.java);
            intent.putExtra("userEmail", email);
            intent.action = action.name
            Log.d("startedservice_4","yes")
            startServiceIntent(intent);

        }.start()

    }

    fun toggleAudioSpeakerMode(isSpeaker : Boolean) {
        if(!isSpeaker) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.isSpeakerphoneOn = true
//            rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
//            rtcAudioManager.selectAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        }
        else {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.isSpeakerphoneOn = false
//            rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
//            rtcAudioManager.selectAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun toggleScreenShare(shouldShow : Boolean, context_: Context) {

        webRTCHandler.setScreenCapturer(screenPermissionIntent!!)
        if(isPreviousStateVideo) {
            webRTCHandler.toggleVideo(true);

            webRTCHandler.setScreenCapturer(screenPermissionIntent!!)
            Log.d("Startedscreen_2", "yes")
            webRTCHandler.toggleScreenShare(true);
        }
        else {
            webRTCHandler.toggleVideo(false);
            webRTCHandler.toggleScreenShare(false);
        }
    }


}