package com.okason.prontonotepadfirebase.ui.addNote;


import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.okason.prontonotepadfirebase.ui.notes.NoteListActivity;
import com.okason.prontonotepadfirebase.R;
import com.okason.prontonotepadfirebase.listeners.OnCategorySelectedListener;
import com.okason.prontonotepadfirebase.model.Category;
import com.okason.prontonotepadfirebase.model.Note;
import com.okason.prontonotepadfirebase.ui.category.SelectCategoryDialogFragment;
import com.okason.prontonotepadfirebase.ui.sketch.SketchActivity;
import com.okason.prontonotepadfirebase.util.Constants;
import com.okason.prontonotepadfirebase.util.FileUtils;
import com.okason.prontonotepadfirebase.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A simple {@link Fragment} subclass.
 */
public class NoteEditorFragment extends Fragment {

    private boolean isInEditMode;
    private Note mCurrentNote = null;
    private Category mCurrentCategory = null;
    private View mRootView;
    private Toolbar mToolbarBottom;

    private List<Category> mCategories;
    private SelectCategoryDialogFragment selectCategoryDialog;
    private final static String LOG_TAG = "NoteEditorFragment";


    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mFirebaseStorageReference;
    private StorageReference mAttachmentStorageReference;

    private DatabaseReference mDatabase;
    private DatabaseReference noteCloudReference;
    private DatabaseReference categoryCloudReference;

    private final int EXTERNAL_PERMISSION_REQUEST = 1;
    private final int RECORD_AUDIO_PERMISSION_REQUEST = 2;
    private final int IMAGE_CAPTURE_REQUEST = 3;
    private final int SKETCH_CAPTURE_REQUEST = 4;


    @BindView(R.id.edit_text_category) EditText mCategory;
    @BindView(R.id.edit_text_title) EditText mTitle;
    @BindView(R.id.edit_text_note) EditText mContent;
    @BindView(R.id.image_attachment) ImageView mImageAttachment;
    @BindView(R.id.sketch_attachment) ImageView mSketchAttachment;

    private MediaRecorder mRecorder = null;
    private MediaPlayer   mPlayer = null;

    private String mLocalAudioFilePath = null;
    private boolean audioUploadedToCloud = false;
    private String mLocalImagePath = null;
    private boolean imageUploadedToCloud = false;
    private String mLocalSketchPath = null;
    private boolean sketchUploadedToCloud = false;
    private Calendar mReminderTime;
    private boolean showLinedEditor = false;


    private Uri mImageURI = null;



