package com.example.shifttracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shifttracker.databinding.ItemLayoutBinding
import com.example.shifttracker.model.TimeValue

class ShiftAdapter(var shiftlist: ArrayList<TimeValue>) : RecyclerView.Adapter<ShiftAdapter.ShiftHolder>(){

    class ShiftHolder(val binding : ItemLayoutBinding) : RecyclerView.ViewHolder(binding.root){

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftHolder {
        val binding = ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShiftHolder(binding)

    }

    override fun onBindViewHolder(holder: ShiftHolder, position: Int) {
        val currentStatu = shiftlist[position]
        holder.binding.tvSubtitle.text=currentStatu.label
        holder.binding.tvMainTitle.text=currentStatu.time

    }

    override fun getItemCount(): Int {
        return shiftlist.size
    }
    fun updateData(list: ArrayList<TimeValue>) {
        shiftlist = list
        notifyDataSetChanged()
    }
}