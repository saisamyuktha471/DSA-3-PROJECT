import java.io.*;
import java.util.*;

public class IntelligentSpellChecker {

    // =========================================================
    // TRIE NODE
    // =========================================================

    static class TrieNode {

        TrieNode[] children = new TrieNode[26];

        boolean isWord = false;
    }

    // =========================================================
    // TRIE
    // =========================================================

    static class Trie {

        TrieNode root = new TrieNode();

        // Insert a word into Trie
        void insert(String word) {

            TrieNode current = root;

            for (char ch : word.toCharArray()) {

                if (ch < 'a' || ch > 'z') {
                    continue;
                }

                int index = ch - 'a';

                if (current.children[index] == null) {

                    current.children[index] =
                            new TrieNode();
                }

                current =
                        current.children[index];
            }

            current.isWord = true;
        }

        // Search a word in Trie
        boolean search(String word) {

            TrieNode current = root;

            for (char ch : word.toCharArray()) {

                if (ch < 'a' || ch > 'z') {
                    return false;
                }

                int index = ch - 'a';

                if (current.children[index] == null) {
                    return false;
                }

                current =
                        current.children[index];
            }

            return current.isWord;
        }
    }

    // =========================================================
    // EDIT RESULT
    // =========================================================

    static class EditResult {

        int distance;

        List<String> operations;

        EditResult(
                int distance,
                List<String> operations) {

            this.distance = distance;
            this.operations = operations;
        }
    }

    // =========================================================
    // GLOBAL DATA
    // =========================================================

    static Set<String> dictionary =
            new HashSet<>();

    static Trie dictionaryTrie =
            new Trie();

    static Map<Integer, List<String>>
            lengthBuckets =
            new HashMap<>();

    static int totalWords = 0;

    static int incorrectWords = 0;

    static int correctionsMade = 0;

    // =========================================================
    // LEVENSHTEIN EDIT DISTANCE
    // =========================================================

    static EditResult levenshteinDistance(
            String source,
            String target) {

        int m = source.length();

        int n = target.length();

        int[][] dp =
                new int[m + 1][n + 1];

        // Base case for source
        for (int i = 0; i <= m; i++) {

            dp[i][0] = i;
        }

        // Base case for target
        for (int j = 0; j <= n; j++) {

            dp[0][j] = j;
        }

        // Dynamic Programming table
        for (int i = 1; i <= m; i++) {

            for (int j = 1; j <= n; j++) {

                // Characters are equal
                if (source.charAt(i - 1)
                        == target.charAt(j - 1)) {

                    dp[i][j] =
                            dp[i - 1][j - 1];

                } else {

                    // Insert a character
                    int insertion =
                            dp[i][j - 1];

                    // Delete a character
                    int deletion =
                            dp[i - 1][j];

                    // Replace a character
                    int substitution =
                            dp[i - 1][j - 1];

                    dp[i][j] =
                            1 + Math.min(
                                    insertion,
                                    Math.min(
                                            deletion,
                                            substitution
                                    )
                            );
                }
            }
        }

        // =====================================================
        // BACKTRACKING
        // =====================================================

        List<String> operations =
                new ArrayList<>();

        int i = m;

        int j = n;

        while (i > 0 || j > 0) {

            // Characters match
            if (i > 0 &&
                    j > 0 &&
                    source.charAt(i - 1)
                            == target.charAt(j - 1)) {

                i--;

                j--;
            }

            // Substitution
            else if (i > 0 &&
                    j > 0 &&
                    dp[i][j]
                            == dp[i - 1][j - 1] + 1) {

                operations.add(
                        "Substitute '" +
                                source.charAt(i - 1) +
                                "' with '" +
                                target.charAt(j - 1) +
                                "'"
                );

                i--;

                j--;
            }

            // Insertion
            else if (j > 0 &&
                    dp[i][j]
                            == dp[i][j - 1] + 1) {

                operations.add(
                        "Insert '" +
                                target.charAt(j - 1) +
                                "'"
                );

                j--;
            }

            // Deletion
            else if (i > 0 &&
                    dp[i][j]
                            == dp[i - 1][j] + 1) {

                operations.add(
                        "Delete '" +
                                source.charAt(i - 1) +
                                "'"
                );

                i--;
            }
        }

        Collections.reverse(operations);

        return new EditResult(
                dp[m][n],
                operations
        );
    }

    // =========================================================
    // MAXIMUM EDIT DISTANCE
    // =========================================================

    static int getMaximumDistance(
            String word) {

        int length =
                word.length();

        if (length <= 4) {

            return 1;
        }

        if (length <= 7) {

            return 2;
        }

        return 3;
    }

    // =========================================================
    // LOAD DICTIONARY
    // =========================================================

    static boolean loadDictionary(
            String path) {

        dictionary.clear();

        lengthBuckets.clear();

        dictionaryTrie =
                new Trie();

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(path)
                    );

            String word;

