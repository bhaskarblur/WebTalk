package com.bhaskarblur.webtalk.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bhaskarblur.webtalk.R
import com.bhaskarblur.webtalk.model.userPublicModel

class usersAdapter : RecyclerView.Adapter<usersAdapter.viewHolder> {

    private lateinit var list : ArrayList<userPublicModel>;
    private lateinit var content : Context;

    constructor(list: ArrayList<userPublicModel>, content: Context) : super() {
        this.list = list
        this.content = content
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        var view = LayoutInflater.from(content).inflate(R.layout.user_layout, parent, false);
        return viewHolder(view);
    }

    override fun getItemCount(): Int {
        return list.size;
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {

        holder.name.setText(list.get(position).username);
        holder.status.setText(list.get(position).status);
        Log.d("statusUser", list.get(position).status);
        if(list.get(position).status.equals("Offline")) {
            holder.video.setVisibility(View.GONE);
            holder.voice.setVisibility(View.GONE);

        }
        else {
            holder.video.setVisibility(View.VISIBLE);
            holder.voice.setVisibility(View.VISIBLE);
        }
    }


    class viewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var name : TextView = itemView.findViewById(R.id.userName);
        var status : TextView = itemView.findViewById(R.id.userStatus);
        var video : ImageView = itemView.findViewById(R.id.videoIcon);
        var voice : ImageView = itemView.findViewById(R.id.voiceIcon);

        init {

        }
    }
}