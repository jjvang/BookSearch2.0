package com.example.android.booksearch20;

import android.app.SearchManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

// -----------------------------------GLOBAL VARIABLES----------------------------------------------
private static final String LOG_TAG = MainActivity.class.getName();
private TextView mEmptyStateTextView;
// Variable string to give the base google API search since the user can search multiple query or topics
private static final String  GOOGLE_BOOKS_REQUEST_SEARCh_URL = "https://www.googleapis.com/books/v1/volumes?maxResults=20&q=";
// Variable which will be the new and used url to GET info from the searched results
private String urlQuery = "";
// Global Variable since it will need to be changed globally
private ListView listView;
private WordAdapter mAdapter;
private List savedBooks;

// -----------------------------------UPDATE UI METHOD----------------------------------------------
    private void updateUi(List<Word> bookList) {
        mAdapter.clear();
        savedBooks = bookList;
        mAdapter.addAll(bookList);
    }
// -----------------------------------onCREATE METHOD-----------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize listView
        listView = (ListView) findViewById(R.id.list);
        // Create a new adapter that takes an empty list of Word as input
        mAdapter = new WordAdapter(this, new ArrayList<Word>());
        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        listView.setAdapter(mAdapter);

        // USED TO INITIALIZE THE ERROR TEXT VIEW WHEN NO DATA OCCURS
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(mEmptyStateTextView);

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // Put in text to tell user to search by clicking search icon on top right
        mEmptyStateTextView.setText(R.string.search_me);
        // Initialize and removes loading indicator since nothing is loading yet
        View loadingIndicator = findViewById(R.id.loading_indicator);
        loadingIndicator.setVisibility(View.GONE);

        if (networkInfo != null && networkInfo.isConnected()) {
            mAdapter.clear();
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_internet_connection);
        }
    }
// ----------------------onSaveInstanceState & onRestoreInstanceState-------------------------------
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mAdapter.clear();
            savedBooks = savedInstanceState.getParcelableArrayList("books");
            mAdapter.addAll(savedBooks);
        }
    }

    @Override
    public void onSaveInstanceState (Bundle outstate){
        outstate.putParcelableArrayList("books", (ArrayList<? extends Parcelable>) savedBooks);
        super.onSaveInstanceState(outstate);
    }
// -----------------USED TO INITIALIZE AND CREATE THE SEARCH IMAGE AND FUNCTIONS--------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_menu, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Full manual mode, can get callbacks and search to know exactly what happens
        // OnQueryTextListener occurs once the user submits a search
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        // This does not need to do anything since the user isn't really trying to get existing data
                        // Returning false is good so there is no change
                        return false;
                    }

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        // Get a reference to the ConnectivityManager to check state of network connectivity
                        ConnectivityManager connMgr = (ConnectivityManager)
                                getSystemService(Context.CONNECTIVITY_SERVICE);

                        // Get details on the currently active default data network
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

                        // If there is a network connection, fetch data
                        if (networkInfo != null && networkInfo.isConnected()) {
                            // Get a reference to the LoaderManager, in order to interact with loaders.
                            // Text submit already is saved into the query
                            String encodedQuery = "";
                            try {
                                encodedQuery = URLEncoder.encode(query, "utf-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            urlQuery = GOOGLE_BOOKS_REQUEST_SEARCh_URL + encodedQuery + "&maxResults=10";

                            // This initialize and execute the AsyncTask to make internet connection and display updated list view
                            BookAsyncTask task = new BookAsyncTask();
                            task.execute();

                            mEmptyStateTextView.setVisibility(View.GONE);

                        // If there is no data, display no connection gracefully
                        } else {
                            // Otherwise, display error
                            // First, hide loading indicator so error message will be visible
                            View loadingIndicator = findViewById(R.id.loading_indicator);
                            loadingIndicator.setVisibility(View.GONE);

                            // Update empty state with no connection error message
                            mAdapter.clear();
                            mEmptyStateTextView.setText(R.string.no_internet_connection);
                        }

                        return true;
                    }
                }
        );

        return true;
    }
