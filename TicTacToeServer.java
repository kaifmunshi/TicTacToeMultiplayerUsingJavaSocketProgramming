import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
public class TicTacToeServer {

    public static void main(String[] args) throws Exception {
        ServerSocket sc = new ServerSocket(9000);       // Creating a server socket on port number 9000
        System.out.println("Tic Tac Toe Server has Started");
    
    
        try {
            while (true) {
                Game game = new Game();
                Game.Player playerX = game.new Player(sc.accept(), 'X');   // Inner class object player 
                System.out.println("[+]PLayer 1 Connected!");
                Game.Player playerO = game.new Player(sc.accept(), 'O');    // passing sockets to constructor
                System.out.println("[+]PLayer 2 Connected!");
                // sc.accept() is blocking code 
                playerX.setOpponent(playerO); 
                playerO.setOpponent(playerX);
                game.currentPlayer = playerX; // denotes whos change it is 
                playerX.start();
                playerO.start();
            }
        } finally {
            sc.close();
        }
    }
}

class Game {
    // a board of 9 squares
    private Player[] board = {
        null, null, null,
        null, null, null,
        null, null, null};

    //current player
    Player currentPlayer;

    // checks for the winner
    public boolean hasWinner() {
        return
            (board[0] != null && board[0] == board[1] && board[0] == board[2])
          ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
          ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
          ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
          ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
          ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
          ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
          ||(board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }

    // check for no empty squares
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }
    // thread when player tries a move
    // Shared method 
    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;   // change the chance if x then 0 if 0 then x 
            currentPlayer.otherPlayerMoved(location);
            return true;
        }
        return false;
    }
    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;
        // thread handler to initialize stream fields
        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                InputStreamReader ir = new InputStreamReader(socket.getInputStream());
                input = new BufferedReader(ir);
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark);
                output.println("MESSAGE Waiting for opponent to connect"); // sent on client 
            } catch (IOException e) {
                System.out.println("Player left the game: " + e);
            }
        }
        //sets the opponent for this player
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        
         //Handles the otherPlayerMoved message. 
        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            output.println(
                hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
        }
    
        public void run() {
            try {
                // The thread is only started after everyone connects.
                output.println("MESSAGE Both players connected");

                // Tell the first player that it is his turn.
                if (mark == 'X') {
                    output.println("MESSAGE Your move");
                }

                //  get commands from the client and process them.
                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5)); // after MOVE for from index 5 
                        // legal move for current player 
                        if (legalMove(location, this)) {
                            output.println("VALID_MOVE");
                            output.println(hasWinner() ? "VICTORY"
                                         : boardFilledUp() ? "TIE"
                                         : "");
                        } else {
                            output.println("MESSAGE Invalid Input!");
                        }
                    } else if (command.startsWith("QUIT")) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Player left the game: " + e);
            } finally {
                try {socket.close();} catch (IOException e) {}
            }
        }
    }
}
