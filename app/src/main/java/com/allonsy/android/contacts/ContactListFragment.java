package com.allonsy.android.contacts;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import au.com.bytecode.opencsv.CSVWriter;

import static android.content.ContentValues.TAG;

public class ContactListFragment extends Fragment {

    private RecyclerView mContactRecyclerView;
    private TextView mContactTextView;
    private ContactAdapter mAdapter;
    private String mQuery;
    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";

    private static final String CONTACT_ID = "contactID";

    public static String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list, container, false);
        mContactRecyclerView = (RecyclerView) view
                .findViewById(R.id.contact_recycler_view);
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mContactTextView = (TextView) view
                .findViewById(R.id.empty_view);

        updateUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_contact_list, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                mQuery=s;
                updateUI();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setQuery(mQuery, false);
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener()
        {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item)
            {
                mQuery=null;
                updateUI();
                return true; // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item)
            {
                return true;
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_new_contact:
                Contact contact = new Contact();
                Intent intent = ContactEditActivity.newIntent(getActivity(), contact, ContactEditFragment.ADD_CONTACT);
                startActivityForResult(intent,ContactEditFragment.ADD_CONTACT);
                return true;
            case R.id.menu_item_export_contacts:
                boolean permissionsGranted = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    permissionsGranted = checkPermissions(getActivity());

                if(!permissionsGranted)
                    askPermissions();
                else {
                    ExportDatabaseCSVTask task = new ExportDatabaseCSVTask();
                    task.execute();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateSubtitle() {
        ContactLab contactLab = ContactLab.get(getActivity());

        int contactSize = contactLab.getContacts().size();
        String subtitle = getResources()
                .getQuantityString(R.plurals.subtitle_plural, contactSize, contactSize);


        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);
    }

    private void updateUI() {
        ContactLab contactLab = ContactLab.get(getActivity());
        List<Contact> contacts;
        if(mQuery==null)
            contacts = contactLab.getContacts();
        else
            contacts = contactLab.searchContactsByName(mQuery);

        if (mAdapter == null) {
            mAdapter = new ContactAdapter(contacts);
            mContactRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setContacts(contacts);
            mAdapter.notifyDataSetChanged();
            //mAdapter.notifyItemChanged(mAdapter.getPosition());
        }

        int contactSize = contactLab.getContacts().size();

        if (contactSize==0) {
            mContactRecyclerView.setVisibility(View.GONE);
            mContactTextView.setVisibility(View.VISIBLE);
        }
        else {
            mContactRecyclerView.setVisibility(View.VISIBLE);
            mContactTextView.setVisibility(View.GONE);
        }

        updateSubtitle();
    }

    private class ContactHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mNameTextView;
        private ImageView mPhotoView;
        private CheckBox mSolvedCheckBox;
        private Contact mContact;
        private ContactAdapter mAdapter;
        int imageViewWidth=0;
        int imageViewHeight=0;



        public ContactHolder(View itemView, ContactAdapter adaptor ) {
            super(itemView);
            mNameTextView = (TextView)
                    itemView.findViewById(R.id.list_item_contact_name);
            mPhotoView = (ImageView)
                    itemView.findViewById(R.id.list_item_contact_photo);
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

            itemView.setOnClickListener(this);
            mAdapter=adaptor;
        }

        public void bindContact(Contact contact) {
            mContact = contact;
            mNameTextView.setText(mContact.getName());
            updatePhotoView();

        }
        private void updatePhotoView()
        {
            if(imageViewWidth!=0 && imageViewHeight!=0) {
                File mPhotoFile = ContactLab.get(getActivity()).getPhotoFile(mContact);
                if (mPhotoFile == null || !mPhotoFile.exists()) {
                    String name = mContact.getName();
                    if (name != null && !name.isEmpty()) {
                        String initials = String.valueOf(name.charAt(0));
                        //String char2 = String.valueOf(name.substring(name.indexOf(' ') + 1).charAt(0));
                        //if(char2!=null && !char2.isEmpty())
                        // initials = initials + char2;
                        Bitmap bitmap = PictureUtils.generateCircleBitmap(getContext(),
                                PictureUtils.getMaterialColor(name),
                                40, initials);
                        // + String.valueOf())
                        mPhotoView.setImageBitmap(bitmap);
                    } else
                        mPhotoView.setImageDrawable(null);

                } else {
                    mPhotoView.setImageBitmap(PictureUtils.getCircularBitmap(mPhotoFile.getPath(), 40,imageViewWidth, imageViewHeight));
                }
            }

        }

        @Override
        public void onClick(View v) {

            mAdapter.setPosition(getAdapterPosition());
            Intent intent = ContactViewActivity.newIntent(getActivity(), mContact.getId());
            startActivity(intent);
        }
    }

    private class ContactAdapter extends RecyclerView.Adapter<ContactHolder> {

        private List<Contact> mContacts;

        private int position;

        public ContactAdapter(List<Contact> contacts) {
            mContacts = contacts;
        }

        @Override
        public ContactHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater
                    .inflate(R.layout.list_item_contact, parent, false);
            return new ContactHolder(view,this);
        }

        @Override
        public void onBindViewHolder(ContactHolder holder, int position) {
            Contact contact = mContacts.get(position);
            holder.bindContact(contact);
        }

        @Override
        public int getItemCount() {
            return mContacts.size();
        }

        public void setContacts(List<Contact> contacts) {
            mContacts = contacts;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == ContactEditFragment.ADD_CONTACT) {

            String returnValue = data.getStringExtra(ContactEditFragment.RETURN_STATE);
            if(returnValue!=null) {
                if (returnValue.equals("0")){
                    Toast.makeText(getActivity(), "deleted",
                            Toast.LENGTH_SHORT).show();
                }
                else if (returnValue.equals("1")) {

                    Contact contact = (Contact) data.getSerializableExtra(ContactEditFragment.CONTACT_OBJECT);
                    if(contact!=null) {
                        Toast.makeText(getActivity(), "saved",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        updateUI();
    }

    private class ExportDatabaseCSVTask extends AsyncTask<String ,String, String> {
        //private final ProgressDialog dialog = new ProgressDialog(getActivity());
        @Override
        protected void onPreExecute() {
            //this.dialog.setMessage("Exporting database...");
            //this.dialog.show();
        }

        protected String doInBackground(final String... args){
            File exportDir = new File(Environment.getExternalStorageDirectory(), "");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            Date date = new Date() ;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss") ;

            File file = new File(exportDir, dateFormat.format(date) + ".csv");
            try {

                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));

                //data
                ContactLab contactLab = ContactLab.get(getActivity());
                List<Contact> contacts;
                List<String> phones;
                List<String> emails;
                contacts = contactLab.getContacts();
                int max_phones=0;
                int max_emails=0;

                //get max number of phone and emails in database
                for (int i = 0; i != contacts.size(); i++) {
                    phones = contacts.get(i).getPhones();
                    if(phones.size()>max_phones)
                        max_phones=phones.size();
                    emails = contacts.get(i).getEmails();
                    if(emails.size()>max_emails)
                        max_emails=emails.size();
                }

                //write headers
                ArrayList<String> header= new ArrayList<String>();
                header.add("Name");
                for (int j = 0; j != max_phones; j++) {
                    header.add("Phone "+ String.valueOf(j+1));
                }
                for (int j = 0; j != max_emails; j++) {
                    header.add("Email "+ String.valueOf(j+1));
                }

                String[] arr1 = header.toArray(new String[header.size()]);
                csvWrite.writeNext(arr1);

                //write data
                for (int i = 0; i != contacts.size(); i++) {

                    ArrayList<String> data= new ArrayList<String>();
                    data.add(contacts.get(i).getName());


                    phones = contacts.get(i).getPhones();
                    int j;
                    for (j = 0; j != phones.size(); j++) {
                        data.add(phones.get(j));
                    }
                    for (; j< max_phones; j++) {
                        data.add("");
                    }

                    emails = contacts.get(i).getEmails();
                    for (j = 0; j != emails.size(); j++) {
                        data.add(emails.get(j));
                    }
                    for (; j< max_emails; j++) {
                        data.add("");
                    }

                    String[] arr = data.toArray(new String[data.size()]);
                    csvWrite.writeNext(arr);

                }
                csvWrite.close();

                if(contacts.size()==0)
                    return "0";

                return "1";
            }
            catch (IOException e){
                Log.e("allonsy.contacts", e.getMessage(), e);
                return "2";
            }
        }

        @SuppressLint("NewApi")
        @Override
        protected void onPostExecute(final String success) {

            /*if (this.dialog.isShowing()){
                this.dialog.dismiss();
            }*/
            if (success!=null && success.equals("0")){
                Toast.makeText(getActivity(), "No Contacts Saved", Toast.LENGTH_SHORT).show();
            }
            else if (success!=null && success.equals("1")){
                Toast.makeText(getActivity(), "Export successful!", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getActivity(), "Export failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public static boolean checkPermissions(Context context) {
        int result;

        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(context,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    public void askPermissions() {
        int result;
        Toast.makeText(getActivity(), "Please grant permissions on next screen", Toast.LENGTH_SHORT).show();
        try {Thread.sleep(1000);} catch (Exception e) {Log.e(TAG, e.getMessage());}

        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(getActivity(),p);
            if (result != PackageManager.PERMISSION_GRANTED) {
               requestPermissions(new String[]{p}, 0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! continue

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getActivity(), "Permissions Not Granted", Toast.LENGTH_SHORT).show();
                    try {Thread.sleep(1000);} catch (Exception e) {Log.e(TAG, e.getMessage());}
                    //callback.finishActivity();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


}
