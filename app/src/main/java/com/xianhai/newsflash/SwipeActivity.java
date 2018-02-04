package com.xianhai.newsflash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class SwipeActivity extends AppCompatActivity {
    private static final String EMPTY = "";
    private static final int SEED = 69;
    private static final int NEWS_BATCH = 30;
    private static int[] backgroundColors = new int[] {0xFFA37A9D,0xFF884E89, 0xFF4D3159, 0xFF2C1B30, 0xFF5151A3};
    private ArrayList<Bitmap> images;
    private ArrayList<ArrayList<String>> newsInfo;
    private ArrayList<String> storedHeadlines;
    private HashMap<String, String> articles;
    private HashMap<String, String> keytermsMap;
    private ArrayList<String> keytermsList;
    private View decorView;
    private int apiKeyIndex = 0;

    private Typeface customFont;
    private TextView headlineTextView;
    private ImageButton savedBtn;
    private ImageButton likeBtn;
    private ImageButton dislikeBtn;
    private ImageButton randomBtn;
    private ImageButton linkBtn;
    private ImageButton saveBtn;
    private ImageButton returnButton;
    private ScrollView summaryScrollView;
    private TextView titleTextView;
    private TextView summaryTextView;
    private ProgressBar progressBar;
    private LinearLayout btnLayout;
    private ConstraintLayout bodyLayout;
    private ConstraintLayout listViewLayout;
    private SearchView searchView;
    private WebView webView;
    private Spinner keyWords;
    private ListView savedList;
    private ListView keyTermsListView;
    private Spinner keytermsSpinner;
    private TextView keytermsTextView;
    private ArrayAdapter<String> savedAdapter;
    private ArrayAdapter<String> keyTermsAdapter;

    private LinearLayout summaryLinearLayout;


    private Random random;
    private int lastColorSet;

    private boolean inWebView;
    boolean pageLoaded = false;
    private boolean inListView;
    private boolean savePressed;

    private boolean inSummaryMode;
    private boolean isLoading;
    private boolean summaryLoading;



    private String searchQuery;

    private HashMap<String, Integer> pageHistory;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        random = new Random(SEED);
        lastColorSet = 0;
        inWebView = false;
        inSummaryMode = false;
        isLoading = false;
        savePressed = false;
        summaryLoading = false;

        searchQuery = EMPTY;

        pageHistory = new HashMap<String, Integer>();
        images = new ArrayList<Bitmap>();
        newsInfo = new ArrayList<ArrayList<String>>();
        storedHeadlines = new ArrayList<String>();
        articles = new HashMap<String, String>();
        keytermsList = new ArrayList<String>();
        keytermsMap = new HashMap<String, String>();

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        likeBtn = (ImageButton) findViewById(R.id.likeBtn);
        customFont = Typeface.createFromAsset(getAssets(), "fonts/coolvetica_rg.ttf");
        headlineTextView = (TextView) findViewById(R.id.headlineTextView);
        dislikeBtn = (ImageButton) findViewById(R.id.dislikeBtn);
        summaryScrollView = (ScrollView) findViewById(R.id.summaryScrollView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        summaryTextView = (TextView) findViewById(R.id.summaryTextView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnLayout = (LinearLayout) findViewById(R.id.btnLayout);
        bodyLayout = (ConstraintLayout) findViewById(R.id.bodyLayout);
        linkBtn = (ImageButton) findViewById(R.id.linkBtn);
        saveBtn = (ImageButton) findViewById(R.id.saveBtn);
        savedBtn = (ImageButton) findViewById(R.id.savedBtn);
        randomBtn = (ImageButton) findViewById(R.id.randomBtn);
        searchView = (SearchView) findViewById(R.id.searchView);
        webView= (WebView)findViewById(R.id.WebView1);
        //keyWords = (Spinner)findViewById(R.id.spinner1);
        savedList = (ListView)findViewById(R.id.savedList);
        returnButton = (ImageButton) findViewById(R.id.returnBtn);
        savedList = (ListView)findViewById(R.id.savedList);
        //keyTermsListView = (ListView)findViewById(R.id.keyermsListView);
        keytermsSpinner = (Spinner)findViewById(R.id.keytermsSpinner);
        keytermsTextView = (TextView) findViewById(R.id.keytermsTextView);

        //headlineTextView.setTypeface(customFont);
        // write helper method for vis initialization
        summaryScrollView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        //keyWords.setVisibility(View.GONE);
        savedList.setVisibility(View.GONE);
        initializeBtnSwipe();
        //keyTermsListView.setVisibility(View.GONE);
        keytermsSpinner.setVisibility(View.GONE);

        webView.setWebViewClient(new MyBrowser());
        webView.getSettings().getLoadsImagesAutomatically();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);


        likeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateVisSummaryUnloaded();
                initializeBtnSummary();
                generateSummary();
            }
        });

        dislikeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                if(!isLoading) {
                    displayNextNews();
                    if (newsInfo.size() < 10) generateNews(NEWS_BATCH);
                }
            }
        });

        randomBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                clearNewsInfo();
                searchView.setQuery("", true);
                searchView.clearFocus();
                searchQuery = EMPTY;
                updateVisSwipeUnloaded();
                generateNews(NEWS_BATCH);
            }
        });

        linkBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                openWebView();
            }
        });

        savedBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                updateLoadSavedArticles();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                if(!savePressed) {
                    storedHeadlines.add(newsInfo.get(0).get(0));
                    articles.put(newsInfo.get(0).get(0), newsInfo.get(0).get(1));
                    savedAdapter.notifyDataSetChanged();
                    savePressed = true;
                }
            }
        });

        returnButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
               onBackPressed();
            }
        });

        savedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, storedHeadlines){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view =super.getView(position, convertView, parent);

                TextView textView=(TextView) view.findViewById(android.R.id.text1);

            /*YOUR CHOICE OF COLOR*/
                textView.setTextColor(Color.WHITE);

                return view;
            }
        };
        savedList.setAdapter(savedAdapter);

        savedList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3)
            {
                String value = (String)adapter.getItemAtPosition(position);
                webView.loadUrl(articles.get(value));
                updateVisWebViewLoaded();
                inWebView = true;

                // assuming string and if you want to get the value on click of list item

            }
        });

        keyTermsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, keytermsList);
        keytermsSpinner.setAdapter(keyTermsAdapter);


        keytermsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
            {
                String value = (String)parentView.getItemAtPosition(position);
                if(value!= "Choose term to view") {
                    webView.loadUrl(keytermsMap.get(value));
                    updateVisWebViewLoaded();
                    inWebView = true;
                }

                // assuming string and if you want to get the value on click of list item

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                clearNewsInfo();
                query = processQuery(query);
                searchQuery = query;
                updateVisSwipeUnloaded();
                generateNews(NEWS_BATCH);
                if (searchView != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        bodyLayout.setOnTouchListener(new OnSwipeTouchListener(SwipeActivity.this) {
            public void onSwipeRight() {
                updateVisSummaryUnloaded();
                initializeBtnSummary();
                generateSummary();
            }

            public void onSwipeLeft() {
                if(!isLoading) {
                    displayNextNews();
                    if (newsInfo.size() < 10) generateNews(NEWS_BATCH);
                }
            }

            public void onSwipeTop() {

            }

            public void onSwipeBottom() {
            }
        });

        generateNews(NEWS_BATCH);
        displayNextNews();
        //makeRequest();
    }

    @Override
    public void onBackPressed() {
        if (inSummaryMode) {
            displayNextNews();
            updateVisSwipeLoaded();
            initializeBtnSwipe();
            webView.loadUrl("about:blank");
            pageLoaded = false;
            savePressed = false;
        }
        else if(inWebView){
            webView.loadUrl("about:blank");
            if(inListView){
                updateLoadSavedArticles();
            }
            else {
                updateVisSummaryLoaded();
            }
        }
        else if(inListView){
            updateVisSwipeLoaded();
            initializeBtnSwipe();
            inListView = false;
        }
    }

    private void updateVisSwipeLoaded() {
        progressBar.setVisibility(View.GONE);
        summaryScrollView.setVisibility(View.GONE);
        btnLayout.setVisibility(View.VISIBLE);
        headlineTextView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        savedList.setVisibility(View.GONE);
        inSummaryMode = false;
        isLoading = false;
        inWebView = false;
    }

    private void updateVisSwipeUnloaded() {
        progressBar.setVisibility(View.VISIBLE);
        summaryScrollView.setVisibility(View.GONE);
        btnLayout.setVisibility(View.VISIBLE);
        headlineTextView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        savedList.setVisibility(View.GONE);
        inSummaryMode = false;
        isLoading = true;
        inWebView = false;
    }

    private void updateVisSummaryLoaded() {
        progressBar.setVisibility(View.GONE);
        summaryScrollView.setVisibility(View.VISIBLE);
        btnLayout.setVisibility(View.GONE);
        headlineTextView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        savedList.setVisibility(View.GONE);
        //keyTermsListView.setVisibility(View.VISIBLE);
        keytermsSpinner.setVisibility(View.VISIBLE);
        inSummaryMode = true;
        isLoading = false;
        inWebView = false;
    }

    private void updateVisSummaryUnloaded() {
        progressBar.setVisibility(View.VISIBLE);
        summaryScrollView.setVisibility(View.GONE);
        btnLayout.setVisibility(View.GONE);
        headlineTextView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        savedList.setVisibility(View.GONE);
        inSummaryMode = false;
        isLoading = true;
        inWebView = false;
    }

    private void updateVisWebViewLoaded(){
        progressBar.setVisibility(View.GONE);
        summaryScrollView.setVisibility(View.GONE);
        btnLayout.setVisibility(View.GONE);
        headlineTextView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        savedList.setVisibility(View.GONE);
        inSummaryMode = false;
        isLoading = false;
        inWebView = true;
        pageLoaded = true;
    }
    private void updateLoadSavedArticles(){
        progressBar.setVisibility(View.GONE);
        summaryScrollView.setVisibility(View.GONE);
        btnLayout.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.GONE);
        returnButton.setVisibility(View.VISIBLE);
        headlineTextView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        savedList.setVisibility(View.VISIBLE);
        inSummaryMode = false;
        isLoading = false;
        inWebView = false;
        pageLoaded = false;
        inListView = true;
    }

    private void displayNextNews() {
        if (newsInfo.size() != 0) {
            newsInfo.remove(0);
            headlineTextView.setText(newsInfo.get(0).get(0));
            changeColorSet();
        }
        else {
            updateVisSwipeUnloaded();
        }
    }

    private void changeColorSet() {
        int rand;
        do {
            rand = random.nextInt(5);
        } while (rand == lastColorSet);
        System.err.println(rand);
        decorView.setBackgroundColor(backgroundColors[rand]);
        headlineTextView.setTextColor(0xFFFFFFFF);
        summaryTextView.setTextColor(0xFFFFFFFF);
        titleTextView.setTextColor(0xFFFFFFFF);
        keytermsTextView.setTextColor(0xFFFFFFFF);
        lastColorSet = rand;
    }

    private static String constructURL(String API, ArrayList<ArrayList<String>> params) {
        String url = API + "?";
        for (int i = 0; i < params.size(); i++) {
            if (i != 0) url += "&";
            url += params.get(i).get(0) + "=" + params.get(i).get(1);
        }
        return url;
    }

    private void generateNews(int toRetrieve) {

        RequestQueue queue = Volley.newRequestQueue(this);
        ArrayList<ArrayList<String>> params = new ArrayList<ArrayList<String>>();
        String API = "https://newsapi.org/v2";
        int page = 1;
        if (pageHistory.containsKey(searchQuery)) {
            pageHistory.put(searchQuery, pageHistory.get(searchQuery).intValue() + 1);
            page = pageHistory.get(searchQuery).intValue();
        }
        else {
            pageHistory.put(searchQuery, page);
        }

        params.add(new ArrayList<String>(Arrays.asList("page", Integer.toString(page))));
        params.add(new ArrayList<String>(Arrays.asList("pageSize", Integer.toString(toRetrieve))));
        params.add(new ArrayList<String>(Arrays.asList("apiKey", Config.NEWS_API_KEY)));
        if (searchQuery == null || searchQuery == "") {
            //params.add(new ArrayList<String>(Arrays.asList("sources", "google-news")));
            params.add(new ArrayList<String>(Arrays.asList("sources", "cnn,cbc-news,abc-news,google-news")));
            //API += "/top-headlines";
        }
        else {
            params.add(new ArrayList<String>(Arrays.asList("q", searchQuery)));
        }
        params.add(new ArrayList<String>(Arrays.asList("language", "en")));
        params.add(new ArrayList<String>(Arrays.asList("sortBy", "popularity")));
        API += "/everything";

        String url = constructURL(API, params);
        System.err.println(url);
        StringRequestRetry stringRequest = new StringRequestRetry(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject data = new JSONObject(response);
                            JSONArray articles = data.getJSONArray("articles");

                            for (int i = 0; i < articles.length(); i++) {
                                JSONObject article = articles.getJSONObject(i);
                                ArrayList<String> info = new ArrayList<String >();
                                info.add(article.getString("title"));
                                info.add(article.getString("url"));
                                info.add(article.getString("urlToImage"));
				info.add(article.getString("description"));
                                newsInfo.add(info);
                            }
                            if (isLoading) displayNextNews();
                            updateVisSwipeLoaded();

                            System.err.println("Success: news retrieval");
                            System.err.println(response);
                        } catch (JSONException e) {
                            System.err.println(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.err.println("Fail: news retrieval");
                System.err.println(error);
            }
        });
        queue.add(stringRequest);
    }
    private void generateSummary() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.aylien.com/api/v1/summarize";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject data = new JSONObject(response);
                            JSONArray sentences = data.getJSONArray("sentences");
                            String content = "";
                            for (int i = 0; i < sentences.length(); i++) {
                                content += sentences.getString(i);
                            }
                            summaryTextView.setText(content);
                            titleTextView.setText(newsInfo.get(0).get(0));
                            updateVisSummaryLoaded();
                        } catch (JSONException e) {
                            System.err.println(e);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        System.err.println(error);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                HashMap<String, String> params = new HashMap<>();
                params.put("url", newsInfo.get(0).get(1));
                params.put("sentences_number", Config.SENT_LENGTH);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                String key = Config.AYLIEN_API_KEY.get(apiKeyIndex);
                String id = Config.AYLIEN_ID.get(apiKeyIndex);
                apiKeyIndex = ++apiKeyIndex % Config.AYLIEN_API_KEY.size();
                headers.put("X-AYLIEN-TextAPI-Application-Key", key);
                headers.put("X-AYLIEN-TextAPI-Application-ID", id);

                return headers;
            }
        };
        queue.add(postRequest);

        generateKeywords();
    }


    private void generateKeywords() {
        keytermsList.clear();
        keytermsMap.clear();
        keytermsList.add("Choose term to view");

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.aylien.com/api/v1/concepts";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject data = new JSONObject(response);
                            JSONObject concepts = data.getJSONObject("concepts");
                            Iterator<?> keys = concepts.keys();
                            String content = "";
                            int index = 0;
                            while (keys.hasNext() && index < Config.KEYWORD_COUNT) {
                                String key = (String) keys.next();
                                if (concepts.get(key) instanceof JSONObject) {
                                    JSONArray surfaceForms = ((JSONObject) concepts.get(key)).getJSONArray("surfaceForms");
                                    String keyterm = ((JSONObject) surfaceForms.get(0)).getString("string");
                                    keytermsList.add(keyterm);
                                    keyTermsAdapter.notifyDataSetChanged();
                                    keytermsMap.put(keyterm, key);
                                }
                                index++;
                            }
                            keyTermsAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            System.err.println(e);
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        System.err.println(error);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                HashMap<String, String> params = new HashMap<>();
                params.put("url", newsInfo.get(0).get(1));

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("X-AYLIEN-TextAPI-Application-Key", "bd22e5376ee81016f78c0768f4601dd4");
                headers.put("X-AYLIEN-TextAPI-Application-ID", "c5bf3e34");

                return headers;
            }
        };
        queue.add(postRequest);
    }



    private void openWebView(){

        //open browser inside your app
        String url = newsInfo.get(0).get(1);
        if(!pageLoaded) webView.loadUrl(url);
        updateVisWebViewLoaded();
        inWebView = true;
    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            view.loadUrl(url);
            return true;
        }
    }

    private void clearNewsInfo() {
        newsInfo.clear();
    }

    private static String processQuery(String query) {
        return query.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }


    private void initializeBtnSwipe() {
        likeBtn.setVisibility(View.VISIBLE);
        dislikeBtn.setVisibility(View.VISIBLE);
        randomBtn.setVisibility(View.VISIBLE);
        savedBtn.setVisibility(View.VISIBLE);
        searchView.setVisibility(View.VISIBLE);
        linkBtn.setVisibility(View.GONE);
        saveBtn.setVisibility(View.GONE);
        returnButton.setVisibility(View.GONE);
    }
    private void initializeBtnSummary() {
        likeBtn.setVisibility(View.GONE);
        dislikeBtn.setVisibility(View.GONE);
        randomBtn.setVisibility(View.GONE);
        savedBtn.setVisibility(View.GONE);
        searchView.setVisibility(View.GONE);
        linkBtn.setVisibility(View.VISIBLE);
        saveBtn.setVisibility(View.VISIBLE);
        returnButton.setVisibility(View.VISIBLE);
    }
}
