package com.xianhai.newsflash;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class SwipeActivity extends AppCompatActivity {
    private static final String EMPTY = "food";
    private static final int SEED = 69;
    private static final int NEWS_BATCH = 30;
    private static int[] backgroundColors = new int[] {0xFFA37A9D,0xFF884E89, 0xFF4D3159, 0xFF2C1B30, 0xFF5151A3};
    private static int[] textColors = new int[] {0xFFFFFFFF,0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
    private ArrayList<Bitmap> images;
    private ArrayList<ArrayList<String>> newsInfo;
    private ArrayList<String> storedHeadlines;
    private HashMap<String, String> articles;
    private View decorView;
    private ImageView backgroundView;
    private int apiKeyIndex = 0;

    private Typeface customFont;
    private TextView headlineTextView;
    private ImageButton likeBtn;
    private ImageButton dislikeBtn;
    private Button linkBtn;
    private Button saveArticleBtn;
    private ImageButton savedBtn;
    private ScrollView summaryScrollView;
    private TextView titleTextView;
    private TextView summaryTextView;
    private ProgressBar progressBar;
    private LinearLayout btnLayout;
    private ConstraintLayout bodyLayout;
    private SearchView searchView;
    private WebView webView;
    private Spinner keyWords;
    private ListView savedList;
    private ArrayAdapter<String> savedAdapter;

    private Random random;
    private int lastColorSet;

    private boolean inWebView;
    boolean pageLoaded = false;
    private boolean inListView;
    private boolean savePressed;

    private boolean inSummaryMode;
    private boolean isLoading;

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

        searchQuery = EMPTY;

        pageHistory = new HashMap<String, Integer>();
        images = new ArrayList<Bitmap>();
        newsInfo = new ArrayList<ArrayList<String>>();
        storedHeadlines = new ArrayList<String>();
        articles = new HashMap<String, String>();

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        likeBtn = (ImageButton) findViewById(R.id.likeBtn);
        customFont = Typeface.createFromAsset(getAssets(), "fonts/coolvetica_rg.ttf");
        headlineTextView = (TextView) findViewById(R.id.headlineTextView);
        dislikeBtn = (ImageButton) findViewById(R.id.dislikeBtn);
        backgroundView = (ImageView) findViewById(R.id.backgroundView);
        summaryScrollView = (ScrollView) findViewById(R.id.summaryScrollView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        summaryTextView = (TextView) findViewById(R.id.summaryTextView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnLayout = (LinearLayout) findViewById(R.id.btnLayout);
        bodyLayout = (ConstraintLayout) findViewById(R.id.bodyLayout);
        linkBtn = (Button) findViewById(R.id.linkBtn);
        saveArticleBtn = (Button) findViewById(R.id.saveArticleBtn);
        savedBtn = (ImageButton) findViewById(R.id.savedBtn);
        searchView = (SearchView) findViewById(R.id.searchView);
        webView= (WebView)findViewById(R.id.WebView1);
        //keyWords = (Spinner)findViewById(R.id.spinner1);
        savedList = (ListView)findViewById(R.id.list1);

        //headlineTextView.setTypeface(customFont);
        // write helper method for vis initialization
        summaryScrollView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        //keyWords.setVisibility(View.GONE);
        savedList.setVisibility(View.GONE);

        likeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                decorView.setBackgroundColor(Color.WHITE);
                headlineTextView.setTextColor(Color.BLACK);
                headlineTextView.setVisibility(View.GONE);
                btnLayout.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                generateSummary();
            }
        });

        dislikeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                displayNextNews();
                if (newsInfo.size() < 10) generateNews(NEWS_BATCH);
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

        saveArticleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                if(!savePressed) {
                    String url = newsInfo.get(0).get(1);
                    storedHeadlines.add(newsInfo.get(0).get(0));
                    articles.put(newsInfo.get(0).get(0), newsInfo.get(0).get(1));
                    savedAdapter.notifyDataSetChanged();
                    savePressed = true;
                }
            }
        });

        savedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, storedHeadlines);
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

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                clearNewsInfo();
                query = processQuery(query);
                searchQuery = query;
                updateVisSwipeUnloaded();
                generateNews(NEWS_BATCH);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        bodyLayout.setOnTouchListener(new OnSwipeTouchListener(SwipeActivity.this) {
            public void onSwipeRight() {
                decorView.setBackgroundColor(Color.WHITE);
                headlineTextView.setTextColor(Color.BLACK);
                headlineTextView.setVisibility(View.GONE);
                btnLayout.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                generateSummary();
            }

            public void onSwipeLeft() {
                displayNextNews();
                if (newsInfo.size() < 10) generateNews(NEWS_BATCH);
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
            webView.loadUrl("about:blank");
            pageLoaded = false;
            savePressed = false;
        }
        else if(inWebView){
            if(inListView){
                updateLoadSavedArticles();
                webView.loadUrl("about:blank");
            }
            else {
                updateVisSummaryLoaded();
            }
        }
        else if(inListView){
            updateVisSwipeLoaded();
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
        btnLayout.setVisibility(View.GONE);
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
        }
        else {
            updateVisSwipeUnloaded();
        }
        changeColorSet();
    }

    private void changeColorSet() {
        int rand;
        do {
            rand = random.nextInt(5);
        } while (rand == lastColorSet);
        System.err.println(rand);
        decorView.setBackgroundColor(backgroundColors[rand]);
        headlineTextView.setTextColor(textColors[rand]);
        lastColorSet = rand;
    }

    private static String constructURL(String API, Map<String, String> params) {
        String url = API + "?";
        Iterator<String> keyIterator = params.keySet().iterator();
        int index = 0;
        while (keyIterator.hasNext()) {
            if (index != 0) url += "&";
            String key = keyIterator.next();
            url += key + "=" + params.get(key);
            index++;
        }
        return url;
    }

    private void generateNews(int toRetrieve) {

        RequestQueue queue = Volley.newRequestQueue(this);
        Map<String, String> params = new HashMap<String, String>();
        String API = "https://newsapi.org/v2";
        int page = 1;
        if (pageHistory.containsKey(searchQuery)) {
            pageHistory.put(searchQuery, pageHistory.get(searchQuery).intValue() + 1);
            page = pageHistory.get(searchQuery).intValue();
        }
        else {
            pageHistory.put(searchQuery, page);
        }

        params.put("apiKey", Config.NEWS_API_KEY);
        params.put("page", Integer.toString(page));
        if (searchQuery == null) {
            params.put("country", "us");
            API += "/top-headlines";
        }
        else {
            params.put("q", searchQuery);
            params.put("language", "en");
            params.put("sortBy", "popularity");
            API += "/everything";
        }
        String url = constructURL(API, params);

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
                                newsInfo.add(info);
                            }
                            if (isLoading) displayNextNews();
                            updateVisSwipeLoaded();

                            System.err.println("Success");
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
        Map<String, String> params = new HashMap<String, String>();

        params.put("SM_URL", newsInfo.get(0).get(1));
        params.put("SM_LENGTH", Config.SM_LENGTH);
        params.put("SM_API_KEY", Config.SMMRY_API_KEY.get(apiKeyIndex));
	    apiKeyIndex = (apiKeyIndex + 1) % Config.SMMRY_API_KEY.size();
        String url = constructURL("https://api.smmry.com", params);
        System.err.println(url);
	// Request a string response from the provided URL.
        StringRequestRetry stringRequest = new StringRequestRetry(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        HashMap<String,String> data = new Gson().fromJson(response, new TypeToken<HashMap<String, String>>(){}.getType());
                        summaryTextView.setText(data.get("sm_api_content"));
                        titleTextView.setText(newsInfo.get(0).get(0));
                        updateVisSummaryLoaded();
                        System.err.println("Success: summary");
                        System.err.println(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.err.println("Fail: summary");
                System.err.println(error);
            }
        });
        queue.add(stringRequest);
    }

    private void openWebView(){

        //open browser inside your app
        webView.setWebViewClient(new MyBrowser());
        String url = newsInfo.get(0).get(1);
        webView.getSettings().getLoadsImagesAutomatically();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
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
    private void generateBackgroundImg() {
        RequestQueue queue = Volley.newRequestQueue(this);
        ImageLoader imageLoader = new ImageLoader(queue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap>
                    cache = new LruCache<String, Bitmap>(20);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });

        // If you are using normal ImageView
        imageLoader.get(newsInfo.get(0).get(2), new ImageLoader.ImageListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //Log.e(TAG, "Image Load Error: " + error.getMessage());
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean arg1) {
                if (response.getBitmap() != null) {
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int screenHeight = displayMetrics.heightPixels;
                    int screenWidth = displayMetrics.widthPixels;

                    Bitmap imgBitmap = response.getBitmap();
                    int imgHeight = imgBitmap.getHeight();
                    int imgWidth = imgBitmap.getWidth();
                    double imgAS = (double) imgWidth / imgHeight;
                    Bitmap imgResized;
                    Bitmap imgCropped;
                    if (imgAS > 1.0) {
                        imgResized = Bitmap.createScaledBitmap(imgBitmap, (int) (screenHeight * imgAS), screenHeight, false);
                        imgCropped = Bitmap.createBitmap(imgResized,
                                (int) (imgResized.getWidth() - screenWidth) / 2,
                                0,
                                screenWidth, screenHeight);
                    }
                    else {
                        imgResized = Bitmap.createScaledBitmap(imgBitmap, screenWidth, (int) (screenWidth / imgAS), false);
                        imgCropped = Bitmap.createBitmap(imgResized,
                                0,
                                (int) (imgResized.getHeight() - screenHeight) / 2,
                                screenWidth, screenHeight);
                    }
                    backgroundView.setImageBitmap(imgCropped);
                }
            }
        });
    }


    private void clearNewsInfo() {
        newsInfo.clear();
    }

    private static String processQuery(String query) {
        return query.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }
}
