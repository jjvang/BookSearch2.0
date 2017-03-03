package com.example.android.booksearch20;

import android.widget.ImageView;

/**
 * Created by Justin on 1/25/2017.
 */
public class Word {

    private String mAuthor;
    private String mTitle;

    public Word(String mAuthor, String mTitle) {
        this.mAuthor = mAuthor;
        this.mTitle = mTitle;
    }

    public String getmAuthor() {
        return mAuthor;
    }

    public String getmTitle() {
        return mTitle;
    }


}
