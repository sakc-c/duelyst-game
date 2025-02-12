package structures;

import structures.basic.Player;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {
    private int currentTurn;  // Current turn number
    private Player currentPlayer;  // Player whose turn it is
    private Player opponentPlayer;  // The opponent player

    public GameState() {
        this.currentTurn = 1;  // Start the game on turn 1
        this.currentPlayer = null;  // Initialize event handler to set up
        this.opponentPlayer = null;  // Initialize event handler to set up
    }

    // Method to initialize the players for the first time
    public void initializePlayers(Player player1, Player player2) {
        this.currentPlayer = player1;  // Set the current player
        this.opponentPlayer = player2;  // Set the opponent player
    }

    public void nextTurn() {
        this.currentTurn++;  // Increment the turn number

        // Swap current player and opponent player
        Player temp = this.currentPlayer;
        this.currentPlayer = this.opponentPlayer;
        this.opponentPlayer = temp;
    }

    // Getter for the current turn number
    public int getCurrentTurn() {
        return currentTurn;
    }

    // Getter for the current player
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    // Getter for the opponent player
    public Player getOpponentPlayer() {
        return opponentPlayer;
    }
}
