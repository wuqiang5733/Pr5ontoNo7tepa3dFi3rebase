package com.okason.prontonotepadfirebase.listeners;


import com.okason.prontonotepadfirebase.model.Note;

/**
 * Created by Valentine on 3/12/2016.
 */
public interface NoteItemListener {

    void onNoteClick(Note clickedNote);

    void onDeleteButtonClicked(Note clickedNote);
}
