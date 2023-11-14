package com.example.shifttracker.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shifttracker.R
import com.example.shifttracker.adapter.ShiftAdapter
import com.example.shifttracker.databinding.FragmentPastShiftsBinding
import com.example.shifttracker.model.TimeValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class PastShifts : Fragment() {
    private lateinit var binding: FragmentPastShiftsBinding
    private lateinit var adapter: ShiftAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val shifts = ArrayList<TimeValue>()
    private lateinit var auth: FirebaseAuth





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPastShiftsBinding.inflate(inflater, container, false)

        // Firestore'dan verileri çek
        fetchShiftList()

        adapter = ShiftAdapter(shifts)
        binding.shiftRc.layoutManager = LinearLayoutManager(requireContext())
        binding.shiftRc.adapter = adapter

        return binding.root
    }

    private fun fetchShiftList() {
        val email = auth.currentUser!!.email.toString()

        firestore.collection(email).orderBy("timestamp" , Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                shifts.clear() // Önceki verileri temizle
                for (document in documents) {
                    val entryExit = document.getString("label")
                    val time = document.getString("time")
                    val timestamp = document.getLong("timestamp")
                    if (entryExit != null && time != null && timestamp!=null) {
                        val shift = TimeValue(entryExit, time,timestamp)
                        shifts.add(shift)
                    }
                }
                adapter.updateData(shifts) // RecyclerView'ı güncelle
            }
            .addOnFailureListener { exception ->
                println("Firestore'dan verileri çekerken hata oluştu: $exception")
            }
    }


}