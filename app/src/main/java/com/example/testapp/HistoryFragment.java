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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class HistoryFragment extends Fragment {

    protected RadioGroup radioGroupTheme; // Радиогруппа для темы
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

    private void loadSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); // Получаем SharedPreferences
        int themeId = prefs.getInt(KEY_THEME, R.id.radio_system); // Получаем ID темы
        boolean downloadEnabled = prefs.getBoolean(KEY_DOWNLOAD, true); // Получаем состояние чекбокса загрузки
        boolean uploadEnabled = prefs.getBoolean(KEY_UPLOAD, true); // Получаем состояние чекбокса выгрузки

        checkboxDownload.setChecked(downloadEnabled); // Устанавливаем чекбокс загрузки
        checkboxUpload.setChecked(uploadEnabled); // Устанавливаем чекбокс выгрузки
        radioGroupTheme.check(themeId); // Устанавливаем выбранную тему
    }

    public void setTestingState(boolean testing) {
        isTesting = testing; // Устанавливаем состояние тестирования
    }

    private void saveSettings() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); // Получаем SharedPreferences
        SharedPreferences.Editor editor = prefs.edit(); // Редактор для изменений
        int selectedTheme = radioGroupTheme.getCheckedRadioButtonId(); // ID выбранной темы
        editor.putInt(KEY_THEME, selectedTheme); // Сохраняем тему
        editor.putBoolean(KEY_DOWNLOAD, checkboxDownload.isChecked()); // Сохраняем состояние чекбокса загрузки
        editor.putBoolean(KEY_UPLOAD, checkboxUpload.isChecked()); // Сохраняем состояние чекбокса выгрузки
        editor.apply(); // Применяем изменения

        if (!isTesting) { // Если не в тесте
            applyTheme(); // Применяем тему
        }

        // Обновляем состояние тестирования в MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).restoreTestingState();
        }

        // Показываем тост о сохранении
        Toast.makeText(getContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show();
    }

    private void applyTheme() {
        int themeId = radioGroupTheme.getCheckedRadioButtonId(); // ID выбранной темы

        // Применяем тему по выбору
        if (themeId == R.id.radio_light) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); // Светлая
        } else if (themeId == R.id.radio_dark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Тёмная
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); // По умолчанию
        }
        // Обновите активность, чтобы сразу применить изменения
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }
}
