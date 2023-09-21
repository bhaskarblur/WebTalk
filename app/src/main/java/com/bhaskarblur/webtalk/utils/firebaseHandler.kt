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
import java.lang.Exception
import java.util.concurrent.Callable

class firebaseHandler {

    private lateinit var context: Context;
    private lateinit var currentUser : String;
    private lateinit var dbRef: DatabaseReference;
    private lateinit var gsonObject : Gson;
    private var acceptCall = true;

    constructor(context: Context, dbRef: DatabaseReference,currentUser : String) {
        this.context = context
        this.dbRef = dbRef
        gsonObject = Gson();
        this.currentUser = currentUser;
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
                                        respond.onCallReceived(event);
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
        dbRef.child(helper().cleanWord(message.targetEmail!!))
            .child("latestEvents").setValue(serMessage)
            .addOnSuccessListener({
                success = true;

                // adding user to call so that no one else can call!
                dbRef.child(helper().cleanWord(message.senderEmail.toString())).child("status").setValue("OnCall");
                dbRef.child(helper().cleanWord(message.targetEmail.toString())).child("status").setValue("OnCall");
            })
            .addOnFailureListener({
                success = false;
            })

        return success;

    }
}