            while ((word =
                    reader.readLine()) != null) {

                word =
                        word.trim()
                                .toLowerCase();

                if (word.isEmpty()) {

                    continue;
                }

                if (!word.matches(
                        "[a-z]+")) {

                    continue;
                }

                dictionary.add(word);

                dictionaryTrie.insert(word);

                int length =
                        word.length();

                lengthBuckets
                        .computeIfAbsent(
                                length,
                                k -> new ArrayList<>()
                        )
                        .add(word);
            }

            reader.close();

            return !dictionary.isEmpty();

        } catch (IOException e) {

            System.out.println();

            System.out.println(
                    "Error reading dictionary."
            );

            return false;
        }
    }

    // =========================================================
    // FIND BEST CORRECTION
    // =========================================================

    static String findBestCorrection(
            String word) {

        int maximumDistance =
                getMaximumDistance(word);

        String bestWord = null;

        int bestDistance =
                Integer.MAX_VALUE;

        /*
         * Only dictionary words with similar
         * lengths are checked.
         */

        int minimumLength =
                Math.max(
                        1,
                        word.length()
                                - maximumDistance
                );

        int maximumLength =
                word.length()
                        + maximumDistance;

        for (int length =
                     minimumLength;

             length <= maximumLength;

             length++) {

            List<String> candidates =
                    lengthBuckets.get(length);

            if (candidates == null) {

                continue;
            }

            for (String candidate :
                    candidates) {

                EditResult result =
                        levenshteinDistance(
                                word,
                                candidate
                        );

                if (result.distance
                        < bestDistance) {

                    bestDistance =
                            result.distance;

                    bestWord =
                            candidate;
                }
            }
        }

        if (bestDistance
                <= maximumDistance) {

            return bestWord;
        }

        return null;
    }

    // =========================================================
    // PRESERVE CAPITALIZATION
    // =========================================================

    static String preserveCase(
            String original,
            String corrected) {

        if (original.isEmpty()) {

            return corrected;
        }

        // Example:
        // LIBARY -> LIBRARY

        if (original.equals(
                original.toUpperCase())) {

            return corrected.toUpperCase();
        }

        // Example:
        // Libary -> Library

        if (Character.isUpperCase(
                original.charAt(0))) {

            return Character.toUpperCase(
                    corrected.charAt(0)
            )
                    + corrected.substring(1);
        }

        return corrected;
    }

    // =========================================================
    // CHECK WHETHER TOKEN IS A WORD
    // =========================================================

    static boolean isWord(
            String token) {

        return token.matches(
                "[a-zA-Z]+"
        );
    }

    // =========================================================
    // PROCESS DOCUMENT
    // =========================================================

    static String processDocument(
            String content) {

        StringBuilder result =
                new StringBuilder();

        /*
         * Split the document into words
         * and non-word characters.
         *
         * This allows us to preserve:
         * spaces
         * commas
         * full stops
         * question marks
         * etc.
         */

        String[] tokens =
                content.split(
                        "(?<=\\W)|(?=\\W)"
                );

        totalWords = 0;

        incorrectWords = 0;

        correctionsMade = 0;

        System.out.println();

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "              DOCUMENT ANALYSIS"
        );

        System.out.println(
                "=============================================="
        );

        for (String token :
                tokens) {

            // Keep punctuation and spaces
            if (!isWord(token)) {

                result.append(token);

                continue;
            }

            totalWords++;

            String lowerWord =
                    token.toLowerCase();

            // =================================================
            // WORD EXISTS IN DICTIONARY
            // =================================================

            if (dictionaryTrie.search(
                    lowerWord)) {

                result.append(token);

                continue;
            }

            // =================================================
            // WORD NOT FOUND
            // =================================================

            incorrectWords++;

            String correction =
                    findBestCorrection(
                            lowerWord
                    );

            System.out.println();

            System.out.println(
                    "----------------------------------------------"
            );

            System.out.println(
                    "Incorrect Word : " +
                            token
            );

            // =================================================
            // NO SUITABLE CORRECTION
            // =================================================

            if (correction == null) {

                System.out.println(
                        "Suggested Word : " +
                                "No suitable correction"
                );

                System.out.println(
                        "Action         : " +
                                "Word kept unchanged"
                );

                result.append(token);

                continue;
            }

            // =================================================
            // CALCULATE EDIT OPERATIONS
            // =================================================

            EditResult editResult =
                    levenshteinDistance(
                            lowerWord,
                            correction
                    );

            String correctedWord =
                    preserveCase(
                            token,
                            correction
                    );

            System.out.println(
                    "Suggested Word : " +
                            correctedWord
            );

            System.out.println(
                    "Edit Distance  : " +
                            editResult.distance
            );

            System.out.println(
                    "Operations:"
            );

            for (String operation :
                    editResult.operations) {

                System.out.println(
                        "  - " +
                                operation
                );
            }

            // =================================================
            // APPLY CORRECTION
            // =================================================

            result.append(
                    correctedWord
            );

            correctionsMade++;
        }

        System.out.println();

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "               ANALYSIS COMPLETE"
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "Total Words       : " +
                        totalWords
        );

        System.out.println(
                "Incorrect Words   : " +
                        incorrectWords
        );

        System.out.println(
                "Corrections Made  : " +
                        correctionsMade
        );

        System.out.println(
                "=============================================="
        );

        return result.toString();
    }

    // =========================================================
    // READ DOCUMENT
    // =========================================================

    static String readDocument(
            String path) {

        StringBuilder content =
                new StringBuilder();

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(path)
                    );

            String line;

            while ((line =
                    reader.readLine()) != null) {

                content.append(line);

                content.append(
                        System.lineSeparator()
                );
            }

            reader.close();

        } catch (IOException e) {

            return null;
        }

        return content.toString();
    }

    // =========================================================
    // SAVE CORRECTED DOCUMENT
    // =========================================================

    static boolean saveDocument(
            String path,
            String content) {

        try {

            BufferedWriter writer =
                    new BufferedWriter(
                            new FileWriter(path)
                    );

            writer.write(content);

            writer.close();

            return true;

        } catch (IOException e) {

            return false;
        }
    }

    // =========================================================
    // DISPLAY STATISTICS
    // =========================================================

    static void displayStatistics() {

        System.out.println();

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "              PROJECT STATISTICS"
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "Total Words       : " +
                        totalWords
        );

        System.out.println(
                "Incorrect Words   : " +
                        incorrectWords
        );

        System.out.println(
                "Corrections Made  : " +
                        correctionsMade
        );

        if (totalWords > 0) {

            double correctPercentage =
                    ((double)
                            (totalWords
                                    - incorrectWords)
                                    / totalWords)
                            * 100;

            System.out.printf(
                    "Correct Words %%   : %.2f%%%n",
                    correctPercentage
            );
        }

        System.out.println(
                "=============================================="
        );
    }

    // =========================================================
    // CHECK DOCUMENT
    // =========================================================

    static void checkDocument(
            Scanner scanner) {

        System.out.println();

        System.out.println(
                "Enter the path of the document:"
        );

        String documentPath =
                scanner.nextLine().trim();

        File document =
                new File(documentPath);

        if (!document.exists()) {

            System.out.println();

            System.out.println(
                    "Document not found."
            );

            return;
        }

        System.out.println();

        System.out.println(
                "Enter the path of dictionary.txt:"
        );

        String dictionaryPath =
                scanner.nextLine().trim();

        if (!loadDictionary(
                dictionaryPath)) {

            System.out.println();

            System.out.println(
                    "Dictionary is empty " +
                            "or could not be loaded."
            );

            return;
        }

        System.out.println();

        System.out.println(
                "Dictionary loaded successfully."
        );

        System.out.println(
                "Dictionary Words : " +
                        dictionary.size()
        );

        String content =
                readDocument(
                        documentPath
                );

        if (content == null) {

            System.out.println();

            System.out.println(
                    "Error reading document."
            );

            return;
        }

        System.out.println();

        System.out.println(
                "Document loaded successfully."
        );

        String correctedContent =
                processDocument(
                        content
                );

        // =====================================================
        // CREATE OUTPUT FILE NAME
        // =====================================================

        String parent =
                document.getParent();

        String outputPath;

        if (parent != null) {

            outputPath =
                    parent
                            + File.separator
                            + "corrected_"
                            + document.getName();

        } else {

            outputPath =
                    "corrected_"
                            + document.getName();
        }

        // =====================================================
        // SAVE OUTPUT
        // =====================================================

        if (saveDocument(
                outputPath,
                correctedContent)) {

            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "          CORRECTED DOCUMENT CREATED"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println();

            System.out.println(
                    "Saved at:"
            );

            System.out.println(
                    outputPath
            );

            System.out.println();

        } else {

            System.out.println();

            System.out.println(
                    "Error creating corrected document."
            );
        }
    }

    // =========================================================
    // MAIN MENU
    // =========================================================

    public static void main(
            String[] args) {

        Scanner scanner =
                new Scanner(System.in);

        while (true) {

            System.out.println();

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "          INTELLIGENT SPELL CHECKER"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "1. Check Document"
            );

            System.out.println(
                    "2. View Statistics"
            );

            System.out.println(
                    "3. Exit"
            );

            System.out.println(
                    "=============================================="
            );

            System.out.print(
                    "Enter your choice: "
            );

            String choice =
                    scanner.nextLine();

            switch (choice) {

                case "1":

                    checkDocument(
                            scanner
                    );

                    break;

                case "2":

                    displayStatistics();

                    break;

                case "3":

                    System.out.println();

                    System.out.println(
                            "Thank you for using " +
                                    "Intelligent Spell Checker!"
                    );

                    scanner.close();

                    return;

                default:

                    System.out.println();

                    System.out.println(
                            "Invalid choice. " +
                                    "Please try again."
                    );
            }
        }
    }
}