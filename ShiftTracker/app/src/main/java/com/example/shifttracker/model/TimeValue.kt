package com.example.shifttracker.model

data class TimeValue(val time: String, val label : String,val timestamp: Long = System.currentTimeMillis())