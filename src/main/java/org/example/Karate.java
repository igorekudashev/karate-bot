package org.example;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import lombok.SneakyThrows;
import lombok.val;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Karate implements NativeKeyListener, NativeMouseListener {


    static Color woodColor = new Color(167, 92, 42);
    static Point leftUpPos = new Point(1221, 786);
    static Point leftDownPos = new Point(1241, 844);
    static Point rightDownPos = new Point(1316, 845);
    static Point rightUpPos = new Point(1317, 787);
    static Point endgamePos = new Point(1126, 732);
    static Color endgameWhiteColor = new Color(234, 234, 238);

    private static LinkedList<Boolean> list = new LinkedList<>();
    private static NeuralNetwork nn1 = new NeuralNetwork(180 * 115, 64, 0.02, "_v1");
    private static NeuralNetwork nn2 = new NeuralNetwork(250 * 2, 16, 0.01, "_v2");

    private static final Robot robot;
    private static Point s1 = new Point(1163, 765);
    private static Point s2 = new Point(1343, 880);

    static Point ss1 = new Point(1225, 640);
    static Point ss2 = new Point(1330, 890);
    private static Rectangle captureArea2 = new Rectangle(ss1.x, ss1.y, ss2.x - ss1.x, ss2.y - ss1.y);

    private static Rectangle captureArea = new Rectangle(s1.x, s1.y, s2.x - s1.x, s2.y - s1.y);
    private static final Point right = new Point(1309, 806);
    private static final Point left = new Point(1230, 805);
    private static AtomicBoolean enabled = new AtomicBoolean(false);
    static boolean lastChoice = true;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        nn1.loadWeights();
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        Karate listener = new Karate();
        GlobalScreen.addNativeKeyListener(listener);
        GlobalScreen.addNativeMouseListener(listener);

        System.out.println("STARTED");

        enableBot();
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        switch (e.getKeyCode()) {
            case NativeKeyEvent.VC_P -> System.exit(0);
            case NativeKeyEvent.VC_L -> enabled.set(!enabled.get());
            case NativeKeyEvent.VC_K -> nn2.saveWeights();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // Не требуется обработка
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // Не требуется обработка
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        // Не требуется обработка клика, т.к. мы выводим при нажатии
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        // Проверяем, что нажата левая кнопка мыши
        if (e.getButton() == NativeMouseEvent.BUTTON1) {
//            printCurrentPixelColor();
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        // Не требуется обработка
    }

    private static void printCurrentPixelColor() {
        // Получаем текущую позицию мыши
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
        // Получаем цвет пикселя под курсором
        Color color = robot.getPixelColor(mouseLocation.x, mouseLocation.y);
        // Выводим в консоль
        System.out.println("(" + mouseLocation.x + ", " + mouseLocation.y + "): " + color);
    }

    @SneakyThrows
    private static void wait(int ms) {
        Thread.sleep(ms);
    }

    public static double[] convertImageToGrayscaleArray(BufferedImage image) {
        double[] result = new double[image.getWidth() * image.getHeight()];
        int index = 0;

//        val testImage = new BufferedImage(180, 115, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red   = (rgb >> 16) & 0xFF;
                int green = (rgb >>  8) & 0xFF;
                int blue  = (rgb      ) & 0xFF;

                // Усреднение по каналам
                double gray = (red + green + blue) / 3.0;

//                testImage.setRGB(x, y, (int) gray);

                // Нормируем к 0..1
                double normalized = gray / 255.0;

                result[index++] = normalized;
            }
        }

        return result;
    }

    private static double calc() {
        int correct = 0;
        for (Boolean res : list) {
            if (res) correct++;
        }
        return ((double) correct) / list.size();
    }

    private static void enableBot() {
        while (!enabled.get()) {
        }

        while (true) {
            BufferedImage screenshot1 = robot.createScreenCapture(captureArea);

            Color leftUpColor = robot.getPixelColor(leftUpPos.x, leftUpPos.y);
            Color leftDownColor = robot.getPixelColor(leftDownPos.x, leftDownPos.y);
            Color rightUpColor = robot.getPixelColor(rightUpPos.x, rightUpPos.y);
            Color rightDownColor = robot.getPixelColor(rightDownPos.x, rightDownPos.y);

            BufferedImage screenshot2 = new BufferedImage(2, 250, BufferedImage.TYPE_INT_RGB);
            BufferedImage bigScreenshot = robot.createScreenCapture(captureArea2);
            val graphics = screenshot2.createGraphics();
            graphics.drawImage(bigScreenshot.getSubimage(0, 0, 1, 250), 0, 0, null);
            graphics.drawImage(bigScreenshot.getSubimage(bigScreenshot.getWidth() - 1, 0, 1, 250), 1, 0, null);
            graphics.dispose();

            double[] array2 = convertImageToGrayscaleArray(screenshot2);
            boolean prediction2 = nn2.predict(array2);

            if (leftUpColor.equals(woodColor) || leftDownColor.equals(woodColor)) {
                chooseRight();
            } else if (rightUpColor.equals(woodColor) || rightDownColor.equals(woodColor)) {
                chooseLeft();
            } else {
                System.out.println("NEURAL NETWORK CHOOSING!!!");
                double[] array1 = convertImageToGrayscaleArray(screenshot1);
                boolean prediction1 = nn1.predict(array1);
                lastChoice = prediction1;
                if (prediction1) {
                    chooseLeft();
                } else {
                    chooseRight();
                }
            }

            wait(150);
            Color endgameColor = robot.getPixelColor(endgamePos.x, endgamePos.y);
            Color leftColor = robot.getPixelColor(left.x, left.y);
            boolean isCorrect;
            if (endgameColor.getRed() > 200 && endgameColor.getGreen() < 70 && endgameColor.getBlue() < 70 || leftColor.equals(endgameWhiteColor)) {
                System.out.println("ENDGAME!!!");
//                isCorrect = prediction1 != lastChoice;
                isCorrect = prediction2 != lastChoice;
                wait(3000);
                restartGame();
            } else {
//                isCorrect = prediction1 == lastChoice;
                isCorrect = prediction2 == lastChoice;
            }
//            nn1.train(array1, isCorrect == prediction1);
            nn2.train(array2, isCorrect == prediction2);

            list.addLast(isCorrect);
            if (list.size() > 100) {
                list.removeFirst();
            }
            double total = calc();
//            System.out.println("PREDICTION = " + prediction1 + " | " + (isCorrect ? "CORRECT" : "WRONG") + " | " + total);
            System.out.println("PREDICTION = " + prediction2 + " | " + (isCorrect ? "CORRECT" : "WRONG") + " | " + total);
        }
    }

    private static void restartGame() {
        wait(300);
        robot.mouseMove(1341, 956);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        wait(300);
        robot.mouseMove(1341, 956);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        wait(300);
    }

    private static void chooseLeft() {
        lastChoice = true;

        robot.mouseMove(left.x, left.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static void chooseRight() {
        lastChoice = false;

        robot.mouseMove(right.x, right.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
}
