import java.io.*;
import java.util.*;

class Move {
    int level;
    int previousScore;

    Move(int level, int previousScore) {
        this.level = level;
        this.previousScore = previousScore;
    }
}

class NodeStack {
    Move data;
    NodeStack next;

    NodeStack(Move data) {
        this.data = data;
    }
}

class LinkedStack {
    private NodeStack top;

    public void push(Move data) {
        NodeStack node = new NodeStack(data);
        node.next = top;
        top = node;
    }

    public Move pop() {
        if (top == null) return null;
        Move value = top.data;
        top = top.next;
        return value;
    }

    public boolean isEmpty() {
        return top == null;
    }
}

class Player {
    String name;
    int lives;
    static final int MAX_LIVES = 5;
    int maxLevel;
    int[] levelScores;
    LinkedStack undoStack;

    Player(String name) {
        this.name = name;
        this.lives = MAX_LIVES;
        this.maxLevel = 0;
        this.levelScores = new int[100];
        this.undoStack = new LinkedStack();
    }
}

public class CandyCrushGame {

    static Scanner sc = new Scanner(System.in);
    static Map<String, Player> players = new LinkedHashMap<>();
    static Player currentPlayer;
    static Random rand = new Random();

    public static void main(String[] args) {

        loadScoresFromCSV();

        while (true) {

            System.out.println("\n===== Candy Crush Challenge =====");

            if (currentPlayer != null) {
                System.out.println("Current Player: " + currentPlayer.name);
                System.out.println("Lives: " + currentPlayer.lives);
                System.out.println("Max Level: " + currentPlayer.maxLevel);
            } else {
                System.out.println("No player selected.");
            }

            System.out.println("1. Start / Switch Player");
            System.out.println("2. Complete Level");
            System.out.println("3. Undo Last Move");
            System.out.println("4. Show & Export Leaderboard");
            System.out.println("5. Refill Lives");
            System.out.println("6. List User Scores by Level");
            System.out.println("0. Exit");

            System.out.print("Choose option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    startOrSwitchPlayer();
                    break;

                case 2:
                    completeLevel();
                    break;

                case 3:
                    undoMove();
                    break;

                case 4:
                    // combined: show + export leaderboard
                    showLeaderboard();
                    exportLeaderboard();
                    break;

                case 5:
                    refillLives();
                    break;

                case 6:
                    listUserScoresByLevel();
                    break;

                case 0:
                    System.out.println("Game exited.");
                    return;

                default:
                    System.out.println("Invalid option");
            }
        }
    }

    static void startOrSwitchPlayer() {

        System.out.print("Enter player name: ");
        String name = sc.nextLine().trim();

        if (!players.containsKey(name)) {
            players.put(name, new Player(name));
            System.out.println("New player created.");
        } else {
            System.out.println("Existing player loaded.");
        }

        currentPlayer = players.get(name);
    }

    static void completeLevel() {

        if (currentPlayer == null) {
            System.out.println("No player selected.");
            return;
        }

        if (currentPlayer.lives <= 0) {
            System.out.println("No lives left.");
            return;
        }

        System.out.print("Enter level number: ");
        int level = sc.nextInt();
        sc.nextLine(); // consume newline

        if (level < 1 || level > 99) {
            System.out.println("Invalid level.");
            return;
        }

        // Random score between 100 and 1000
        int score = rand.nextInt(901) + 100;
        System.out.println("Random score generated for level " + level + ": " + score);

        int previousScore = currentPlayer.levelScores[level];

        // Push previous best score for undo
        currentPlayer.undoStack.push(new Move(level, previousScore));

        // Update in-memory best score
        if (score > currentPlayer.levelScores[level]) {
            currentPlayer.levelScores[level] = score;
        }

        if (level > currentPlayer.maxLevel)
            currentPlayer.maxLevel = level;

        currentPlayer.lives--;

        // Save only BEST score for that player+level into scores.csv
        saveScoreToCSV(currentPlayer.name, level, currentPlayer.levelScores[level]);

        System.out.println("Level completed.");
        System.out.println("Lives remaining: " + currentPlayer.lives);
    }

    static void undoMove() {

        if (currentPlayer == null) {
            System.out.println("No player selected.");
            return;
        }

        if (currentPlayer.undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }

        Move lastMove = currentPlayer.undoStack.pop();

        currentPlayer.levelScores[lastMove.level] = lastMove.previousScore;

        if (currentPlayer.lives < Player.MAX_LIVES)
            currentPlayer.lives++;

        // Note: scores.csv is not rewritten on undo
        System.out.println("Move undone.");
    }

    static void refillLives() {

        if (currentPlayer == null) {
            System.out.println("No player selected.");
            return;
        }

        currentPlayer.lives = Player.MAX_LIVES;

        System.out.println("Lives refilled.");
    }

