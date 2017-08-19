package com.allonsy.android.contacts.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.allonsy.android.contacts.database.ContactDbSchema.PersonTable;
import com.allonsy.android.contacts.database.ContactDbSchema.PhoneTable;
import com.allonsy.android.contacts.database.ContactDbSchema.EmailTable;

public class ContactBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "contactBase.db";

    public ContactBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + PersonTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                PersonTable.Cols.UUID + ", " +
                PersonTable.Cols.NAME +
                ")"
        );
        db.execSQL("create table " + PhoneTable.NAME + "(" +
                " _id integer primary key not null, " +
                PhoneTable.Cols.UUID + ", " +
                PhoneTable.Cols.PHONE + ", " +
                "foreign key (" + PhoneTable.Cols.UUID + ") " +
                "references " + PersonTable.NAME + "(" + PersonTable.Cols.UUID + ") " +
                ")"
        );
        db.execSQL("create table " + EmailTable.NAME + "(" +
                " _id integer primary key not null, " +
                EmailTable.Cols.UUID + ", " +
                EmailTable.Cols.EMAIL + ", " +
                "foreign key (" + EmailTable.Cols.UUID + ") " +
                "references " + PersonTable.NAME + "(" + PersonTable.Cols.UUID + ") " +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}