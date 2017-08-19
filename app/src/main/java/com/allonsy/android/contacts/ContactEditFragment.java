package com.allonsy.android.contacts;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContactEditFragment extends Fragment {

    private Contact mContact;
    private File mTempPhotoFile;
    private String mTempPhotoName;
    private File mContactPhotoFile;
    private String mContactPhotoName;
    private String pictureStoragePath;
    private EditText mName;
    String name;
    private ImageView mPhotoView;
    private ImageButton mPhotoAdd;
    private ImageButton mPhotoDelete;
    private List<EditText> mPhones;
    List<String> phones;
    private Button mAddPhoneButton;
    private List<EditText> mEmails;
    List<String> emails;
    private Button mAddEmailButton;

    private LinearLayout mPhonesLayout;
    private LinearLayout mEmailsLayout;


    int imageViewWidth=0;
    int imageViewHeight=0;


    private static final String ARG_CONTACT = "contact";
    private static final String DIALOG_CONTACT_IMAGE = "DialogContactImage";
    public static final String CONTACT_OBJECT = "contactObject";
    private static final int REQUEST_PHOTO= 0;
    public static final int ADD_CONTACT= 0;
    public static final int UPDATE_CONTACT= 1;
    public static final String RETURN_STATE = "contactState";
    private static boolean deletePhoto;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContact = (Contact) getArguments().getSerializable(ARG_CONTACT);

        if (savedInstanceState != null) {
            mContact = (Contact) savedInstanceState.getSerializable(ARG_CONTACT);
        }
        if(mContact==null)
            getActivity().finish();

        mTempPhotoFile = null;
        mContactPhotoFile = null;
        File appDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(appDir!=null)
            if (appDir.exists()) {
                pictureStoragePath = appDir.getAbsolutePath() + File.separator;
                mTempPhotoFile = new File(appDir.getAbsolutePath() + File.separator + "temp.png");
                mContactPhotoFile = new File(appDir.getAbsolutePath() + File.separator + mContact.getPhotoFilename());
                mTempPhotoName = "temp.png";
                mContactPhotoName = mContact.getPhotoFilename();
            }


    }

    @Override
    public void onResume() {
        super.onResume();
        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){
                    save();
                    return true;
                }
                return false;
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contact_edit, container, false);

        mName = (EditText) v.findViewById(R.id.edit_contact_name);
        name=mContact.getName();
        mName.setText(name);
        mName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This space intentionally left blank
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                name=s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {
                // This one too
            }
        });

        //copy contacts photo to temp.png if it exists
        if ( mContactPhotoFile != null && mContactPhotoFile.exists()) {
            PictureUtils.copyFile(pictureStoragePath, mContactPhotoName, pictureStoragePath , mTempPhotoName);
        }


        mPhotoView = (ImageView) v.findViewById(R.id.edit_contact_photo);
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
                if (mTempPhotoFile != null && mTempPhotoFile.exists()) {
                    FragmentManager manager = getFragmentManager();
                    ContactImageFragment dialog = ContactImageFragment
                            .newInstance(mTempPhotoFile.getAbsolutePath());
                    dialog.show(manager, DIALOG_CONTACT_IMAGE);
                }
            }
        });


        final Intent pickImage = new Intent(Intent.ACTION_PICK);
        pickImage.setType("image/*");
        deletePhoto =false;
        mPhotoAdd = (ImageButton) v.findViewById(R.id.edit_contact_photo_add);
        mPhotoAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickImage, REQUEST_PHOTO);
                }
        });

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickImage,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mPhotoAdd.setEnabled(false);
        }

        mPhotoDelete = (ImageButton) v.findViewById(R.id.edit_contact_photo_delete);
        mPhotoDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePhoto=true;
                PictureUtils.deleteFile(pictureStoragePath, mTempPhotoName);
                updatePhotoView();
            }
        });



        mPhonesLayout = (LinearLayout) v.findViewById(R.id.edit_contact_phones_list);
        phones = mContact.getPhones();
        mPhones = new ArrayList<>();
        if(phones.size()>0) {
            for (int i = 0; i != phones.size(); i++) {
                addPhoneEditText(phones.get(i));
            }
        }
        else
        {
            addPhoneEditText("");
            phones.add("");
        }

        mAddPhoneButton = (Button)v.findViewById(R.id.edit_contact_add_phone_button);
        mAddPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPhones.size()<=10) {
                    addPhoneEditText("");
                    phones.add("");
                }
                else
                    Toast.makeText(getActivity(), "Max limit is 10",
                            Toast.LENGTH_LONG).show();
            }
        });


        mEmailsLayout = (LinearLayout) v.findViewById(R.id.edit_contact_emails_list);
        emails = mContact.getEmails();
        mEmails = new ArrayList<>();
        if(emails.size()>0) {
            for (int i = 0; i != emails.size(); i++) {
                addEmailEditText(emails.get(i));
            }
        }

        else {
            addEmailEditText("");
            emails.add("");
        }



        mAddEmailButton = (Button)v.findViewById(R.id.edit_contact_add_email_button);
        mAddEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(mEmails.size()<=10) {
                addEmailEditText("");
                emails.add("");
            }
            else {
                Toast.makeText(getActivity(), "Max limit is 10",
                        Toast.LENGTH_LONG).show();
            }
            }
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_contact_edit, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_save:
                save();
                return true;
            case R.id.menu_item_cancel:
                cancel();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void save() {
        boolean valid = true;

        //check name validity
        if (name.isEmpty() || (!name.isEmpty() && !name.matches("^[\\p{L} .'-{0-9}]+$"))) {
            mName.setError("enter a valid name");
            valid = false;
        } else if (name.length() > 25) {
            mName.setError("name can only be 25 chars");
            valid = false;
        } else
            mName.setError(null);

        for (int i = 0; i != mPhones.size(); i++) {
            String phone = phones.get(i);
            //check phone validity
            if (!phone.isEmpty() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
                mPhones.get(i).setError("enter a valid phone number");
                valid = false;
            }
            if (phone.length() > 25) {
                mPhones.get(i).setError("phone can only be 25 chars");
                valid = false;
            } else
                mPhones.get(i).setError(null);

        }

        for (int i = 0; i != mEmails.size(); i++) {
            String email = emails.get(i);

            //check email validity
            if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mEmails.get(i).setError("enter a valid email address");
                valid = false;
            } else if (email.length() > 25) {
                mEmails.get(i).setError("email can only be 25 chars");
                valid = false;
            } else
                mEmails.get(i).setError(null);

        }

        if (valid) {
            mContact.setName(name);
            List<String> newPhones = new ArrayList<>();
            for (int i = 0; i != phones.size(); i++) {
                if(!phones.get(i).isEmpty())
                    newPhones.add(phones.get(i));
            }
            mContact.setPhones(newPhones);

            List<String> newEmails = new ArrayList<>();

            for (int i = 0; i != emails.size(); i++) {
                if(!emails.get(i).isEmpty())
                    newEmails.add(emails.get(i));
            }
            mContact.setEmails(newEmails);

            ContactLab.get(getActivity()).updateContact(mContact);
            Intent resultIntent = new Intent();
            if (deletePhoto) {
                PictureUtils.deleteFile(pictureStoragePath, mContactPhotoName);
            }

            //save temp.png as contacts photo if it exists
            if (!deletePhoto && mTempPhotoFile != null && mTempPhotoFile.exists()) {
                PictureUtils.moveFile(pictureStoragePath, mTempPhotoName, pictureStoragePath, mContactPhotoName);
            } else
                PictureUtils.deleteFile(pictureStoragePath, mTempPhotoName);

            Intent resultIntent1 = new Intent();
            resultIntent1.putExtra(RETURN_STATE, "1");
            resultIntent1.putExtra(CONTACT_OBJECT, mContact);
            getActivity().setResult(Activity.RESULT_OK, resultIntent1);

            getActivity().finish();
        }
    }


    private void cancel() {
        //delete temp.png if it exists
        if ( mTempPhotoFile != null && mTempPhotoFile.exists()) {
            PictureUtils.deleteFile(pictureStoragePath, mTempPhotoName);
        }
        Intent resultIntent2 = new Intent();
        resultIntent2.putExtra(RETURN_STATE, "0");
        getActivity().setResult(Activity.RESULT_OK, resultIntent2);
        getActivity().finish();
    }

    private void updatePhotoView() {
        if (mTempPhotoFile == null || !mTempPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap image = PictureUtils.getScaledBitmap(
                    mTempPhotoFile.getAbsolutePath(), imageViewWidth, imageViewHeight);
            mPhotoView.setImageBitmap(image);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_PHOTO) {
            Uri selectedImage = data.getData();
            try {
                if(selectedImage!=null) {
                    Toast.makeText(getActivity(), "Photo Selected",
                            Toast.LENGTH_SHORT).show();

                    InputStream imageStream = getContext().getContentResolver().openInputStream(selectedImage);



                    Bitmap image = PictureUtils.decodeUri(selectedImage, getActivity().getApplicationContext(),
                            imageViewWidth, imageViewHeight);

                    if(mTempPhotoFile != null) {
                        FileOutputStream out = new FileOutputStream(mTempPhotoFile);
                        image.compress(Bitmap.CompressFormat.PNG, 100, out);
                        updatePhotoView();
                        deletePhoto=false;
                    }
                    else
                        Toast.makeText(getActivity(), "Sorry, Picture Storage Support not available",
                                Toast.LENGTH_LONG).show();
                }
            }
            catch (IOException ioException) {

            }

        }
    }
    private void addPhoneEditText(String text)
    {
        EditText phoneEditText = new EditText(getContext());
        mPhones.add(phoneEditText);
        int i = mPhones.size()-1;
        mPhones.get(i).setText(text);
        mPhones.get(i).setId(i);
        mPhones.get(i).addTextChangedListener(new PhoneTextWatcher(mPhones.get(i)));
        mPhonesLayout.addView(mPhones.get(i));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mPhones.get(i).getLayoutParams();
        params.setMargins(10, 0, 10, 0);
        mPhones.get(i).setLayoutParams(params);
    }

    private void addEmailEditText(String text)
    {
        EditText emailEditText = new EditText(getContext());
        mEmails.add(emailEditText);
        int i = mEmails.size()-1;
        mEmails.get(i).setText(text);
        mEmails.get(i).setId(i);
        mEmails.get(i).addTextChangedListener(new EmailTextWatcher(mEmails.get(i)));
        mEmailsLayout.addView(mEmails.get(i));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mEmails.get(i).getLayoutParams();
        params.setMargins(10, 0, 10, 0);
        mEmails.get(i).setLayoutParams(params);
    }


    public class PhoneTextWatcher implements TextWatcher {
        private EditText mEditText;

        public PhoneTextWatcher(EditText e) {
            mEditText = e;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            phones.set(mEditText.getId(),s.toString());

        }

        public void afterTextChanged(Editable s) {
        }
    }

    public class EmailTextWatcher implements TextWatcher {
        private EditText mEditText;

        public EmailTextWatcher(EditText e) {
            mEditText = e;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            emails.set(mEditText.getId(),s.toString());
        }

        public void afterTextChanged(Editable s) {
        }
    }

    public static ContactEditFragment newInstance(Contact contact) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTACT, contact);
        ContactEditFragment fragment = new ContactEditFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        mContact.setName(name);
        mContact.setPhones(phones);
        mContact.setEmails(emails);
        savedInstanceState.putSerializable(ARG_CONTACT, mContact);
    }
}
