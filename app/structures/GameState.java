package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.*;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {
    private int currentTurn;
    private HumanPlayer player1;
    private AIController player2;
    private boolean isHumanTurn;
    private boolean gameInitialized;
    private Board board;
    private Set<Tile> highlightedTiles;  // Track highlighted tiles
    private Tile sourceTile; // Track the source tile for highlighting
    private Unit selectedUnit;
    private Card selectedCard;
    private int nextUnitId = 1; 
    

    public GameState() {
        this.currentTurn = 1;
        this.isHumanTurn = true; //start with player's turn
        this.gameInitialized = false; //needs to be initialised
        this.highlightedTiles = new HashSet<>();
        
       
    }
    
   
   public void initializePlayers(HumanPlayer player1, AIController player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1.setAvatar(BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 1, Unit.class));
        this.player2.setAvatar(BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 2, Unit.class));
    } 

   public void setBoard(Board board) {
       this.board = board;
       
       board.placeUnitOnTile(player1.getAvatar(), board.getTile(1, 2));
       board.placeUnitOnTile(player2.getAvatar(), board.getTile(7, 2));
       

   }     

    public void nextTurn() {
        this.isHumanTurn = !this.isHumanTurn;
        if (!isHumanTurn) {
            this.currentTurn++;
        }
        resetHasMovedFlags();
    }

    public void resetHasMovedFlags() {
        for (Unit unit : board.getUnitMap().values()) {
            unit.setHasMoved(false);
        }
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public Player getCurrentPlayer() {
        return isHumanTurn ? player1 : player2;
    }

    public Player getOpponentPlayer() {
        return isHumanTurn ? player2 : player1;
    }

    public void setGameInitialized(boolean initialized) {
        this.gameInitialized = initialized;
    }

    public Board getBoard() {
        return board;
    }

    public void addHighlightedTile(Tile tile) {
        highlightedTiles.add(tile);
    }

    public void clearHighlightedTiles() {
        highlightedTiles.clear();
    }

    public Set<Tile> getHighlightedTiles() {
        return highlightedTiles;
    }

    public boolean isHighlightedTile(Tile tile) {
        return highlightedTiles.contains(tile);
    }

    public Tile getSourceTile() {
        return sourceTile;
    }

    public void setSourceTile(Tile sourceTile) {
        this.sourceTile = sourceTile;
    }

    public Unit getSelectedUnit() {
        return selectedUnit;
    }

    public void setSelectedUnit(Unit selectedUnit) {
        this.selectedUnit = selectedUnit;
    }
    
    public Card getSelectedCard() {
        return selectedCard;
    }

    public void setSelectedCard(Card selectedCard) {
        this.selectedCard = selectedCard;
    }
    
    public int getNextUnitId() {
        return nextUnitId++;
    }
    
    public Unit getCurrentPlayerAvatar() {
        return isHumanTurn ? player1.getAvatar() : player2.getAvatar();
    }
    
    public void clearAllHighlights(ActorRef out) {
        for (Tile tile : highlightedTiles) {
            BasicCommands.drawTile(out, tile, 0); // Reset highlight (mode = 0)
        }
        highlightedTiles.clear(); // Clear the set of highlighted tiles
    }


    public List<Tile> getTilesOccupiedByCurrentPlayer() {
        List<Tile> occupiedTiles = new ArrayList<>();
        for (Map.Entry<Tile, Unit> entry : board.getUnitMap().entrySet()) {
            if (entry.getValue().getOwner() == getCurrentPlayer()) {
                occupiedTiles.add(entry.getKey());
            }
        }
        return occupiedTiles;
    }
}
