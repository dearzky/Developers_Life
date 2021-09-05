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

        tabs = findViewById(R.id.tabLayout);
        description = findViewById(R.id.description);
        gif = findViewById(R.id.gif);
        back = findViewById(R.id.back);
        next = findViewById(R.id.next);

        back.setEnabled(false);

        final Animation animAlpha = AnimationUtils.loadAnimation(this, R.anim.alpha);
        Extract("https://developerslife.ru/latest/" + RandomNumber() + "?json=true"); // первая прогрузка данных

        back.setOnClickListener(v -> { // обработчик нажатия "назад"
            v.startAnimation(animAlpha); // анимация

            hashLevel--; // проходим вниз по уровню кэша

            SetTextAndGif(hashText.get(hashLevel), hashGifs.get(hashLevel));

            if (hashLevel == 0) { // если кэш уровень стал равен нуля то кнопку назад делаем не активной
                back.setEnabled(false);
            }
        });

        next.setOnClickListener(v -> { // обработчик нажатия "следующая"
            v.startAnimation(animAlpha); // анимация
            back.setEnabled(true); // назад делаем активной
            hashLevel++; // проходим вперед по уровню кэша
            if (hashText.size() > hashLevel){ // читаем кэш если это возможно

                SetTextAndGif(hashText.get(hashLevel), hashGifs.get(hashLevel));
                return;
            }
            // если кэш полностью прошли то подгружаем новые данные с сервера
            String finalUrl = ChooseCategory(tabs.getSelectedTabPosition());
            Extract(finalUrl);
        });

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { // выбор элемента в категориях
                hashLevel = 0; // сбрасываем кэщ
                hashText.clear(); // очищаем кэщ описания
                hashGifs.clear(); // очищаем кэщ гивок
                back.setEnabled(false); // делаем кнопку назад не активной

                String finalUrl = ChooseCategory(tab.getPosition());
                Extract(finalUrl);
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
    void Extract(String query) {
        if(isOnline(this)) {
            Thread thread = new Thread(() -> {
                JSONObject jsonObject = null;
                try {
                    jsonObject = convertToJObj(query);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                String descrip = null;
                try {
                    assert jsonObject != null;
                    descrip = GetData(jsonObject,"description");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String gifURL = null;
                try {
                    gifURL = GetData(jsonObject,"gifURL");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String finalGifURL = gifURL != null ? gifURL.replace("http:", "https:") : null; // заменяем http на https. с http не хочет работать

                SetTextAndGif(descrip, finalGifURL);
                hashText.put(hashLevel, descrip); // добавляем описание в кэш
                hashGifs.put(hashLevel, finalGifURL); // добавляем url на gif в кэш, не добавлял полноценную gif в кэш потому что как я понял glide сам кэширует данные, по крайней мере уже загруженные файлы выводятся на экран быстрее и подгружаются без интернета
            });
            thread.start(); // запускаем всю обработку в отдельном потоке
        }
        else { // если не прошла проверка на подключениечение к сети
            hashLevel--; // уменьшаем значение кэша так как до этого его увеличили
            if (hashLevel == -1) // тут обрабатываем ситуацию когда с первой загрузки без сети по нажатию кнопки дальше кнопка назад становится активна
                back.setEnabled(false);
            Toast toast = Toast.makeText(MainActivity.this, "Произошла ошибка при загрузке данных. Проверьте подключение к сети.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show(); // выводим сообщение об ошибке
        }
    }

    static URL setURL(String query) throws MalformedURLException {
        return new URL(query);
    }

    static StringBuilder getAnswerFromGetQuery(String query) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(setURL(query).openStream()));
        String str;
        StringBuilder textJSON = new StringBuilder();

        while ((str = in.readLine()) != null) {
            textJSON.append(str); // читаем полученные данные построчно и складываем в переменную
        }
        in.close();
        return textJSON;
    }

    static JSONObject convertToJObj(String query) throws IOException, JSONException {
        return new JSONObject(getAnswerFromGetQuery(query).toString());
    }

    static String GetData(JSONObject jobj, String field) throws JSONException {
        return jobj.getJSONArray("result").getJSONObject(0).getString(field);
    }

    void SetTextAndGif(String desc, String Gif)
    {
        description.setText(desc); // выводим текст
        runOnUiThread(() -> Glide.with(getApplicationContext())
                .load(Gif)
                .thumbnail(Glide.with(getApplicationContext()).load(R.drawable.loading))
                .centerCrop()
                .into(gif)); // выводим гифку
    }

    /* Функция получения случайного числа на диапозоне до 1500 */
    static int RandomNumber()
    {
        Random random = new Random();
        return random.nextInt(1500 + 1);
    }

    /* Функция принимающая номер выбранной категории и вызывающая функцию отправки get запроса */
    static String ChooseCategory(int numCategory)
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