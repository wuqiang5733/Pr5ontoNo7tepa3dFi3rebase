package com.okason.prontonotepadfirebase.util;

/**
 * Created by vokafor on 12/23/2016.
 */

public class Constants {
    public final static int NOTES = 1;
    public final static int CATEGORIES = 2;
    public final static int SETTINGS = 3;
    public final static int LOGOUT = 4;
    public final static int DELETE = 5;


    public final static String NOTE_TYPE_TEXT = "text";
    public final static String NOTE_TYPE_IMAGE = "image";
    public final static String NOTE_TYPE_AUDIO = "audio";
    public final static String NOTE_TYPE_REMINDER = "reminder";


    public static final String NOTE_ID = "note_id";


    public static final String FIRST_RUN = "first_run";
    public static final String SERIALIZED_CATEGORY = "serialized_category";
    public static final String DEFAULT_CATEGORY = "General";

    public static final String NOTE_CLOUD_END_POINT = "/notes";
    public static final String CATEGORY_CLOUD_END_POINT = "/categories";
    public static final String ATTACHMENT_CLOUD_END_POINT = "/attachments";
    public static final String NOTE_ATTACHMENT_CLOUD_END_POINT = "/note_attachments";
    public static final String USERS_CLOUD_END_POINT = "/users/";
    public static final String SERIALZED_CATEGORY = "serialized_category";

    public final static String SELECTED_CATEGORY_ID = "selected_category_id";
    public static final String SERIALIZED_NOTE = "serialized_note";
    public static final String ATTACHMENTS_FOLDER = "ProntoNote/Attachments";


    public final static String MIME_TYPE_IMAGE = "image/jpeg";
    public final static String MIME_TYPE_AUDIO = "audio/amr";
    public final static String MIME_TYPE_SKETCH = "image/png";


    public final static String MIME_TYPE_IMAGE_EXT = ".jpeg";
    public final static String MIME_TYPE_AUDIO_EXT = ".amr";
    public final static String MIME_TYPE_SKETCH_EXT = ".png";


    public static final String FIREBASE_STORAGE_BUCKET = "gs://prontonotepad.appspot.com";
    public static final String IS_DUAL_SCREEN = "is_dual_screen";
    public static final String SKETCH_PATH = "sketch_path";
}
