package com.example.testapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeedTestFragment extends Fragment {

    // UI-компоненты
    private TextView textSpeedDownload, textSpeedUpload, textAverageSpeedDownload, textMedianSpeedDownload;
    private TextView textAverageSpeedUpload, textMedianSpeedUpload, textTestStatus, textCurrentServer;
    private Button buttonStartTest, buttonShowAdditionalInfo;

    private final OkHttpClient client = new OkHttpClient(); // Класс для работы с сетью
    private final Handler handler = new Handler();// Для управления задержками и потоками
    private SpeedTestViewModel viewModel; // ViewModel для хранения состояния теста

    private boolean showAdditionalInfo = false;
    private boolean additionalInfoVisible = false;

    // Измерения
    private static final int TEST_DURATION_MS = 20000; // Длительность теста (20 секунд)
    private static final int UPDATE_INTERVAL_MS = 5000; //Интервал обновления (5 секунд)
    private static final String DOWNLOAD_URL = "https://speedtest.selectel.ru/10MB";// URL для теста загрузки
    private static final String UPLOAD_URL = "http://147.45.147.32";// URL для теста выгрузки

    private String testStatus = "";
    private String lastMessage = "";
    private boolean isPaused = false; // Флаг для паузы

    private boolean pendingDownloadSetting = false;// Отложенная настройка для загрузки
    private boolean pendingUploadSetting = false;// Отложенная настройка для отдачи

    private String currentServerText = ""; // Для сохранения текста текущего сервера

    @Nullable
    @Override// Раздуваем макет фрагмента
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_speedtest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textCurrentServer = view.findViewById(R.id.text_current_server);

        // Инициализация ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(SpeedTestViewModel.class);

        // Инициализация UI-компонентов
        textSpeedDownload = view.findViewById(R.id.text_speed_download);
        textSpeedUpload = view.findViewById(R.id.text_speed_upload);
        textAverageSpeedDownload = view.findViewById(R.id.text_average_speed_download);
        textMedianSpeedDownload = view.findViewById(R.id.text_median_speed_download);
        textAverageSpeedUpload = view.findViewById(R.id.text_average_speed_upload);
        textMedianSpeedUpload = view.findViewById(R.id.text_median_speed_upload);
        textTestStatus = view.findViewById(R.id.text_test_status);
        buttonStartTest = view.findViewById(R.id.button_start_test);
        buttonShowAdditionalInfo = view.findViewById(R.id.button_show_additional_info);

        // Восстановление состояний
        if (savedInstanceState != null) {
            lastMessage = savedInstanceState.getString("LAST_MESSAGE", "");
            additionalInfoVisible = savedInstanceState.getBoolean("ADDITIONAL_INFO_VISIBLE", false);
            currentServerText = savedInstanceState.getString("CURRENT_SERVER_TEXT", ""); // Восстановление текста сервера
        }


        textCurrentServer.setText(currentServerText);// Устанавливаем текст текущего сервера

        // Устанавливаем видимость дополнительной информации
        if (additionalInfoVisible) {
            updateAverageSpeeds();// Обновляем средние и медианные скорости
        } else {
            hideAverageSpeeds();// Прячем средние и медианные скорости
        }

        updateUI();// Обновляем UI
        textTestStatus.setText(testStatus);
        textTestStatus.append(lastMessage);

        buttonStartTest.setOnClickListener(v -> {
            if (viewModel.isTesting) {
                stopTest();  // Ensure that the test stops completely when the button is pressed
            } else {
                if (viewModel.measureDownload || viewModel.measureUpload) {
                    viewModel.isTesting = true;
                    isPaused = false;
                    buttonStartTest.setText("Stop Test");
                    lastMessage = "";
                    startTests();
                    updateHistoryFragmentTestingState(true);
                } else {
                    lastMessage = "Выберите в настройках, что хотите протестировать";
                    textTestStatus.setText(lastMessage);
                }
            }
        });


        buttonShowAdditionalInfo.setOnClickListener(v -> {
            showAdditionalInfo = !showAdditionalInfo;// Переключаем флаг отображения дополнительной информации
            additionalInfoVisible = showAdditionalInfo;
            if (showAdditionalInfo) {
                updateAverageSpeeds();// Показываем средние и медианные скорости
            } else {
                hideAverageSpeeds();// Прячем средние и медианные скорости
            }
        });

        loadSettingsAndPrepareSpeed();// Загружаем настройки и подготавливаем тест
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("LAST_MESSAGE", lastMessage);
        outState.putBoolean("ADDITIONAL_INFO_VISIBLE", additionalInfoVisible);
        outState.putString("CURRENT_SERVER_TEXT", textCurrentServer.getText().toString()); // Сохранение текста текущего сервера
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Ставим тест на паузу при изменении темы
        if (viewModel.isTesting) {
            isPaused = true;
            buttonStartTest.setText("Continue Test");

            stopTest(); // Останавливаем тест, чтобы не прерывать его без необходимости
        }
    }

    private void updateUI() { // Обновляем UI с текущими данными
        textSpeedDownload.setText("Current Download Speed: " + formatSpeed(getCurrentDownloadSpeed()));
        textSpeedUpload.setText("Current Upload Speed: " + formatSpeed(getCurrentUploadSpeed()));
        buttonStartTest.setText(viewModel.isTesting ? (isPaused ? "Continue Test" : "Stop Test") : "Start Test");
        textTestStatus.setText(testStatus);
        textTestStatus.append(lastMessage);
    }

    private double getCurrentDownloadSpeed() {// Получаем текущую скорость загрузки
        return viewModel.downloadSpeeds.isEmpty() ? 0 : viewModel.downloadSpeeds.get(viewModel.downloadSpeeds.size() - 1);
    }

    private double getCurrentUploadSpeed() {// Получаем текущую скорость отдачи
        return viewModel.uploadSpeeds.isEmpty() ? 0 : viewModel.uploadSpeeds.get(viewModel.uploadSpeeds.size() - 1);
    }

    private void startTests() {
        // Запуск тестов скорости
        viewModel.downloadSpeeds.clear();// Очищаем список скоростей загрузки
        viewModel.uploadSpeeds.clear();// Очищаем список скоростей отдачи
        textTestStatus.setText(testStatus);
        isPaused = false; // Сброс флага паузы

        final long startTimestamp = System.currentTimeMillis();// Время начала теста
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (viewModel.isTesting && !isPaused && System.currentTimeMillis() - startTimestamp < TEST_DURATION_MS) {
                    measureSpeeds();// Измеряем скорости
                    updateUI();
                    handler.postDelayed(this, UPDATE_INTERVAL_MS);// Запускаем повторно через интервал
                } else {
                    if (viewModel.isTesting) {
                        finishTest();
                    }
                }
            }
        }, 0);
    }

    public void stopTest() {
        viewModel.isTesting = false;
        handler.removeCallbacksAndMessages(null);
        buttonStartTest.setText("Start Test");
        textTestStatus.setText(testStatus);
        isPaused = false;


        updateHistoryFragmentTestingState(false);


    }

    private void resumeTest() {// Возобновление теста после паузы
        if (viewModel.isTesting) return; // Если тест уже идет, ничего не делаем
        isPaused = false; // Сбрасываем флаг паузы
        updateUI(); // Обновляем UI
        startTests(); // Возобновляем тест
    }

    private void finishTest() {
        viewModel.isTesting = false;
        buttonStartTest.setText("Start Test");
        textTestStatus.setText(testStatus);
        updateAverageSpeeds();// Обновляем средние и медианные скорости
        updateHistoryFragmentTestingState(false);// Обновляем состояние в HistoryFragment
    }

    private void measureSpeeds() {// Измеряем скорости загрузки и отдачи
        if (viewModel.measureDownload) {
            currentServerText = "Current Server: " + DOWNLOAD_URL; // Текущий сервер для загрузки
            textCurrentServer.setText(currentServerText);
            measureDownloadSpeed();// Запускаем измерение скорости загрузки
        }
        if (viewModel.measureUpload) {
            currentServerText = "Current Server: " + UPLOAD_URL; // Текущий сервер для отдачи
            textCurrentServer.setText(currentServerText);
            measureUploadSpeed();// Запускаем измерение скорости отдачи
        }
    }

    private void measureDownloadSpeed() {// Измеряем скорость загрузки
        if (!viewModel.isTesting || isPaused) return;// Если тест не идет или на паузе, выходим

        Request downloadRequest = new Request.Builder().url(DOWNLOAD_URL).build();// Запрос на загрузку
        client.newCall(downloadRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SpeedTest", "Download failed", e);
                handler.post(() -> textSpeedDownload.setText("Current Download Speed: Error"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("SpeedTest", "Unexpected code " + response);
                    handler.post(() -> textSpeedDownload.setText("Current Download Speed: Error"));
                    return;
                }

                long startTime = System.currentTimeMillis(); // Время начала измерения
                long bytesRead = 0;// Количество считанных байтов
                try (InputStream inputStream = response.body().byteStream()) {
                    byte[] buffer = new byte[8192];// Буфер для чтения данных
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        bytesRead += read;// Увеличиваем количество считанных байтов
                        long duration = System.currentTimeMillis() - startTime;// Время, прошедшее с начала измерения
                        if (duration > 0) {
                            double speedBps = (bytesRead * 8) / (duration / 1000.0);// Скорость в битах в секунду
                            double speedMbps = speedBps / 1_000_000;// Скорость в мегабитах в секунду

                            handler.post(() -> {
                                if (viewModel.isTesting && !isPaused) {
                                    textSpeedDownload.setText("Current Download Speed: " + formatSpeed(speedMbps));// Обновляем UI
                                    viewModel.downloadSpeeds.add(speedMbps);// Добавляем скорость в список
                                    Log.d("SpeedTest", "Current Download Speed: " + formatSpeed(speedMbps));
                                }
                            });
                        }
                    }
                } finally {
                    response.close();// Закрываем ответ
                }
            }
        });
    }

    private void measureUploadSpeed() {// Измеряем скорость выгрузки
        if (!viewModel.isTesting || isPaused) return;// Если тест не идет или на паузе, выходим

        byte[] uploadData = new byte[300_000];// Данные для загрузки
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), uploadData);// Тело запроса
        Request uploadRequest = new Request.Builder().url(UPLOAD_URL).post(requestBody).build();// Запрос на выгрузку

        long startTime = System.currentTimeMillis();
        client.newCall(uploadRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SpeedTest", "Upload failed", e);
                handler.post(() -> textSpeedUpload.setText("Current Upload Speed: Error"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Log.e("SpeedTest", "Unexpected code " + response);
                        handler.post(() -> textSpeedUpload.setText("Current Upload Speed: Error"));
                        return;
                    }

                    long duration = System.currentTimeMillis() - startTime;// Время, прошедшее с начала измерения
                    if (duration == 0) duration = 1;// Защита от деления на ноль

                    double speedBps = (uploadData.length * 8) / (duration / 1000.0);
                    double speedMbps = speedBps / 1_000_000;

                    handler.post(() -> {
                        if (viewModel.isTesting && !isPaused) {
                            textSpeedUpload.setText("Current Upload Speed: " + formatSpeed(speedMbps));
                            viewModel.uploadSpeeds.add(speedMbps);
                            Log.d("SpeedTest", "Current Upload Speed: " + formatSpeed(speedMbps));
                        }
                    });
                } finally {
                    response.close();// Закрываем ответ
                }
            }
        });
    }

    private String formatSpeed(double speed) {
        return String.format("%.2f Mbps", speed);// Форматируем скорость в мегабитах
    }

    private double calculateAverageSpeed(List<Double> speeds) { // Рассчитываем среднюю скорость
        double sum = 0;
        for (double speed : speeds) {
            sum += speed;
        }
        return speeds.isEmpty() ? 0 : sum / speeds.size();
    }

    private double calculateMedianSpeed(List<Double> speeds) {// Рассчитываем медианную скорость
        if (speeds.isEmpty()) return 0;

        List<Double> sortedSpeeds = new ArrayList<>(speeds);
        sortedSpeeds.sort(Double::compareTo);
        int middle = sortedSpeeds.size() / 2;
        /// Если список четный, медиана — среднее значение двух центральных элементов
        return (sortedSpeeds.size() % 2 == 0) ? (sortedSpeeds.get(middle - 1) + sortedSpeeds.get(middle)) / 2 : sortedSpeeds.get(middle);
    }

    private void updateAverageSpeeds() {// Обновляем отображение средних и медианных скоростей
        double averageDownloadSpeed = calculateAverageSpeed(viewModel.downloadSpeeds);
        double medianDownloadSpeed = calculateMedianSpeed(viewModel.downloadSpeeds);
        double averageUploadSpeed = calculateAverageSpeed(viewModel.uploadSpeeds);
        double medianUploadSpeed = calculateMedianSpeed(viewModel.uploadSpeeds);

        handler.post(() -> {
            textAverageSpeedDownload.setVisibility(View.VISIBLE);
            textAverageSpeedDownload.setText("Average Download Speed: " + formatSpeed(averageDownloadSpeed));
            textMedianSpeedDownload.setVisibility(View.VISIBLE);
            textMedianSpeedDownload.setText("Median Download Speed: " + formatSpeed(medianDownloadSpeed));
            textAverageSpeedUpload.setVisibility(View.VISIBLE);
            textAverageSpeedUpload.setText("Average Upload Speed: " + formatSpeed(averageUploadSpeed));
            textMedianSpeedUpload.setVisibility(View.VISIBLE);
            textMedianSpeedUpload.setText("Median Upload Speed: " + formatSpeed(medianUploadSpeed));
        });
    }

    private void hideAverageSpeeds() {// Прячем отображение средних и медианных скоростей
        handler.post(() -> {
            textAverageSpeedDownload.setVisibility(View.GONE);
            textMedianSpeedDownload.setVisibility(View.GONE);
            textAverageSpeedUpload.setVisibility(View.GONE);
            textMedianSpeedUpload.setVisibility(View.GONE);
        });
    }

    public void loadSettingsAndPrepareSpeed() {
        if (getActivity() == null) return;

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(HistoryFragment.PREFS_NAME, Context.MODE_PRIVATE);
        boolean newMeasureDownload = sharedPreferences.getBoolean(HistoryFragment.KEY_DOWNLOAD, true);
        boolean newMeasureUpload = sharedPreferences.getBoolean(HistoryFragment.KEY_UPLOAD, true);

        if (viewModel.isTesting) {
            // Stop current measuring if the preferences have changed during the test
            if (newMeasureDownload != viewModel.measureDownload || newMeasureUpload != viewModel.measureUpload) {
                stopTest(); // Immediately stop the test if the state changes
            }
        }

        viewModel.measureDownload = newMeasureDownload;
        viewModel.measureUpload = newMeasureUpload;
        updateUI();
    }


    private void savePendingSettings() {// Сохраняем отложенные настройки после завершения теста
        if (getActivity() == null) return;

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(HistoryFragment.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HistoryFragment.KEY_DOWNLOAD, pendingDownloadSetting);
        editor.putBoolean(HistoryFragment.KEY_UPLOAD, pendingUploadSetting);
        editor.apply();
    }

    private void updateHistoryFragmentTestingState(boolean isTesting) { // Обновляем состояние тестирования в HistoryFragment
        FragmentManager fragmentManager = getActivity() != null ? getActivity().getSupportFragmentManager() : null;
        if (fragmentManager != null) {
            HistoryFragment historyFragment = (HistoryFragment) fragmentManager.findFragmentByTag("HistoryFragmentTag");
            if (historyFragment != null) {
                historyFragment.setTestingState(isTesting);
            }
        }
    }
}
