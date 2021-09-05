package com.example.developers_life;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void CategoryLatestIsCorrect() {
        assertTrue("Категория не равна latest!", MainActivity.ChooseCategory(0).contains("latest"));
    }

    @Test
    public void CategoryTopIsCorrect() {
        assertTrue("Категория не равна top!", MainActivity.ChooseCategory(1).contains("top"));
    }

    @Test
    public void CategoryHotIsCorrect() {
        assertTrue("Категория не равна hot!", MainActivity.ChooseCategory(2).contains("top"));
    }
}