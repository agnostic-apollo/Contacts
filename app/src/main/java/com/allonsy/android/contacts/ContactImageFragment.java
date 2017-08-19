package com.allonsy.android.contacts;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;


public class ContactImageFragment extends DialogFragment {

    private static final String ARG_IMAGE = "image";
    private ImageView mPhotoView;
    String path;
    public static final String EXTRA_DATE =
            "com.bignerdranch.android.criminalintent.date";

    public static ContactImageFragment newInstance(String path) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_IMAGE, path);
        ContactImageFragment fragment = new ContactImageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        path = (String) getArguments().getSerializable(ARG_IMAGE);


        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_image, null);



        mPhotoView = (ImageView) v.findViewById(R.id.dialog_image_image_view);
        Bitmap bitmap = PictureUtils.getScaledBitmap(
                path,getActivity());
        mPhotoView.setImageBitmap(bitmap);


        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.contact_image_title)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}

