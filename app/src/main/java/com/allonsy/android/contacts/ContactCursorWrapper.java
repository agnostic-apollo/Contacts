package com.allonsy.android.contacts;


import android.database.Cursor;
import android.database.CursorWrapper;

import com.allonsy.android.contacts.database.ContactDbSchema.PersonTable;
import com.allonsy.android.contacts.database.ContactDbSchema.PhoneTable;
import com.allonsy.android.contacts.database.ContactDbSchema.EmailTable;

import java.util.Date;
import java.util.UUID;

public class ContactCursorWrapper extends CursorWrapper {
    public ContactCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public String getContactPersonUUID() {
        return getString(getColumnIndex(PersonTable.Cols.UUID));

    }

    public String getContactPersonName() {
        return getString(getColumnIndex(PersonTable.Cols.NAME));
    }

    public String getContactPhoneUUID() {
        return getString(getColumnIndex(PhoneTable.Cols.UUID));
    }

    public String getContactPhonePhone() {
        return getString(getColumnIndex(PhoneTable.Cols.PHONE));
    }

    public String getContactEmailUUID() {
        return getString(getColumnIndex(EmailTable.Cols.UUID));
    }

    public String getContactEmailEmail() {
        return getString(getColumnIndex(EmailTable.Cols.EMAIL));
    }
}