package com.allonsy.android.contacts;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Contact implements Serializable {
    private UUID mId;
    private String mName;
    private List<String> mPhones = new ArrayList<>();
    private List<String> mEmails = new ArrayList<>();

    public Contact() {
        // Generate unique identifier
        mName="";
        mId = UUID.randomUUID();

    }

    public Contact(UUID id) {
        mId = id;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public List<String> getPhones() {
        return mPhones;
    }

    public void setPhones(List<String> phones) {
        mPhones = phones;
    }

    public void addPhone(String phone) {
        mPhones.add(phone);
    }

    public List<String> getEmails() {
        return mEmails;
    }

    public void setEmails(List<String> emails) {
        mEmails = emails;
    }

    public void addEmail(String email) {
        mEmails.add(email);
    }

    public String getPhotoFilename() {
        return "IMG_" + getId().toString() + ".png";
    }
}