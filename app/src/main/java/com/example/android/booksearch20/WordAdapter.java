package com.example.android.booksearch20;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Justin on 1/25/2017.
 */
public class WordAdapter extends ArrayAdapter<Word>{

    public WordAdapter(Activity context, List<Word> books) {
        // Here, we initialize the ArrayAdapter's internal storage for the context and the list.
        // the second argument is used when the ArrayAdapter is populating a single TextView.
        // Because this is a custom adapter for two TextViews and an ImageView, the adapter is not
        // going to use this second argument, so it can be any value. Here, we used 0.
        // Pass a zero for the resource Id because we are inflating the view ourselves with the getView Method
        super(context, 0, books);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        // This method call is used to recycle the view when ever it is not being used
        // The list item view is commonly null, so the statement is used to help prevent any errors
        View listItemView = convertView;
        // Used to inflate the listItem if there was nothing there
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        // Get the {@link Word} object located at this position in the list
        Word currentWord = getItem(position);

//------------------------------- HOW TO FORMAT THE MAGNITUDE---------------------------------------
        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView authorText = (TextView) listItemView.findViewById(R.id.author);
        authorText.setText("Author(s): " + currentWord.getmAuthor());

        TextView titleText = (TextView) listItemView.findViewById(R.id.title);
        titleText.setText("Title: " + currentWord.getmTitle());

        return listItemView;
    }
}
