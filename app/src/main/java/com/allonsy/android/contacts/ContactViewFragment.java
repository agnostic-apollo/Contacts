package com.allonsy.android.contacts;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import au.com.bytecode.opencsv.CSVWriter;

public class ContactViewFragment extends Fragment {

    private Contact mContact;
    private File mPhotoFile;
    private TextView mName;
    private List<TextView> mPhones;
    List<String> phones;
    private List<TextView> mEmails;
    List<String> emails;
    private ImageView mPhotoView;
    private LinearLayout mPhonesLayout;
    private LinearLayout mEmailsLayout;
    int imageViewWidth=0;
    int imageViewHeight=0;


    private static final String ARG_CONTACT_ID = "contact_id";
    private static final String DIALOG_CONTACT_IMAGE = "DialogContactImage";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        UUID contactId = (UUID) getArguments().getSerializable(ARG_CONTACT_ID);
        mContact = ContactLab.get(getActivity()).getContact(contactId);
        mPhotoFile = ContactLab.get(getActivity()).getPhotoFile(mContact);

    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contact_view, container, false);

        mName = (TextView) v.findViewById(R.id.view_contact_name);


        mPhotoView = (ImageView) v.findViewById(R.id.view_contact_photo);
        final ViewTreeObserver vto = mPhotoView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                imageViewWidth = mPhotoView.getWidth();
                imageViewHeight = mPhotoView.getHeight();

                updatePhotoView();

                //Then remove layoutChange Listener
                ViewTreeObserver vto = mPhotoView.getViewTreeObserver();
                vto.removeOnGlobalLayoutListener(this);
            }
        });


        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoFile != null && mPhotoFile.exists()) {
                    FragmentManager manager = getFragmentManager();
                    ContactImageFragment dialog = ContactImageFragment
                            .newInstance(mPhotoFile.getPath());
                    dialog.show(manager, DIALOG_CONTACT_IMAGE);
                }
            }
        });


        mPhonesLayout = (LinearLayout) v.findViewById(R.id.view_contact_phones_list);

        mEmailsLayout = (LinearLayout) v.findViewById(R.id.view_contact_emails_list);



        return v;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_contact_view, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_contact_edit:
                Intent intent = ContactEditActivity.newIntent(getActivity(), mContact, ContactEditFragment.UPDATE_CONTACT);
                startActivityForResult(intent,ContactEditFragment.UPDATE_CONTACT);
                //getActivity().finish();
                return true;
            case R.id.menu_item_contact_delete:
                showConfirmDeleteDialogue();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateUI()
    {
        if(mContact!=null)
        {
            mName.setText(mContact.getName());

            mPhonesLayout.removeAllViews();
            phones = mContact.getPhones();
            mPhones = new ArrayList<>();
            for(int i=0;i!=phones.size();i++)
            {
                addPhoneTextView(phones.get(i));
            }

            mEmailsLayout.removeAllViews();
            emails = mContact.getEmails();
            mEmails = new ArrayList<>();
            for(int i=0;i!=emails.size();i++)
            {
                addEmailTextView(emails.get(i));
            }

            if(imageViewWidth!=0 && imageViewHeight!=0)
                updatePhotoView();
        }
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), imageViewWidth, imageViewHeight);
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    private void addPhoneTextView(String text)
    {
        TextView phoneTextView = new TextView(getContext());
        mPhones.add(phoneTextView);
        int i = mPhones.size()-1;
        mPhones.get(i).setText(text);
        //mPhones.get(i).setHeight(30);
        mPhones.get(i).setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        mPhones.get(i).setPadding(10,10,10,10);
        mPhones.get(i).setId(i);
        mPhones.get(i).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + mPhones.get(v.getId()).getText().toString().trim()));
                startActivity(intent);
            }
        });
        mPhonesLayout.addView(mPhones.get(i));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mPhones.get(i).getLayoutParams();
        params.setMargins(30, 30, 30, 30);
        mPhones.get(i).setLayoutParams(params);
        mPhones.get(i).setBackground(getBorderDrawable());

    }

    private void addEmailTextView(String text)
    {
        TextView emailTextView = new TextView(getContext());
        mEmails.add(emailTextView);
        int i = mEmails.size()-1;
        mEmails.get(i).setText(text);
        mEmails.get(i).setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        mEmails.get(i).setPadding(10,10,10,10);
        mEmails.get(i).setId(i);
        mEmails.get(i).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, mEmails.get(v.getId()).getText().toString().trim());
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        mEmailsLayout.addView(mEmails.get(i));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mEmails.get(i).getLayoutParams();
        params.setMargins(30, 30, 30, 30);
        mEmails.get(i).setLayoutParams(params);
        mEmails.get(i).setBackground(getBorderDrawable());
    }

    private ShapeDrawable getBorderDrawable()
    {
        ShapeDrawable sd = new ShapeDrawable();
        sd.setShape(new RectShape());
        sd.getPaint().setColor(Color.LTGRAY);
        sd.getPaint().setStrokeWidth(10f);
        sd.getPaint().setStyle(Paint.Style.STROKE);
        return sd;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == ContactEditFragment.UPDATE_CONTACT) {

            String returnValue = data.getStringExtra(ContactEditFragment.RETURN_STATE);
            if(returnValue!=null) {
                if (returnValue.equals("0")){
                    Toast.makeText(getActivity(), "cancelled",
                            Toast.LENGTH_SHORT).show();
                }
                else if (returnValue.equals("1")) {
                    Contact contact = (Contact) data.getSerializableExtra(ContactEditFragment.CONTACT_OBJECT);
                    if(contact!=null) {
                        Toast.makeText(getActivity(), "saved",
                                Toast.LENGTH_SHORT).show();
                        mContact=contact;
                    }
                }
            }
        }
    }

    public static ContactViewFragment newInstance(UUID contactId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACT_ID, contactId);
        ContactViewFragment fragment = new ContactViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void showConfirmDeleteDialogue() {

        new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Delete")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContactLab.get(getActivity()).deleteContact(mContact);
                        getActivity().finish();
                    }

                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }

                })
                .show();
    }

}