    public NoteEditorFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getCurrentNote();
        setHasOptionsMenu(true);
        showLinedEditor = PreferenceManager
                .getDefaultSharedPreferences(getContext()).getBoolean("default_editor", false);
        Log.d(LOG_TAG, "Line Editor Enabled ?: " + showLinedEditor);
    }

    public static NoteEditorFragment newInstance(String content){
        NoteEditorFragment fragment = new NoteEditorFragment();
        if (!TextUtils.isEmpty(content)){
            Bundle args = new Bundle();
            args.putString(Constants.SERIALIZED_NOTE, content);
            fragment.setArguments(args);
        }
        return fragment;
    }

    public void getCurrentNote(){
        Bundle args = getArguments();
        if (args != null && args.containsKey(Constants.SERIALIZED_NOTE)){
            String serializedNote = args.getString(Constants.SERIALIZED_NOTE, "");
            if (!serializedNote.isEmpty()){
                Gson gson = new Gson();
                mCurrentNote = gson.fromJson(serializedNote, new TypeToken<Note>(){}.getType());
                if (mCurrentNote != null & !TextUtils.isEmpty(mCurrentNote.getNoteId())){
                    isInEditMode = true;
                }
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (showLinedEditor){
            mRootView = inflater.inflate(R.layout.fragment_lined_editor, container, false);
        }else {
            mRootView = inflater.inflate(R.layout.fragment_plain_editor, container, false);
        }
        ButterKnife.bind(this, mRootView);

        mCategories = new ArrayList<>();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseStorageReference = mFirebaseStorage.getReferenceFromUrl(Constants.FIREBASE_STORAGE_BUCKET);
        mAttachmentStorageReference = mFirebaseStorageReference.child("users/" + mFirebaseUser.getUid() + "/attachments");

        noteCloudReference =  mDatabase.child(Constants.USERS_CLOUD_END_POINT + mFirebaseUser.getUid() + Constants.NOTE_CLOUD_END_POINT);
        categoryCloudReference =  mDatabase.child(Constants.USERS_CLOUD_END_POINT + mFirebaseUser.getUid() + Constants.CATEGORY_CLOUD_END_POINT);


        categoryCloudReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null){
                    for (DataSnapshot categorySnapshot: dataSnapshot.getChildren()){
                        Category category = categorySnapshot.getValue(Category.class);
                        mCategories.add(category);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (isInEditMode){
            populateNote(mCurrentNote);
        }
    }

    public void populateNote(Note note) {
        mTitle.setText(note.getTitle());
        mTitle.setHint(R.string.placeholder_note_title);
        mContent.setText(note.getContent());
        mContent.setHint(R.string.placeholder_note_text);
        if (!TextUtils.isEmpty(note.getCategoryName())){
            mCategory.setText(note.getCategoryName());
        }else {
            mCategory.setText(Constants.DEFAULT_CATEGORY);
        }

        if (!TextUtils.isEmpty(note.getLocalAudioPath())){
            mLocalAudioFilePath = note.getLocalAudioPath();
            audioUploadedToCloud = note.isCloudAudioExists();
        }
        if (!TextUtils.isEmpty(note.getLocalImagePath())){
            mLocalImagePath = note.getLocalImagePath();
            imageUploadedToCloud = note.isCloudImageExists();
            populateImage(mLocalImagePath, imageUploadedToCloud);
        }
        if (!TextUtils.isEmpty(note.getLocalSketchImagePath())){
            mLocalSketchPath = note.getLocalSketchImagePath();
            sketchUploadedToCloud = note.isCloudSketchExists();
            populateSketch(mLocalImagePath, sketchUploadedToCloud);
        }

    }



    @OnClick(R.id.edit_text_category)
    public void showSelectCategory(){
        showChooseCategoryDialog(mCategories);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_note, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PackageManager packageManager = getActivity().getPackageManager();
        switch (item.getItemId()) {
            case R.id.action_save:
                if (validateContent()){
                    addNoteToFirebase("");
                }
                break;
            case R.id.action_delete:
                if (isInEditMode && mCurrentNote != null) {
                    promptForDelete(mCurrentNote);
                } else {
                    promptForDiscard();
                }
                break;
            case R.id.action_share:
                displayShareIntent();
                break;
            case R.id.action_camera:
                //show camera intent
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                    if (isStoragePermissionGranted()) {
                        if (isRecordPermissionGranted()) {
                            takePhoto();
                        }
                    }
                } else {
                    makeToast(getContext().getString(R.string.error_no_camera));
                }
                break;
            case R.id.action_record:
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                    if (isStoragePermissionGranted()) {
                        if (isRecordPermissionGranted()) {
                            promptToStartRecording();
                        }
                    }
                } else {
                    makeToast(getContext().getString(R.string.error_no_mic));
                }
                break;
            case R.id.action_play:
                if (mLocalAudioFilePath == null) {
                    makeToast("No Recording found");
                } else {
                    startPlaying();
                }
                break;
            case R.id.action_sketch:
                if (isStoragePermissionGrantedForSketch()) {
                    Intent sketchIntent = new Intent(getActivity(), SketchActivity.class);
                    startActivityForResult(sketchIntent, SKETCH_CAPTURE_REQUEST);
                }
                break;
            case R.id.action_reminder:
                if (mCurrentNote == null){
                    makeToast("Save note before adding a reminder");
                }else {
                    showReminderDate();
                }
                break;
            case android.R.id.home:
                //The user clicked on back button, save the content only if
                //It is not empty.
                if (!TextUtils.isEmpty(mCategory.getText().toString())
                        && !TextUtils.isEmpty(mContent.getText().toString())
                        && !TextUtils.isEmpty(mTitle.getText().toString())){
                    addNoteToFirebase("");
                }

                break;


        }
        return super.onOptionsItemSelected(item);
    }




    private boolean validateContent() {

        String title = mTitle.getText().toString();
        if (TextUtils.isEmpty(title)) {
            mTitle.setError(getString(R.string.title_is_required));
            return false;
        }

        String content = mContent.getText().toString();
        if (TextUtils.isEmpty(content)) {
            mContent.setError(getString(R.string.note_is_required));
            return false;
        }

        return true;
    }

    private void addNoteToFirebase(String message) {
        if (mCurrentNote == null){
            mCurrentNote = new Note();
            String key = noteCloudReference.push().getKey();
            mCurrentNote.setNoteId(key);
        }

        if (mCurrentCategory != null) {
            mCurrentNote.setCategoryName(mCurrentCategory.getCategoryName());
            mCurrentNote.setCategoryId(mCurrentCategory.getCategoryId());
        }else {
            mCurrentNote.setCategoryName(Constants.DEFAULT_CATEGORY);
            mCurrentNote.setCategoryId(getCategoryId(Constants.DEFAULT_CATEGORY));
        }


        mCurrentNote.setContent(mContent.getText().toString());
        mCurrentNote.setTitle(mTitle.getText().toString());
        mCurrentNote.setDateModified(System.currentTimeMillis());
        mCurrentNote.setCloudAudioExists(audioUploadedToCloud);
        mCurrentNote.setCloudImageExists(imageUploadedToCloud);
        mCurrentNote.setCloudSketchExists(sketchUploadedToCloud);
        mCurrentNote.setLocalAudioPath(mLocalAudioFilePath);
        mCurrentNote.setLocalImagePath(mLocalImagePath);
        mCurrentNote.setLocalSketchImagePath(mLocalSketchPath);

        if (audioUploadedToCloud){
            mCurrentNote.setNoteType(Constants.NOTE_TYPE_AUDIO);
        }else if (imageUploadedToCloud){
            mCurrentNote.setNoteType(Constants.NOTE_TYPE_IMAGE);
        }

        noteCloudReference.child(mCurrentNote.getNoteId()).setValue(mCurrentNote);

        String result;
        if (TextUtils.isEmpty(message)) {
            result = isInEditMode ? "Note updated" : "Note added";
        } else {
            result = message;
        }
        makeToast(result);
       // startActivity(new Intent(getActivity(), NoteListActivity.class));

    }

    private String getCategoryId(String categoryName) {
        for (Category category: mCategories){
            if (!TextUtils.isEmpty(category.getCategoryId()) && category.getCategoryName().equals(categoryName)){
                return category.getCategoryId();
            }
        }
        return addCategoryToFirebase(categoryName);
    }


    private String addCategoryToFirebase(String category) {
        Category cat = new Category();
        cat.setCategoryName(category);
        String key = categoryCloudReference.push().getKey();
        cat.setCategoryId(key);
        categoryCloudReference.child(key).setValue(cat);
        return key;
    }



    private void makeToast(String message){
        Snackbar snackbar = Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG);

        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.primary));
        TextView tv = (TextView)snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        snackbar.show();
    }

    public void showChooseCategoryDialog(List<Category> categories) {
        selectCategoryDialog = SelectCategoryDialogFragment.newInstance();
        selectCategoryDialog.setCategories(categories);

        selectCategoryDialog.setCategorySelectedListener(new OnCategorySelectedListener() {
            @Override
            public void onCategorySelected(Category selectedCategory) {
                selectCategoryDialog.dismiss();
                mCategory.setText(selectedCategory.getCategoryName());
                mCurrentCategory = selectedCategory;
            }

            @Override
            public void onEditCategoryButtonClicked(Category selectedCategory) {

            }

            @Override
            public void onDeleteCategoryButtonClicked(Category selectedCategory) {

            }
        });
        selectCategoryDialog.show(getActivity().getFragmentManager(), "Dialog");
    }

    private void promptForDelete(Note note){
        String title = "Delete " + note.getTitle();
        String message =  "Are you sure you want to delete note " + note.getTitle() + "?";


        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = (View)inflater.inflate(R.layout.dialog_title, null);
        TextView titleText = (TextView)titleView.findViewById(R.id.text_view_dialog_title);
        titleText.setText(title);
        alertDialog.show();
        alertDialog.setCustomTitle(titleView);

        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getActivity(), NoteListActivity.class));
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    public void promptForDiscard(){
        String title = "Discard Note";
        String message =  "Are you sure you want to discard note ";


        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = (View)inflater.inflate(R.layout.dialog_title, null);
        TextView titleText = (TextView)titleView.findViewById(R.id.text_view_dialog_title);
        titleText.setText(title);
        alertDialog.setCustomTitle(titleView);

        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetFields();
                startActivity(new Intent(getActivity(), NoteListActivity.class));
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void resetFields() {
        mCategory.setText("");
        mTitle.setText("");
        mContent.setText("");
    }

    public void displayShareIntent() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mTitle.getText().toString());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, mContent.getText().toString());
        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));

    }


    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mLocalAudioFilePath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }



    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        File recordFile = FileUtils.getattachmentFileName(Constants.MIME_TYPE_AUDIO_EXT);
        mLocalAudioFilePath = recordFile.getAbsolutePath();
        mRecorder.setOutputFile(mLocalAudioFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioEncodingBitRate(96000);
        mRecorder.setAudioSamplingRate(44100);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
            makeToast("Unable to record " + e.getLocalizedMessage());
        }


    }


    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }

        makeToast("Recording added");


    }


    public void promptToStartRecording(){
        String title = getContext().getString(R.string.start_recording);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = (View)inflater.inflate(R.layout.dialog_title, null);
        TextView titleText = (TextView)titleView.findViewById(R.id.text_view_dialog_title);
        titleText.setText(title);
        alertDialog.setCustomTitle(titleView);


        alertDialog.setPositiveButton(getString(R.string.start), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startRecording();
                promptToStopRecording();
            }
        });
        alertDialog.setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    public void promptToStopRecording(){
        String title = getContext().getString(R.string.stop_recording);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View titleView = (View)inflater.inflate(R.layout.dialog_title, null);
        TextView titleText = (TextView)titleView.findViewById(R.id.text_view_dialog_title);
        titleText.setText(title);
        alertDialog.setCustomTitle(titleView);


        alertDialog.setPositiveButton(getString(R.string.stop), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopRecording();
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private boolean isStoragePermissionGrantedForSketch() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG,"Permission is granted");
                return true;
            } else {
                Log.v(LOG_TAG,"Permission is revoked");
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, SKETCH_CAPTURE_REQUEST);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(LOG_TAG,"Permission is granted  API < 23");
            return true;
        }
    }


    //Checks whether the user has granted the app permission to
    //access external storage
    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG,"Permission is granted");
                return true;
            } else {
                Log.v(LOG_TAG,"Permission is revoked");
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_PERMISSION_REQUEST);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(LOG_TAG,"Permission is granted  API < 23");
            return true;
        }
    }

    //Checks whether the user has granted the app permission to
    //access external storage
    private boolean isRecordPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_REQUEST);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isRecordPermissionGranted()) {
                        promptToStartRecording();
                    }
                } else {
                    //permission was denied, disable backup
                    makeToast("External Access Denied");
                }
                break;
            case RECORD_AUDIO_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted perform backup
                    promptToStartRecording();
                } else {
                    //permission was denied, disable backup
                    makeToast("Mic Access Denied");
                }
                break;
            case IMAGE_CAPTURE_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted perform backup
                    takePhoto();
                } else {
                    //permission was denied, disable backup
                    makeToast("External storage access denied");
                }
                break;
            case SKETCH_CAPTURE_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted perform backup
                    Intent sketchIntent = new Intent(getActivity(), SketchActivity.class);
                    startActivityForResult(sketchIntent, SKETCH_CAPTURE_REQUEST);
                } else {
                    //permission was denied, disable backup
                    makeToast("External storage access denied");
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK ){
            switch (requestCode){
                case IMAGE_CAPTURE_REQUEST:
                    addPhotoToGallery();
                    populateImage(mLocalImagePath, false);
                    uploadFileToCloud(mLocalImagePath, Constants.MIME_TYPE_IMAGE);
                    break;
                case SKETCH_CAPTURE_REQUEST:
                    String sketchFilePath = data.getData().toString();
                    if (!TextUtils.isEmpty(sketchFilePath)) {
                        populateSketch(sketchFilePath, false);
                        uploadFileToCloud(sketchFilePath, Constants.MIME_TYPE_SKETCH);
                    } else {
                        makeToast("Sketch is empty");
                    }
                    break;
            }
        }

    }

    private void addPhotoToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mLocalImagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.getActivity().sendBroadcast(mediaScanIntent);

    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = FileUtils.createImageFile();
            mLocalImagePath = photoFile.getAbsolutePath();
        } catch (IOException ex) {
            // Error occurred while creating the File
            makeToast("There was a problem saving the photo...");
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            Uri fileUri = Uri.fromFile(photoFile);
            mImageURI = fileUri;
            mLocalImagePath = fileUri.getPath();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageURI);
            startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST);
        };
    }


    private void populateImage(String profileImagePath, boolean isCloudImage) {
        mImageAttachment.setVisibility(View.VISIBLE);
        if (isCloudImage) {
            Uri fileToDownload = Uri.fromFile(new File(mLocalImagePath));
            StorageReference imageRef = mAttachmentStorageReference.child(fileToDownload.getLastPathSegment());
            Glide.with(getContext())
                    .using(new FirebaseImageLoader())
                    .load(imageRef)
                    .placeholder(R.drawable.default_image)
                    .centerCrop()
                    .into(mImageAttachment);

        }else {
            Glide.with(getContext())
                    .load(profileImagePath)
                    .placeholder(R.drawable.default_image)
                    .centerCrop()
                    .into(mImageAttachment);

        }
    }

    private void populateSketch(String sketchImagePath, boolean isCloudImage) {
        mSketchAttachment.setVisibility(View.VISIBLE);
        if (isCloudImage) {
            Uri fileToDownload = Uri.fromFile(new File(sketchImagePath));
            StorageReference imageRef = mAttachmentStorageReference.child(fileToDownload.getLastPathSegment());
            Glide.with(getContext())
                    .using(new FirebaseImageLoader())
                    .load(imageRef)
                    .placeholder(R.drawable.default_image)
                    .centerCrop()
                    .into(mSketchAttachment);

        }else {
            Glide.with(getContext())
                    .load(sketchImagePath)
                    .placeholder(R.drawable.default_image)
                    .centerCrop()
                    .into(mSketchAttachment);

        }
    }


    private void uploadFileToCloud(String filePath, final String fileType){
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(fileType)
                .build();

        Uri fileToUpload = Uri.fromFile(new File(filePath));

        final String fileName = fileToUpload.getLastPathSegment();

        StorageReference imageRef = mAttachmentStorageReference.child(fileName);

        UploadTask uploadTask = imageRef.putFile(fileToUpload, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                makeToast("Unable to upload file to cloud" + e.getLocalizedMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (fileType.equals(Constants.MIME_TYPE_AUDIO)){
                    audioUploadedToCloud = true;
                }else if (fileType.equals(Constants.MIME_TYPE_IMAGE)){
                    imageUploadedToCloud = true;
                }else if (fileType.equals(Constants.MIME_TYPE_SKETCH)){
                    sketchUploadedToCloud = true;
                }
                makeToast("File uploaded successfully");
            }
        });

    }




    private void setAlarm(){
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        Gson gson = new Gson();
        String serializedNote = gson.toJson(mCurrentNote);
        intent.putExtra(Constants.SERIALIZED_NOTE, serializedNote);

        PendingIntent alarmIntent = PendingIntent.getBroadcast(getContext(), 0 , intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, mReminderTime.getTimeInMillis(), alarmIntent);

        String selectedTime = TimeUtils.getDueDate(mReminderTime.getTimeInMillis());


        mCurrentNote.setNoteType(Constants.NOTE_TYPE_REMINDER);
        mCurrentNote.setNextReminder(mReminderTime.getTimeInMillis());
        addNoteToFirebase("Reminder set");


    }

    public void showReminderDate() {
        DialogFragment reminderDatePicker = new ReminderDatePickerDialogFragment();
        reminderDatePicker.setTargetFragment(NoteEditorFragment.this, 0);
        reminderDatePicker.show(getFragmentManager(), "reminderDatePicker");

    }


    public void showReminderTime() {
        DialogFragment reminderTimePicker = new ReminderTimePickerDialogFragment();
        reminderTimePicker.setTargetFragment(NoteEditorFragment.this, 0);
        reminderTimePicker.show(getFragmentManager(), "reminderTimePicker");

    }



    public static class ReminderDatePickerDialogFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener{

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            NoteEditorFragment targetFragment = (NoteEditorFragment)getTargetFragment();
            if (year < 0){
                targetFragment = null;
            } else {
                targetFragment.mReminderTime = Calendar.getInstance();
                targetFragment.mReminderTime.set(year, monthOfYear, dayOfMonth);
                targetFragment.showReminderTime();

            }

        }

    }

    public static class ReminderTimePickerDialogFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener{

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            NoteEditorFragment targetFragment = (NoteEditorFragment)getTargetFragment();
            final Calendar c = targetFragment.mReminderTime;
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            NoteEditorFragment targetFragment = (NoteEditorFragment)getTargetFragment();
            targetFragment.mReminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            targetFragment.mReminderTime.set(Calendar.MINUTE, minute);
            targetFragment.setAlarm();
        }

    }





}