    // scores.csv contains only best score per player+level
    static void saveScoreToCSV(String name, int level, int score) {

        Map<String, Integer> bestPerLevel = new LinkedHashMap<>();
        File file = new File("scores.csv");

        // Load existing best scores
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {

                String line;
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    if (data.length < 3) continue;

                    String existingName = data[0];
                    int existingLevel;
                    int existingScore;
                    try {
                        existingLevel = Integer.parseInt(data[1]);
                        existingScore = Integer.parseInt(data[2]);
                    } catch (NumberFormatException e) {
                        continue;
                    }

                    String key = existingName + "#" + existingLevel;
                    Integer currentBest = bestPerLevel.get(key);
                    if (currentBest == null || existingScore > currentBest) {
                        bestPerLevel.put(key, existingScore);
                    }
                }

            } catch (IOException e) {
                System.out.println("Error reading scores.csv.");
            }
        }

        // Update with new best for this player+level
        String newKey = name + "#" + level;
        Integer currentBestForLevel = bestPerLevel.get(newKey);
        if (currentBestForLevel == null || score > currentBestForLevel) {
            bestPerLevel.put(newKey, score);
        }

        // Rewrite scores.csv with one row per player+level, best score only
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {

            for (Map.Entry<String, Integer> entry : bestPerLevel.entrySet()) {
                String key = entry.getKey();
                int bestScore = entry.getValue();

                int hashIndex = key.indexOf('#');
                String playerName = key.substring(0, hashIndex);
                int lvl = Integer.parseInt(key.substring(hashIndex + 1));

                bw.write(playerName + "," + lvl + "," + bestScore);
                bw.newLine();
            }

        } catch (IOException e) {
            System.out.println("Error writing scores.csv.");
        }
    }

    static void loadScoresFromCSV() {

        File file = new File("scores.csv");

        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");

                if (data.length < 3) continue;

                String name = data[0];
                int level = Integer.parseInt(data[1]);
                int score = Integer.parseInt(data[2]);

                players.putIfAbsent(name, new Player(name));

                Player p = players.get(name);

                if (score > p.levelScores[level])
                    p.levelScores[level] = score;

                if (level > p.maxLevel)
                    p.maxLevel = level;
            }

            System.out.println("Saved data loaded.");

        } catch (Exception e) {
            System.out.println("Error loading scores.");
        }
    }

    // calculate totals based on best score per level from scores.csv
    static Map<String, Integer> calculateTotalsFromCSV() {

        Map<String, Integer> totals = new HashMap<>();

        File file = new File("scores.csv");
        if (!file.exists()) {
            return totals; // empty
        }

        // Key: playerName + "#" + level, Value: bestScore for that level
        Map<String, Integer> bestPerLevel = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = br.readLine()) != null) {

                String[] data = line.split(",");
                if (data.length < 3) continue;

                String name = data[0];
                int level;
                int score;
                try {
                    level = Integer.parseInt(data[1]);
                    score = Integer.parseInt(data[2]);
                } catch (NumberFormatException e) {
                    continue;
                }

                String key = name + "#" + level;

                Integer currentBest = bestPerLevel.get(key);
                if (currentBest == null || score > currentBest) {
                    bestPerLevel.put(key, score);
                }
            }

        } catch (IOException e) {
            System.out.println("Error reading scores.csv for totals.");
            return totals;
        }

        // Sum best score of each level per player
        for (Map.Entry<String, Integer> entry : bestPerLevel.entrySet()) {
            String key = entry.getKey();
            int bestScore = entry.getValue();

            int hashIndex = key.indexOf('#');
            String name = key.substring(0, hashIndex);

            int currentTotal = totals.getOrDefault(name, 0);
            totals.put(name, currentTotal + bestScore);
        }

        return totals;
    }

    // leaderboard uses best score per level from CSV
    static void showLeaderboard() {

        Map<String, Integer> totals = calculateTotalsFromCSV();

        if (totals.isEmpty()) {
            System.out.println("No scores yet.");
            return;
        }

        List<Map.Entry<String, Integer>> list =
                new ArrayList<>(totals.entrySet());

        list.sort((a, b) -> b.getValue() - a.getValue());

        System.out.println("\n===== LEADERBOARD (Best Score Per Level) =====");

        int rank = 1;

        for (Map.Entry<String, Integer> entry : list) {

            System.out.println(rank + ". " + entry.getKey()
                    + "  Total Score: " + entry.getValue());

            rank++;
        }
    }

    static void exportLeaderboard() {

        Map<String, Integer> totals = calculateTotalsFromCSV();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("leaderboard.csv"))) {

            bw.write("Rank,Player,TotalScore");
            bw.newLine();

            List<Map.Entry<String, Integer>> list =
                    new ArrayList<>(totals.entrySet());

            list.sort((a, b) -> b.getValue() - a.getValue());

            int rank = 1;

            for (Map.Entry<String, Integer> entry : list) {

                bw.write(rank + "," + entry.getKey() + "," + entry.getValue());
                bw.newLine();
                rank++;
            }

            System.out.println("Leaderboard exported.");

        } catch (Exception e) {
            System.out.println("Export error.");
        }
    }

    // Show all players’ scores per level (from in-memory best scores)
    static void listUserScoresByLevel() {

        if (players.isEmpty()) {
            System.out.println("No players found.");
            return;
        }

        System.out.println("\n===== USERS SCORES BY LEVEL =====");

        for (Player p : players.values()) {
            System.out.println("\nPlayer: " + p.name);
            System.out.println("Max Level: " + p.maxLevel);
            System.out.println("Level -> BestScore");

            boolean hasAnyScore = false;

            for (int level = 1; level <= p.maxLevel; level++) {
                int score = p.levelScores[level];
                if (score > 0) {
                    hasAnyScore = true;
                    System.out.println("  " + level + " -> " + score);
                }
            }

            if (!hasAnyScore) {
                System.out.println("  (no scores yet)");
            }
        }
    }
}