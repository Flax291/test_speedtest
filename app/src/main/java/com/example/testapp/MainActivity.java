package com.example.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private SpeedTestFragment speedTestFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Получаем SharedPreferences для доступа к сохранённым настройкам
        SharedPreferences sharedPreferences = getSharedPreferences(HistoryFragment.PREFS_NAME, Context.MODE_PRIVATE);
        int themeId = sharedPreferences.getInt(HistoryFragment.KEY_THEME, R.id.radio_system);
        applyTheme(themeId);  // Применяем тему перед вызовом setContentView

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_test) {
                if (speedTestFragment == null) {
                    speedTestFragment = new SpeedTestFragment();
                }
                selectedFragment = speedTestFragment;
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else {
                return false;
            }

            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        });

        if (savedInstanceState == null) {
            speedTestFragment = new SpeedTestFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, speedTestFragment)
                    .commit();
        }
    }

    public void restoreTestingState() {
        // Метод для восстановления состояния тестирования, если фрагмент speedTestFragment уже добавлен
        if (speedTestFragment != null && speedTestFragment.isAdded()) {
            speedTestFragment.loadSettingsAndPrepareSpeed();
        }
    }

    private void applyTheme(int themeId) {
        if (themeId == R.id.radio_light) {
            setTheme(R.style.LightTheme);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeId == R.id.radio_dark) {
            setTheme(R.style.DarkTheme);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            // Применяем системную тему
            setTheme(R.style.AppTheme); // Это обеспечит автоматическое переключение между светлым и темным режимом в зависимости от системных настроек
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