// -----------------------------CREATE PUBLIC ASYNC TASK -------------------------------------------

    private class BookAsyncTask extends AsyncTask<URL, Void, List<Word>> {

//-----------------------PUBLIC METHOD TO ASSIGN QUERY TO URL---------------------------------------
        @Override
        protected List<Word> doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(urlQuery);
            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            List<Word> BooksList = extractFeatureFromJson(jsonResponse);

            // Return the {@link Event} object as the result fo the {@link BookAsyncTask}
            return BooksList;
        }

        /**
         * Update the screen with the given earthquake (which was the result of the
         * {@link BookAsyncTask}).
         */
        @Override
        protected void onPostExecute(List<Word> book) {
            if (book == null) {
                return;
            }
            // Method to initialize when rotated and when AsyncTask is called
            updateUi(book);
        }
//-------------------------BUILD THE URL OBJECT-----------------------------------------------------
        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }
//-------------------------PERFORM NETWORK REQUEST--------------------------------------------------
        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            if (url == null) {
                return jsonResponse;
            }
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();

                // Check if the jSon Response is good via "200"
                // if the jSon response is error, it does not GET any info
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error Response Code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error getting the jSON Response, possible connection error", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }
//-----------------------CONVERT INPUT-STREAM TO STRING --------------------------------------------
        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

//-------------------------PARSE JSON DATA----------------------------------------------------------
        // We parse the JSON data to GET the specific data for the API
        // We Modify the JSON Parsing method to a list of books from the web server response
        /**
         * Return a list of {@link Word} objects that has been built up from
         * parsing the given JSON response.
         */

        private List<Word> extractFeatureFromJson(String booksJSON) {
            // If the JSON string is empty or null, then return early.
            if (TextUtils.isEmpty(booksJSON)) {
                return null;
            }

            // Create an empty ArrayList that we can start adding books to
            List<Word> bookDetails = new ArrayList<>();

            // Try to parse the JSON response string. If there's a problem with the way the JSON
            // is formatted, a JSONException exception object will be thrown.
            // Catch the exception so the app doesn't crash, and print the error message to the logs.
            try {

                // Create a JSONObject from the JSON response string
                JSONObject baseJsonResponse = new JSONObject(booksJSON);

                // Extract the JSONArray associated with the key called "items",
                JSONArray itemsArray = baseJsonResponse.getJSONArray("items");

                // For each book in the itemsArray, repeat till maximum
                for (int i = 0; i < itemsArray.length(); i++) {

                    // Get a single book at position i within the list of books
                    JSONObject currentDetail = itemsArray.getJSONObject(i);

                    // For a given book, extract the JSONObject associated with the
                    // key called "volumeInfo", which represents a list of all properties
                    // for that book.
                    JSONObject properties = currentDetail.getJSONObject("volumeInfo");

                    // Extract the value for the book title called "title"
                    String title = properties.getString("title");

                    // Initialize authors as a string variable
                    String authors = "";

                    // If info does not have authors, does not proceed to prase information
                    if (properties.has("authors")) {
                        JSONArray authorsArray = properties.getJSONArray("authors");

                        for (int ii = 0; ii < authorsArray.length(); ii++) {
                            authors += authorsArray.getString(ii);
                        }
                    }

                    // Create a new {@link Word} object with the title and author
                    Word details = new Word(authors, title);

                    // Add the new {@link Word} to the list of books.
                    bookDetails.add(details);
                }

            } catch (JSONException e) {
                // If an error is thrown when executing any of the above statements in the "try" block,
                // catch the exception here, so the app doesn't crash. Print a log message
                // with the message from the exception.
                Log.e("QueryUtils", "Problem parsing the book JSON results", e);
            }

            // Return the list of bookDetails
            return bookDetails;
        }

    }
}
