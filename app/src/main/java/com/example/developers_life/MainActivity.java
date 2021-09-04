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
    TextView description;
    ImageView gif;
    ImageButton back, next;

    Map<Integer, String> hashText = new HashMap<>();
    Map<Integer, String> hashGifs = new HashMap<>();
    int hashLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabs = (TabLayout) findViewById(R.id.tabLayout);
        description = (TextView) findViewById(R.id.description);
        gif = (ImageView) findViewById(R.id.gif);
        back = (ImageButton) findViewById(R.id.back);
        next = (ImageButton) findViewById(R.id.next);

        back.setEnabled(false);

        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.alpha);
        SetTextAndGif("https://developerslife.ru/latest/" + RandomNumber() + "?json=true"); // первая прогрузка данных

        back.setOnClickListener(v -> { // обработчик нажатия "назад"
            v.startAnimation(animAlpha); // анимация

            hashLevel--; // проходим вниз по уровню кэша
            description.setText(hashText.get(hashLevel)); // выводим текст
            Glide.with(getApplicationContext())
                    .load(hashGifs.get(hashLevel))
                    .thumbnail(Glide.with(getApplicationContext()).load(R.drawable.loading))
                    .centerCrop()
                    .into(gif); // выводим гифку

            if (hashLevel == 0) { // если кэш уровень стал равен нуля то кнопку назад делаем не активной
                back.setEnabled(false);
            }
        });

        next.setOnClickListener(v -> { // обработчик нажатия "следующая"
            v.startAnimation(animAlpha); // анимация
            back.setEnabled(true); // назад делаем активной
            hashLevel++; // проходим вперед по уровню кэша
            if (hashText.size() > hashLevel){ // читаем кэш если это возможно

                description.setText(hashText.get(hashLevel)); // выводим текст
                Glide.with(getApplicationContext())
                        .load(hashGifs.get(hashLevel))
                        .placeholder(R.drawable.loading)
                        .centerCrop()
                        .into(gif); // выводим гифку
                return;
            }
            // если кэш полностью прошли то подгружаем новые данные с сервера
            String finalUrl = ChooseCategory(tabs.getSelectedTabPosition());
            SetTextAndGif(finalUrl);
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { // выбор элемента в категориях
                hashLevel = 0; // сбрасываем кэщ
                hashText.clear(); // очищаем кэщ описания
                hashGifs.clear(); // очищаем кэщ гивок
                back.setEnabled(false); // делаем кнопку назад не активной

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

    /* Функция отправляющая get запрос, и занимается обработкой полученных данных */
    void SetTextAndGif(String query) {
        if(isOnline(this)) {
            Thread thread = new Thread(() -> {

                String str = "";
                StringBuilder textJSON = new StringBuilder();

                URL url = null;
                try {
                    url = new URL(query); // формируем url для отправки get запросом
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
                        assert in != null;
                        if ((str = in.readLine()) == null) break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    textJSON.append(str); // читаем полученные данные построчно и складываем в переменную
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
                    str = jsonObject.getJSONArray("result").getJSONObject(0).getString("description"); // получаем выборочные данные, описание из первого подмассива
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                description.setText(String.valueOf(str)); // выводим описание на экран

                hashText.put(hashLevel, str); // добавляем описание в кэш

                String gifURL = null;
                try {
                    gifURL = jsonObject.getJSONArray("result").getJSONObject(0).getString("gifURL"); // получаем url на gif из первого подмассива
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                assert gifURL != null;
                String finalGifURL = gifURL.replace("http:", "https:"); // заменяем http на https. с http не хочет работать

                runOnUiThread(() -> Glide.with(getApplicationContext())
                        .load(finalGifURL)
                        .thumbnail(Glide.with(getApplicationContext()).load(R.drawable.loading))
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(gif)); // выводим gif на экран, пока грузится показываем значок загрузки

                hashGifs.put(hashLevel, finalGifURL); // добавляем url на gif в кэш, не добавлял в полноценную gif в кэш потому что как я понял glide сам кэширует данные, по крайней мере уже загруженные файлы выводятся на экран быстрее и подгружаются без интернета
            });
            thread.start(); // запускаем всю обработку в отдельном потоке
        }
        else { // если не прошла проверка на пожкд.чение к сети
            hashLevel--; // уменьшаем значение кэша так как до этого его увеличили
            if (hashLevel == -1) // тут обрабатываем ситуацию когда с первой загрузки без сети по нажатию кнопки дальше кнопка назад становится активна
                back.setEnabled(false);
            Toast toast = Toast.makeText(MainActivity.this, "Произошла ошибка при загрузке данных. Проверьте подключение к сети.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show(); // выводим сообщение об ошибке
        }
    }

    /* Функция получения случайного числа на диапозоне до 1500 */
    int RandomNumber()
    {
        Random random = new Random();
        return random.nextInt(1500 + 1);
    }

    /* Функция принимающая номер выбранной категории и вызывающая функцию отправки get запроса */
    String ChooseCategory(int numCategory)
    {
        String url = "";
        switch (numCategory) {
            case 0:
                url = "https://developerslife.ru/latest/" + RandomNumber() + "?json=true";
                break;
            case 1:
            case 2:
                url = "https://developerslife.ru/top/" + RandomNumber() + "?json=true";
                //url = "https://developerslife.ru/hot/" + RandomNumber() + "?json=true"; // Горячее не работает!!!
                break;
            default:
                break;
        }
        return url;
    }

    /* Функция проверки подключения сети */
    public static boolean isOnline(Context context)
    {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}