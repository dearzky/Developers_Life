package com.example.developers_life;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    TabLayout tabs;
    TextView tv;
    ImageView iv;
    ImageButton back, next;

    Map<Integer, String> hashText = new HashMap<>();
    Map<Integer, String> hashGifs = new HashMap<>();
    int hashLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabs = (TabLayout) findViewById(R.id.tabLayout);
        tv = (TextView) findViewById(R.id.textView);
        iv = (ImageView) findViewById(R.id.imageView);
        back = (ImageButton) findViewById(R.id.button2);
        next = (ImageButton) findViewById(R.id.button);

        back.setEnabled(false);

        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.alpha);
        SetTextAndGif("https://developerslife.ru/latest/" + RandomNumber() + "?json=true");

        back.setOnClickListener(v -> {
            v.startAnimation(animAlpha);

            hashLevel--;
            tv.setText(hashText.get(hashLevel));
            Glide.with(getApplicationContext())
                    .load(hashGifs.get(hashLevel))
                    .placeholder(R.drawable.loading)
                    .centerCrop()
                    .into(iv);

            if (hashLevel == 0) {
                back.setEnabled(false);
            }
        });

        next.setOnClickListener(v -> {
            v.startAnimation(animAlpha);
            back.setEnabled(true);
            hashLevel++;
            if (hashText.size() > hashLevel){

                tv.setText(hashText.get(hashLevel));
                Glide.with(getApplicationContext())
                        .load(hashGifs.get(hashLevel))
                        .placeholder(R.drawable.loading)
                        .centerCrop()
                        .into(iv);
                return;
            }

            String finalUrl = ChooseCategory(tabs.getSelectedTabPosition());
            SetTextAndGif(finalUrl);
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                hashLevel = 0;
                hashText.clear();
                hashGifs.clear();
                back.setEnabled(false);

                String finalUrl = ChooseCategory(tab.getPosition());
                SetTextAndGif(finalUrl);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    void SetTextAndGif(String query) {
        if(isOnline(this)) {
            Thread thread = new Thread(() -> {

                String str = "";
                StringBuilder textJSON = new StringBuilder();

                URL url = null;
                try {
                    url = new URL(query);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                BufferedReader in = null;
                try {
                    assert url != null;
                    in = new BufferedReader(new InputStreamReader(url.openStream()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (true) {
                    try {
                        if (!((str = in.readLine()) != null)) break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    textJSON.append(str);
                }

                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(textJSON.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    assert jsonObject != null;
                    str = jsonObject.getJSONArray("result").getJSONObject(0).getString("description");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                tv.setText(String.valueOf(str));

                hashText.put(hashLevel, str);

                String gifURL = null;
                try {
                    gifURL = jsonObject.getJSONArray("result").getJSONObject(0).getString("gifURL");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                assert gifURL != null;
                String finalGifURL = gifURL.replace("http:", "https:");


                runOnUiThread(() -> Glide.with(getApplicationContext())
                        .load(finalGifURL)
                        .placeholder(R.drawable.loading)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(iv));

                hashGifs.put(hashLevel, finalGifURL);
            });
            thread.start();
        }
        else {
            hashLevel--;
            if (hashLevel == -1)
                back.setEnabled(false);
            Toast toast = Toast.makeText(MainActivity.this, "Произошла ошибка при загрузке данных. Проверьте подключение к сети.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    int RandomNumber()
    {
        Random random = new Random();
        return random.nextInt(1500 + 1);
    }

    String ChooseCategory(int numCategory)
    {
        String url = "";
        switch (tabs.getSelectedTabPosition()) {
            case 0:
                url = "https://developerslife.ru/latest/" + RandomNumber() + "?json=true";
                break;
            case 1:
                url = "https://developerslife.ru/top/" + RandomNumber() + "?json=true";
                break;
            case 2:
                //url = "https://developerslife.ru/hot/" + RandomNumber() + "?json=true"; //Не работает!!!
                url = "https://developerslife.ru/top/" + RandomNumber() + "?json=true";
                break;
            default:
                break;
        }
        return url;
    }

    public static boolean isOnline(Context context)
    {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}