package com.example.mayur.xportal.fragments.history

data class HistoryInfo(var progress: Int, internal var fileName: String, internal var totalSize: Long) {
    var date: Long = 0
    var sentSize:Long = 0
    fun setDate(currentTimeMillis: Long): HistoryInfo {
        date = currentTimeMillis
        return this
    }
}