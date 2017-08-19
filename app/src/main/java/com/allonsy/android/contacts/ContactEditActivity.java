package com.allonsy.android.contacts;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import java.util.UUID;


public class ContactEditActivity extends SingleFragmentActivity {

    private static final String EXTRA_CONTACT =
            "com.allonsy.android.contacts.contact";

    public static Intent newIntent(Context packageContext, Contact contact) {
        Intent intent = new Intent(packageContext, ContactEditActivity.class);
        intent.putExtra(EXTRA_CONTACT, contact);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Contact contact = (Contact) getIntent()
                .getSerializableExtra(EXTRA_CONTACT);
        return ContactEditFragment.newInstance(contact);
    }

}
