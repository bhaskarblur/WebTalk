package com.bhaskarblur.webtalk.ui

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.databinding.ActivityMainBinding
import com.bhaskarblur.webtalk.model.userPublicModel
import com.bhaskarblur.webtalk.adapter.usersAdapter
import com.bhaskarblur.webtalk.utils.helper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class mainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding;
    private lateinit var userRef : DatabaseReference;
    private var userList : ArrayList<userPublicModel> = ArrayList();
    private lateinit var userAdapter : usersAdapter;
    private var prefs: SharedPreferences? = null;
    var email : String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(binding.root);
        val database = FirebaseDatabase.getInstance("https://webtalk-72d64-default-rtdb.asia-southeast1.firebasedatabase.app/")
        userRef = database.getReference("Users");
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        manageLogic();
        loadData();
    }

    private fun loadData() {
        var username = prefs!!.getString("userName","");
        email = prefs!!.getString("userEmail","");
        binding.userName.setText("Hello "+username +" !");

        //update status of user!
        userRef.child(helper().cleanWord(email.toString())).child("status").setValue("Online");

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("users", snapshot.value.toString());
                userList.clear();
                snapshot.children.forEach {
                    var user: userPublicModel = it.getValue(userPublicModel::class.java)!!;

                    if(!email.equals(user.email)) {
                        userList.add(user);
                    }

                }
                userAdapter.notifyDataSetChanged();
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("cancelled",error.message);
            }

        });

    }

    private fun manageLogic() {
        userAdapter = usersAdapter(userList, this);
        var llm : LinearLayoutManager = LinearLayoutManager(this);
        llm.orientation = LinearLayoutManager.VERTICAL;
        binding.userRV.layoutManager = llm;
        binding.userRV.adapter = userAdapter;
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left)
        userRef.child(helper().cleanWord(email.toString())).child("status").setValue("Offline");
    }

    override fun onBackPressed() {

        val builder = AlertDialog.Builder(this@mainActivity)

        builder.setMessage("Do you want to exit?")

        builder.setTitle("Exit!")

        builder.setCancelable(false)


        builder.setPositiveButton("Yes",
           { dialog: DialogInterface?, which: Int ->
                finish()
            })

        builder.setNegativeButton("No",
         { dialog: DialogInterface, which: Int ->
                dialog.cancel()
            })


        val alertDialog = builder.create()
        alertDialog.show()
    }
}