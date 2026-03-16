import java.util.*;
import java.io.*;

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
            System.out.println("4. Show Leaderboard");
            System.out.println("5. Refill Lives");
            System.out.println("6. Export Leaderboard");
            System.out.println("7. Exit");

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
                    showLeaderboard();
                    break;

                case 5:
                    refillLives();
                    break;

                case 6:
                    exportLeaderboard();
                    break;

                case 7:
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

        System.out.print("Enter score: ");
        int score = sc.nextInt();

        if (level < 1 || level > 99) {
            System.out.println("Invalid level.");
            return;
        }

        int previousScore = currentPlayer.levelScores[level];

        currentPlayer.undoStack.push(new Move(level, previousScore));

        currentPlayer.levelScores[level] = score;

        if (level > currentPlayer.maxLevel)
            currentPlayer.maxLevel = level;

        currentPlayer.lives--;

        saveScoreToCSV(currentPlayer.name, level, score);

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

    static void saveScoreToCSV(String name, int level, int score) {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("scores.csv", true))) {

            bw.write(name + "," + level + "," + score);
            bw.newLine();

        } catch (IOException e) {
            System.out.println("Error writing file.");
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

    static Map<String, Integer> calculateTotals() {

        Map<String, Integer> totals = new HashMap<>();

        for (Player p : players.values()) {

            int total = 0;

            for (int i = 1; i <= p.maxLevel; i++)
                total += p.levelScores[i];

            totals.put(p.name, total);
        }

        return totals;
    }

    static void showLeaderboard() {

        Map<String, Integer> totals = calculateTotals();

        if (totals.isEmpty()) {
            System.out.println("No scores yet.");
            return;
        }

        List<Map.Entry<String, Integer>> list =
                new ArrayList<>(totals.entrySet());

        list.sort((a, b) -> b.getValue() - a.getValue());

        System.out.println("\n===== LEADERBOARD =====");

        int rank = 1;

        for (Map.Entry<String, Integer> entry : list) {

            System.out.println(rank + ". " + entry.getKey()
                    + "  Total Score: " + entry.getValue());

            rank++;
        }
    }

    static void exportLeaderboard() {

        Map<String, Integer> totals = calculateTotals();

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
}