package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class NeuralNetwork {
    // Размеры сети
    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize = 1;
    private final double learningRate;
    private final String savePostfix;

    // Веса:
    // вход -> скрытый
    private double[][] weightsInputToHidden;
    // скрытый -> выход
    private double[][] weightsHiddenToOutput;

    // Имя файла, из которого загружаем веса
    private String loadWeightsFileName = "20241210_215157";

    // Конструктор: инициализация весов случайными значениями
    public NeuralNetwork(int inputSize, int hiddenSize, double learningRate, String savePostfix) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.learningRate = learningRate;
        this.savePostfix = savePostfix;
        Random rand = new Random();

        weightsInputToHidden = new double[inputSize][hiddenSize];
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputToHidden[i][j] = (rand.nextDouble() - 0.5) * 0.1;
            }
        }

        weightsHiddenToOutput = new double[hiddenSize][outputSize];
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weightsHiddenToOutput[i][j] = (rand.nextDouble() - 0.5) * 0.1;
            }
        }
    }


    public boolean predict(double[] input) {
        if (input.length != inputSize) {
            throw new IllegalArgumentException("Неверный размер входного массива. Ожидается " + inputSize);
        }

        double[] hiddenLayer = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = 0.0;
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputToHidden[i][j];
            }
            hiddenLayer[j] = sigmoid(sum);
        }

        double outputSum = 0.0;
        for (int i = 0; i < hiddenSize; i++) {
            outputSum += hiddenLayer[i] * weightsHiddenToOutput[i][0];
        }

        double output = sigmoid(outputSum);
        return output > 0.5;
    }

    /**
     * Метод обучения (один шаг).
     * Выполняет прямой проход + обратное распространение ошибки (backpropagation),
     * обновляя веса для минимизации ошибки.
     */
    public void train(double[] input, boolean correctAnswer) {
        if (input.length != inputSize) {
            throw new IllegalArgumentException("Неверный размер входного массива. Ожидается " + inputSize);
        }

        // Преобразуем правильный ответ в double: true -> 1.0, false -> 0.0
        double target = correctAnswer ? 1.0 : 0.0;

        // Прямой проход
        double[] hiddenLayer = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = 0.0;
            for (int i = 0; i < inputSize; i++) {
                sum += input[i] * weightsInputToHidden[i][j];
            }
            hiddenLayer[j] = sigmoid(sum);
        }

        double outputSum = 0.0;
        for (int i = 0; i < hiddenSize; i++) {
            outputSum += hiddenLayer[i] * weightsHiddenToOutput[i][0];
        }
        double output = sigmoid(outputSum);

        // Ошибка выходного слоя (MSE)
        double error = output - target;
        // производная для выхода: (для сигмоиды: output * (1 - output))
        double outputDelta = error * output * (1.0 - output);

        // Рассчитываем дельты для скрытого слоя
        double[] hiddenDeltas = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double w = weightsHiddenToOutput[j][0];
            // производная активации скрытого нейрона
            double derivativeHidden = hiddenLayer[j] * (1.0 - hiddenLayer[j]);
            hiddenDeltas[j] = outputDelta * w * derivativeHidden;
        }

        // Обновление весов скрытый->выход
        for (int j = 0; j < hiddenSize; j++) {
            weightsHiddenToOutput[j][0] -= learningRate * hiddenLayer[j] * outputDelta;
        }

        // Обновление весов вход->скрытый
        for (int i = 0; i < inputSize; i++) {
            double inVal = input[i];
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputToHidden[i][j] -= learningRate * inVal * hiddenDeltas[j];
            }
        }
    }

    /**
     * Сигмоида
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public void saveWeights() {
        LocalDateTime now = LocalDateTime.now();
        String fileName = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + savePostfix + ".nnw";

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(weightsInputToHidden);
            oos.writeObject(weightsHiddenToOutput);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении весов: " + e.getMessage());
        }
    }

    public void loadWeights() {
        String fileName = loadWeightsFileName + savePostfix + ".nnw";
        File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("Файл для загрузки весов не найден: " + fileName);
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj1 = ois.readObject();
            Object obj2 = ois.readObject();

            if (obj1 instanceof double[][] && obj2 instanceof double[][]) {
                double[][] loadedInputToHidden = (double[][]) obj1;
                double[][] loadedHiddenToOutput = (double[][]) obj2;

                if (loadedInputToHidden.length == inputSize
                        && loadedInputToHidden[0].length == hiddenSize
                        && loadedHiddenToOutput.length == hiddenSize
                        && loadedHiddenToOutput[0].length == outputSize) {
                    weightsInputToHidden = loadedInputToHidden;
                    weightsHiddenToOutput = loadedHiddenToOutput;
                } else {
                    System.err.println("Структура загруженных весов не соответствует текущей конфигурации сети.");
                }
            } else {
                System.err.println("Неверный формат данных в файле весов.");
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка при загрузке весов: " + e.getMessage());
        }
    }
}

