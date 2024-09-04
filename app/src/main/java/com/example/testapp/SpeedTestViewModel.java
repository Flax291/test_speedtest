package com.example.testapp;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
/**
 * ViewModel для управления данными тестирования скорости интернета.
 */
public class SpeedTestViewModel extends ViewModel {
    // Флаг, указывающий, активно ли тестирование.
    public boolean isTesting = false; // Флаг, указывающий, активно ли тестирование.
    public List<Double> downloadSpeeds = new ArrayList<>();// Список со скоростями загрузки (в Mbps или другой единице).
    public List<Double> uploadSpeeds = new ArrayList<>();// Список со скоростями выгрузки (в Mbps или другой единице).
    public boolean measureDownload = true; // Флаг для включения/выключения измерения скорости загрузки.
    public boolean measureUpload = true;// Флаг для включения/выключения измерения скорости выгрузки.
}

