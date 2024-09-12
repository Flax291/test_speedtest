package com.example.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {

    private RadioGroup radioGroupTheme; // Радиогруппа для темы
    private CheckBox checkboxDownload; // Чекбокс загрузки
    private CheckBox checkboxUpload; // Чекбокс выгрузки
    private Button buttonSave; // Кнопка сохранить

    public static final String PREFS_NAME = "AppSettings"; // Имя настроек
    public static final String KEY_THEME = "theme"; // Ключ для темы
    public static final String KEY_DOWNLOAD = "downloadEnabled"; // Ключ для загрузки
    public static final String KEY_UPLOAD = "uploadEnabled"; // Ключ для выгрузки

    private boolean isTesting = false; // Флаг тестирования

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false); // Загрузка макета
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализация UI
        radioGroupTheme = view.findViewById(R.id.radio_theme); // Радиогруппа для выбора темы
        checkboxDownload = view.findViewById(R.id.checkbox_download); // Чекбокс для измерения загрузки
        checkboxUpload = view.findViewById(R.id.checkbox_upload); // Чекбокс для измерения выгрузки
        buttonSave = view.findViewById(R.id.button_save); // Кнопка сохранить

        loadSettings(); // Загрузка настроек

        // Обработчик клика на кнопку
        buttonSave.setOnClickListener(v -> saveSettings());
    }

    public void setTestingState(boolean testing) {
        isTesting = testing; // Устанавливаем состояние тестирования
    }

    private void applyTheme() {
        int themeId = radioGroupTheme.getCheckedRadioButtonId();

        // Применяем тему по выбору
        if (themeId == R.id.radio_light) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Светлая тема
        } else if (themeId == R.id.radio_dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Тёмная тема
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); // Системная тема
        }

        // Перезагружаем активность, чтобы применить изменения
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private void saveSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int selectedTheme = radioGroupTheme.getCheckedRadioButtonId();
        editor.putInt(KEY_THEME, selectedTheme);
        editor.putBoolean(KEY_DOWNLOAD, checkboxDownload.isChecked());
        editor.putBoolean(KEY_UPLOAD, checkboxUpload.isChecked());
        editor.apply();

        if (!isTesting) {
            applyTheme();
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreTestingState();
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeId = prefs.getInt(KEY_THEME, R.id.radio_system); // Получаем ID темы
        radioGroupTheme.check(themeId);
        boolean downloadEnabled = prefs.getBoolean(KEY_DOWNLOAD, true); // Получаем состояние чекбокса загрузки
        boolean uploadEnabled = prefs.getBoolean(KEY_UPLOAD, true); // Получаем состояние чекбокса выгрузки

        checkboxDownload.setChecked(downloadEnabled); // Устанавливаем чекбокс загрузки
        checkboxUpload.setChecked(uploadEnabled); // Устанавливаем чекбокс выгрузки
        radioGroupTheme.check(themeId); // Устанавливаем выбранную тему
    }
}
