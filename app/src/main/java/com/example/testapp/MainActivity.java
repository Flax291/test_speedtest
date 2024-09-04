package com.example.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // FragmentManager для управления фрагментами в активности
    private final FragmentManager fragmentManager = getSupportFragmentManager();

    // Ссылка на фрагмент тестирования скорости, который может быть создан и использован
    private SpeedTestFragment speedTestFragment;

    // Метод для восстановления состояния тестирования, если фрагмент speedTestFragment уже добавлен
    public void restoreTestingState() {
        // Проверяем, что фрагмент не равен null и был добавлен в активность
        if (speedTestFragment != null && speedTestFragment.isAdded()) {
            // Загружаем настройки и подготавливаем фрагмент к тестированию
            speedTestFragment.loadSettingsAndPrepareSpeed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Получаем SharedPreferences для доступа к сохранённым настройкам
        SharedPreferences sharedPreferences = getSharedPreferences(HistoryFragment.PREFS_NAME, Context.MODE_PRIVATE);
        // Извлекаем ID темы из настроек. По умолчанию используется системная тема
        int themeId = sharedPreferences.getInt(HistoryFragment.KEY_THEME, R.id.radio_system);

        // Устанавливаем тему активности в зависимости от выбранного значения
        if (themeId == R.id.radio_light) {

            setTheme(R.style.LightTheme);
        } else if (themeId == R.id.radio_dark) {

            setTheme(R.style.DarkTheme);
        }

        // Устанавливаем макет активности
        setContentView(R.layout.activity_main);


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {// Установка слушателя нажатий на элементы меню
            Fragment selectedFragment;
            int itemId = item.getItemId();// Получаем ID выбранного элемента меню
            if (itemId == R.id.nav_test) {// Определяем, какой фрагмент нужно отобразить в зависимости от выбранного элемента меню
                if (speedTestFragment == null) {
                    speedTestFragment = new SpeedTestFragment();
                }
                selectedFragment = speedTestFragment;
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else {
                return false;
            }

            fragmentManager.beginTransaction()// Заменяем текущий экран на выбранный и применяем изменения
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;

        });
        if (savedInstanceState == null) {// Проверяем, если savedInstanceState равен null, это означает, что активность создаётся впервые
            speedTestFragment = new SpeedTestFragment();

            fragmentManager.beginTransaction()// Заменяем фрагмент в контейнере на новый экземпляр SpeedTestFragment
                    .replace(R.id.fragment_container, speedTestFragment)
                    .commit();
        }
    }
}
