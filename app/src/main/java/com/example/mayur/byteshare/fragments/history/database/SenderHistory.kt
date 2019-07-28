package com.example.mayur.xportal.fragments.history.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SenderHistory(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    val allData: Cursor
        get() {
            val db = this.writableDatabase
            return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + TABLE_NAME + "("
                    + COL_FILE_NAME + " TEXT,"
                    + COL_FILE_SIZE + " TEXT,"
                    + COL_DATE + " TEXT,"
                    + COL_ABS_PATH + " TEXT);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertData(filename: String, size: String, date: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_FILE_NAME, filename)
        contentValues.put(COL_FILE_SIZE, size)
        contentValues.put(COL_DATE, date)
        db.insert(TABLE_NAME, null, contentValues)
    }

    fun clear() {
        writableDatabase.execSQL("DELETE FROM $TABLE_NAME")
    }

    companion object {
        const val DATABASE_NAME = "senderhistory.db"
        const val TABLE_NAME = "history"
        const val COL_FILE_NAME = "FILENAME"
        const val COL_FILE_SIZE = "SIZE"
        const val COL_DATE = "DATE"
        const val COL_ABS_PATH = "ABS_PATH"
    }
}