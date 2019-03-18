package com.allonsy.android.contacts;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.allonsy.android.contacts.database.ContactBaseHelper;
import com.allonsy.android.contacts.database.ContactDbSchema.PersonTable;
import com.allonsy.android.contacts.database.ContactDbSchema.PhoneTable;
import com.allonsy.android.contacts.database.ContactDbSchema.EmailTable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ContactLab {

    private static ContactLab sContactLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static ContactLab get(Context context) {
        if (sContactLab == null) {
            sContactLab = new ContactLab(context);
        }
        return sContactLab;
    }
    private ContactLab(Context context)
    {
        mContext = context.getApplicationContext();
        mDatabase = new ContactBaseHelper(mContext)
                .getWritableDatabase();
    }

    public void addContact(Contact c)
    {
        ContentValues personValues = getPersonContentValues(c);
        List<ContentValues> phoneValues = getPhoneContentValues(c);
        List<ContentValues> emailValues = getEmailContentValues(c);

        mDatabase.insert(PersonTable.NAME, null, personValues);
        for(int i=0; i!=phoneValues.size();i++)
             mDatabase.insert(PhoneTable.NAME, null, phoneValues.get(i));
        for(int i=0; i!=emailValues.size();i++)
            mDatabase.insert(EmailTable.NAME, null, emailValues.get(i));
    }


    public void updateContact(Contact contact) {
        String uuidString = contact.getId().toString();

        //update name
        ContentValues values = getPersonContentValues(contact);
        mDatabase.update(PersonTable.NAME, values,
                PersonTable.Cols.UUID + " = ?",
                new String[] { uuidString });

        String temp;

        //update phones
        List<String> newPhones= new ArrayList<>();
        List<String> databasePhones = getPhones(uuidString);
        List<String> contactPhones = contact.getPhones();

        //check for blank and null phones
        for(int i=0; i!=contactPhones.size();i++) {
            temp = contactPhones.get(i);
            if(temp != null && !temp.isEmpty()) {
                newPhones.add(temp);
            }

        }

        //check if newPhones and databasePhones are different and update
        if(databasePhones != null && (databasePhones.size() == newPhones.size())){
            databasePhones.removeAll(newPhones);
            if(!databasePhones.isEmpty()) { //both lists not same.
                mDatabase.delete(PhoneTable.NAME,
                        PhoneTable.Cols.UUID + " = ?",
                        new String[] { uuidString });
                contact.setPhones(newPhones);
                List<ContentValues> phoneValues = getPhoneContentValues(contact);
                for(int i=0; i!=phoneValues.size();i++)
                    mDatabase.insert(PhoneTable.NAME, null, phoneValues.get(i));
            }
        }
        else if (databasePhones != null && (databasePhones.size() != newPhones.size())){
                mDatabase.delete(PhoneTable.NAME,
                        PhoneTable.Cols.UUID + " = ?",
                        new String[] { uuidString });
                contact.setPhones(newPhones);
                List<ContentValues> phoneValues = getPhoneContentValues(contact);

            for(int i=0; i!=phoneValues.size();i++)
                    mDatabase.insert(PhoneTable.NAME, null, phoneValues.get(i));
        }


        //update emails
        List<String> newEmails= new ArrayList<>();
        List<String> databaseEmails = getEmails(uuidString);
        List<String> contactEmails = contact.getEmails();
        //Log.d("allonsy.contacts", "'Emails Size " + contactEmails.size() + "'");

        //check for blank and null emails
        for(int i=0; i!=contactEmails.size();i++) {
            temp = contactEmails.get(i);
            if(temp != null && !temp.isEmpty())
                newEmails.add(temp);
        }

        //check if newEmails and databaseEmails are different and update
        if(databaseEmails != null && (databaseEmails.size() == newEmails.size())){
            databaseEmails.removeAll(newEmails);
            if(!databaseEmails.isEmpty()) { //both lists not same.
                mDatabase.delete(EmailTable.NAME,
                        EmailTable.Cols.UUID + " = ?",
                        new String[] { uuidString });
                contact.setEmails(newEmails);
                List<ContentValues> emailValues = getEmailContentValues(contact);
                for(int i=0; i!=emailValues.size();i++)
                    mDatabase.insert(EmailTable.NAME, null, emailValues.get(i));
            }
        }
        else if(databaseEmails != null && (databaseEmails.size() != newEmails.size())){
            mDatabase.delete(EmailTable.NAME,
                    EmailTable.Cols.UUID + " = ?",
                    new String[] { uuidString });
            contact.setEmails(newEmails);
            List<ContentValues> emailValues = getEmailContentValues(contact);
            for(int i=0; i!=emailValues.size();i++)
                mDatabase.insert(EmailTable.NAME, null, emailValues.get(i));
        }
    }

    public void deleteContact(Contact contact) {
        String uuidString = contact.getId().toString();

        //delete contact
        mDatabase.delete(PersonTable.NAME,
                PersonTable.Cols.UUID + " = ?",
                new String[] { uuidString });

        mDatabase.delete(PhoneTable.NAME,
                PhoneTable.Cols.UUID + " = ?",
                new String[] { uuidString });

        mDatabase.delete(EmailTable.NAME,
                EmailTable.Cols.UUID + " = ?",
                new String[] { uuidString });
    }

    public List<Contact> getContacts()
    {
        List<Contact> contacts = new ArrayList<>();
        ContactCursorWrapper personCursor = queryContacts(PersonTable.NAME,null, null);
        try {
            personCursor.moveToFirst();
            while (!personCursor.isAfterLast()) {
                String uuid = personCursor.getContactPersonUUID();
                String name = personCursor.getContactPersonName();
                List<String> phones = getPhones(uuid);
                List<String> emails = getEmails(uuid);

                Contact contact = new Contact(UUID.fromString(uuid));
                contact.setName(name);
                contact.setPhones(phones);
                contact.setEmails(emails);

                contacts.add(contact);
                personCursor.moveToNext();
            }
        } finally {
            personCursor.close();
        }
        return contacts;
    }

    public List<Contact> searchContactsByName(String search)
    {
        List<Contact> contacts = new ArrayList<>();
        ContactCursorWrapper personCursor = queryContacts(PersonTable.NAME, PersonTable.Cols.NAME + " LIKE ?",
                new String[] { "%" + search + "%" });
        try {
            personCursor.moveToFirst();
            while (!personCursor.isAfterLast()) {
                String uuid = personCursor.getContactPersonUUID();
                String name = personCursor.getContactPersonName();
                List<String> phones = getPhones(uuid);
                List<String> emails = getEmails(uuid);

                Contact contact = new Contact(UUID.fromString(uuid));
                contact.setName(name);
                contact.setPhones(phones);
                contact.setEmails(emails);

                contacts.add(contact);
                personCursor.moveToNext();
            }
        } finally {
            personCursor.close();
        }
        return contacts;
    }

    public List<String> getPhones(String id)
    {
        List<String> phones = new ArrayList<>();

        ContactCursorWrapper cursor = queryContacts(
                PhoneTable.NAME,
                PhoneTable.Cols.UUID + " = ?",
                new String[] { id }
        );
        try {
            if (cursor.getCount() == 0) {
                return new ArrayList<>();
            }
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                phones.add(cursor.getContactPhonePhone());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return phones;
    }

    public List<String> getEmails(String id)
    {
        List<String> emails = new ArrayList<>();

        ContactCursorWrapper cursor = queryContacts(
                EmailTable.NAME,
                EmailTable.Cols.UUID + " = ?",
                new String[] { id }
        );
        try {
            if (cursor.getCount() == 0) {
                return new ArrayList<>();
            }
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                emails.add(cursor.getContactEmailEmail());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return emails;
    }

    public Contact getContact(UUID id) {

        ContactCursorWrapper personCursor = queryContacts(
                PersonTable.NAME,
                PersonTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );
        try {
            if (personCursor.getCount() == 0) {
                return null;
            }
            personCursor.moveToFirst();

            String uuid = personCursor.getContactPersonUUID();
            String name = personCursor.getContactPersonName();
            List<String> phones = getPhones(uuid);
            List<String> emails = getEmails(uuid);

            Contact contact = new Contact(UUID.fromString(uuid));
            contact.setName(name);
            contact.setPhones(phones);
            contact.setEmails(emails);

            return contact;
        } finally {
            personCursor.close();
        }
    }

    public File getPhotoFile(Contact contact) {
        File externalFilesDir = mContext
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (externalFilesDir == null) {
            return null;
        }
        return new File(externalFilesDir, contact.getPhotoFilename());
    }

    private static ContentValues getPersonContentValues(Contact contact) {
        ContentValues values = new ContentValues();
        values.put(PersonTable.Cols.UUID, contact.getId().toString());
        values.put(PersonTable.Cols.NAME, contact.getName());
        return values;
    }

    private static List<ContentValues> getPhoneContentValues(Contact contact) {

        List<ContentValues>  values =  new ArrayList<>();
        List<String> phones = contact.getPhones();

        for(int i=0; i!=phones.size();i++) {
            ContentValues value = new ContentValues();
            value.put(PhoneTable.Cols.UUID, contact.getId().toString());
            value.put(PhoneTable.Cols.PHONE, phones.get(i));
            values.add(value);
        }

        return values;
    }

    private static List<ContentValues> getEmailContentValues(Contact contact) {

        List<ContentValues>  values =  new ArrayList<>();
        List<String> emails = contact.getEmails();

        for(int i=0; i!=emails.size();i++) {
            ContentValues value = new ContentValues();
            value.put(EmailTable.Cols.UUID, contact.getId().toString());
            value.put(EmailTable.Cols.EMAIL, emails.get(i));
            values.add(value);
        }

        return values;
    }

    private ContactCursorWrapper queryContacts(String table, String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                table,
                null, // Columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null // orderBy
        );
        return new ContactCursorWrapper(cursor);
    }

}