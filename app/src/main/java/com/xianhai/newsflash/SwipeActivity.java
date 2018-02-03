package com.xianhai.newsflash;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class SwipeActivity extends AppCompatActivity {
    private static final String NEWS_API_KEY = "90dda19c88a8416b860653fc782245f1";
    private static final List<String> SMMRY_API_KEY = new ArrayList<String>(Arrays.asList("2E906B3F01", "0F763BFFB1"));
    private static final String SM_LENGTH = "4";
    private static final int SEED = 69;
    private static int[] backgroundColors = new int[] {0xFFD70F43,0xFF891cd4, 0xFF3C689F, 0xFF08E742, 0xFFF6C72C,0xFFF63C2C};
    private static int[] textColors = new int[] {0xFFFFFFFF,0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000};
    private ArrayList<Bitmap> images;
    private ArrayList<ArrayList<String>> newsInfo;
    private int apiKeyIndex = 0;

    private View decorView;
    private ImageView backgroundView;
    private TextView headlineTextView;
    private Button likeBtn;
    private Button dislikeBtn;
    private ScrollView summaryScrollView;
    private TextView titleTextView;
    private TextView summaryTextView;
    private ProgressBar progressBar;
    private LinearLayout btnLinearLayout;

    private Random random;
    private int lastColorSet;

    private boolean inSwipeMode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        random = new Random(SEED);
        lastColorSet = 0;
        inSwipeMode = true;

        images = new ArrayList<Bitmap>();
        newsInfo = new ArrayList<ArrayList<String>>();

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        likeBtn = (Button) findViewById(R.id.likeBtn);
        headlineTextView = (TextView) findViewById(R.id.headlineTextView);
        dislikeBtn = (Button) findViewById(R.id.dislikeBtn);
        backgroundView = (ImageView) findViewById(R.id.backgroundView);
        summaryScrollView = (ScrollView) findViewById(R.id.summaryScrollView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        summaryTextView = (TextView) findViewById(R.id.summaryTextView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnLinearLayout = (LinearLayout) findViewById(R.id.btnLinearLayout);

        summaryScrollView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        backgroundView.setOnTouchListener(new OnSwipeTouchListener(SwipeActivity.this) {
            public void onSwipeRight() {
                decorView.setBackgroundColor(Color.WHITE);
                headlineTextView.setTextColor(Color.BLACK);
                headlineTextView.setVisibility(View.GONE);
                btnLinearLayout.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                generateSummary();
            }

            public void onSwipeLeft() {
                displayNextNews();
            }

            public void onSwipeTop() {
            }

            public void onSwipeBottom() {
            }
        });

        generateNews(20, null);
        displayNextNews();
        //makeRequest();
    }

    @Override
    public void onBackPressed() {

    }

    private void updateVisSwipeLoaded() {
        progressBar.setVisibility(View.GONE);
        summaryScrollView.setVisibility(View.GONE);
        btnLinearLayout.setVisibility(View.VISIBLE);
        headlineTextView.setVisibility(View.VISIBLE);
    }

    private void updateVisSwipeUnloaded() {
        progressBar.setVisibility(View.VISIBLE);
        summaryScrollView.setVisibility(View.GONE);
        btnLinearLayout.setVisibility(View.VISIBLE);
        headlineTextView.setVisibility(View.GONE);
    }

    private void updateVisSummaryLoaded() {
        progressBar.setVisibility(View.GONE);
        summaryScrollView.setVisibility(View.VISIBLE);
        btnLinearLayout.setVisibility(View.GONE);
        headlineTextView.setVisibility(View.GONE);
    }

    private void updateVisSummaryUnloaded() {
        progressBar.setVisibility(View.VISIBLE);
        summaryScrollView.setVisibility(View.GONE);
        btnLinearLayout.setVisibility(View.GONE);
        headlineTextView.setVisibility(View.GONE);
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
            rand = random.nextInt(6);
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

    private void generateNews(int toRetrieve, String query) {
        RequestQueue queue = Volley.newRequestQueue(this);
        Map<String, String> params = new HashMap<String, String>();
        String API = "https://newsapi.org/v2";
        params.put("apiKey", NEWS_API_KEY);
        if (query == null) {
            params.put("country", "us");
            API += "/top-headlines";
        }
        else {
            params.put("q", query);
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
                            updateVisSwipeLoaded();
                            displayNextNews();

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
        params.put("SM_LENGTH", SM_LENGTH);
        params.put("SM_API_KEY", SMMRY_API_KEY.get(apiKeyIndex));
	apiKeyIndex = (apiKeyIndex + 1) % SMMR_API_KEY.size();
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

    private void generateBackgroundImg() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

        Drawable img = LoadImageFromWebOperations(newsInfo.get(0).get(2));
        Bitmap imgBitmap = ((Bitmap))
    }

    public static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }
}
