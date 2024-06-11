import java.sql.*;
import java.util.Scanner;

public class TicTacToe {
    private static final char EMPTY = ' ';
    private static final char PLAYER_X = 'X';
    private static final char PLAYER_O = 'O';
    private static final String RESET_COLOR = "\u001B[0m";
    private static final String COLOR_X = "\u001B[31m"; // Red
    private static final String COLOR_O = "\u001B[34m"; // Blue

    private static final String DB_URL = "jdbc:mysql://localhost:3306/oefen_tic_tac";
    private static final String DB_USER = "root"; // Verander dit naar jouw MySQL-gebruikersnaam
    private static final String DB_PASSWORD = "root"; // Verander dit naar jouw MySQL-wachtwoord

    private static int playerXId;
    private static int playerOId;

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Scanner scanner = new Scanner(System.in);

            System.out.println("Player X:");
            playerXId = authenticateUser(connection, scanner);

            System.out.println("Player O:");
            playerOId = authenticateUser(connection, scanner);

            boolean playAgain;
            do {
                playGame(connection, scanner);
                System.out.println("Do you want to play again? (yes/no): ");
                String response = scanner.next();
                playAgain = response.equalsIgnoreCase("yes");
            } while (playAgain);

            scanner.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static int authenticateUser(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("1. Register");
        System.out.println("2. Login");
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        if (choice == 1) {
            return registerUser(connection, scanner);
        } else if (choice == 2) {
            return loginUser(connection, scanner);
        } else {
            System.out.println("Invalid choice. Exiting.");
            System.exit(1);
            return -1; // Will never be reached
        }
    }

    private static int registerUser(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                int userId = keys.getInt(1);
                System.out.println("User registered successfully.");
                return userId;
            } else {
                throw new SQLException("User ID retrieval failed.");
            }
        } catch (SQLException e) {
            System.out.println("Registration failed. Username might already exist.");
            throw e;
        }
    }

    private static int loginUser(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String query = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int userId = resultSet.getInt("id");
                System.out.println("Login successful.");
                return userId;
            } else {
                System.out.println("Login failed. Check your username and password.");
                throw new SQLException("Invalid login credentials.");
            }
        }
    }

    private static void playGame(Connection connection, Scanner scanner) throws SQLException {
        char[][] board = {
                {EMPTY, EMPTY, EMPTY},
                {EMPTY, EMPTY, EMPTY},
                {EMPTY, EMPTY, EMPTY}
        };
        char currentPlayer = PLAYER_X;
        int currentUserId = playerXId;
        boolean gameWon = false;

        while (!gameWon && !isBoardFull(board)) {
            printBoard(board);
            System.out.println("Player " + currentPlayer + ", enter your move (1-9): ");
            int move = scanner.nextInt();

            int row = (move - 1) / 3;
            int col = (move - 1) % 3;

            if (move >= 1 && move <= 9 && board[row][col] == EMPTY) {
                board[row][col] = currentPlayer;
                gameWon = checkWin(board, currentPlayer);
                currentPlayer = (currentPlayer == PLAYER_X) ? PLAYER_O : PLAYER_X;
                currentUserId = (currentUserId == playerXId) ? playerOId : playerXId;
            } else {
                System.out.println("This move is not valid");
            }
        }

        printBoard(board);
        char winner = (currentPlayer == PLAYER_X) ? PLAYER_O : PLAYER_X;
        int winningUserId = (winner == PLAYER_X) ? playerXId : playerOId;
        if (gameWon) {
            System.out.println("Player " + winner + " wins!");
        } else {
            System.out.println("The game is a draw!");
            winner = 'D'; // Indicate a draw
            winningUserId = -1; // Indicate no winner
        }

        saveGameResult(connection, winner, winningUserId);
    }

    private static void printBoard(char[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                char cell = board[i][j];
                if (cell == PLAYER_X) {
                    System.out.print(COLOR_X + cell + RESET_COLOR);
                } else if (cell == PLAYER_O) {
                    System.out.print(COLOR_O + cell + RESET_COLOR);
                } else {
                    System.out.print(cell);
                }
                if (j < 2) System.out.print("|");
            }
            System.out.println();
            if (i < 2) System.out.println("-----");
        }
    }

    private static boolean checkWin(char[][] board, char player) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                return true;
            }
        }

        // Check columns
        for (int j = 0; j < 3; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) {
                return true;
            }
        }

        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return true;
        }

        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return true;
        }

        return false;
    }

    private static boolean isBoardFull(char[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void saveGameResult(Connection connection, char winner, int winningUserId) throws SQLException {
        String query = "INSERT INTO game_results (winner, user_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, String.valueOf(winner));
            if (winningUserId == -1) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, winningUserId);
            }
            statement.executeUpdate();
            System.out.println("Game result saved to database.");
        }
    }
}