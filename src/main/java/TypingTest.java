import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class TypingTest {

    private static String lastInput = "";
    private static Scanner scanner = new Scanner(System.in);

    private static AtomicBoolean inputReceived = new AtomicBoolean(false);
    private static AtomicBoolean timerExpired = new AtomicBoolean(false);

    private static int correctWords = 0;
    private static int incorrectWords = 0;
    private static long startTime;
    private static long endTime;

    static class WordLoader {
        public List<String> loadWordsFromFile(String fileName) {
            List<String> words = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    words.add(line.trim());
                }
            } catch (IOException e) {
                System.err.println("Error reading words from file: " + fileName + " - " + e.getMessage());
                e.printStackTrace();
            }
            return words;
        }
    }


    public static class InputRunnable implements Runnable {
        @Override
        public void run() {
            try {
                lastInput = scanner.nextLine();
                inputReceived.set(true);
            } catch (Exception e) {
            }
        }
    }


    public static void testWord(String wordToTest) {
        try {
            System.out.println("Type: " + wordToTest);
            lastInput = "";
            inputReceived.set(false);
            timerExpired.set(false);

            long calculatedTimeout = wordToTest.length() * 400L;
            if (calculatedTimeout < 1000) {
                calculatedTimeout = 1000;
            }
            final long finalTimeout = calculatedTimeout;

            Thread inputThread = new Thread(new InputRunnable());
            inputThread.start();

            Thread timerThread = new Thread(() -> {
                try {
                    Thread.sleep(finalTimeout);
                    if (!inputReceived.get()) {
                        timerExpired.set(true);
                        inputThread.interrupt();
                    }
                } catch (InterruptedException e) {
                }
            });
            timerThread.start();

            while (!inputReceived.get() && !timerExpired.get()) {
                Thread.sleep(10);
            }

            if (inputReceived.get()) {
                if (timerThread.isAlive()) {
                    timerThread.interrupt();
                }
                inputThread.join();

                System.out.println();
                System.out.println("You typed: " + lastInput);
                if (lastInput.equals(wordToTest)) {
                    System.out.println("Correct!");
                    correctWords++;
                } else {
                    System.out.println("Incorrect!");
                    incorrectWords++;
                }
            } else if (timerExpired.get()) {
                System.out.println("\nTime's up! You didn't type in time.");
                incorrectWords++;
            }
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void typingTest(List<String> wordsForTest) throws InterruptedException {
        correctWords = 0;
        incorrectWords = 0;
        startTime = System.currentTimeMillis();

        for (int i = 0; i < wordsForTest.size(); i++) {
            String wordToTest = wordsForTest.get(i);
            testWord(wordToTest);
        }

        endTime = System.currentTimeMillis();

        System.out.println("\n--- Test Results ---");
        System.out.println("Correct words: " + correctWords);
        System.out.println("Incorrect words: " + incorrectWords);
        long totalTimeMillis = endTime - startTime;
        double totalTimeSeconds = totalTimeMillis / 1000.0;
        System.out.printf("Total time: %.2f seconds%n", totalTimeSeconds);
        if (wordsForTest.size() > 0) {
            double avgTimePerWord = totalTimeSeconds / wordsForTest.size();
            System.out.printf("Average time per word: %.2f seconds%n", avgTimePerWord);
        }
        scanner.close();
    }

    public static void main(String[] args) throws InterruptedException {
        WordLoader wordLoader = new WordLoader();

        List<String> allWords = null;
        try {
            allWords = wordLoader.loadWordsFromFile("Words.txt");
        } catch (Exception e) {
            System.err.println("Failed to load words from Words.txt: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (allWords == null || allWords.isEmpty()) {
            System.out.println("No words loaded from Words.txt. Exiting.");
            return;
        }

        Collections.shuffle(allWords);
        int numberOfWordsToTest = Math.min(10, allWords.size());
        List<String> wordsForTest = allWords.subList(0, numberOfWordsToTest);

        typingTest(wordsForTest);
    }
}