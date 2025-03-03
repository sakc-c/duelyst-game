package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 *
 * @author Dr. Richard McCreadie
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
    private int nextUnitId = 2;


    public GameState() {
        this.currentTurn = 1;
        this.isHumanTurn = true; //start with player's turn
        this.gameInitialized = false; //needs to be initialised
        this.highlightedTiles = new HashSet<>();


    }


    public void initializePlayers(HumanPlayer player1, AIController player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.player1.setAvatar(BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, getNextUnitId(), Unit.class));
        this.player2.setAvatar(BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, getNextUnitId(), Unit.class));
    }

    public void setBoard(GameState gameState, ActorRef out, Board board) {
        this.board = board;

        Unit player1Avatar = player1.getAvatar();
        Tile tile1 = board.getTile(1, 2);
        player1Avatar.setPositionByTile(tile1);
        board.getUnitMap().put(tile1, player1Avatar);
        BasicCommands.drawUnit(out, player1Avatar, tile1);

        Unit player2Avatar = player2.getAvatar();
        Tile tile2 = board.getTile(7, 2);
        player2Avatar.setPositionByTile(tile2);
        board.getUnitMap().put(tile2, player2Avatar);
        BasicCommands.drawUnit(out, player2Avatar, tile1);

        //just for testing - remember to remove
        Unit silver = BasicObjectBuilders.loadUnit(StaticConfFiles.silverguardSquire, getNextUnitId(), Unit.class);
        silver.setOwner(player2);
        silver.setCurrentHealth(2);
        silver.setAttackPower(3);
        silver.setMaximumHealth(2);
        board.placeUnitOnTile(gameState,silver, board.getTile(3, 2), false);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("Error");
        }
        BasicCommands.setUnitHealth(out, silver, 2);
        BasicCommands.setUnitAttack(out,silver,3);


    }

    public void nextTurn() {
        for (Map.Entry<Tile, Unit> entry : board.getUnitMap().entrySet()) {
            Unit unit = entry.getValue();
            if (unit.getOwner() == getCurrentPlayer() && unit.isStunned()) {
                unit.setStunned(false);
            }
        }
        this.isHumanTurn = !this.isHumanTurn;
        if (!isHumanTurn) {
            this.currentTurn++;
        }
        resetHasMovedFlags();
    }

    public void resetHasMovedFlags() {
        for (Unit unit : board.getUnitMap().values()) {
            unit.setHasMoved(false);
            unit.setHasAttacked(false);
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

    public List<Tile> getTilesOccupiedByEnemyPlayer() {
        List<Tile> enemyTiles = new ArrayList<>();
        for (Map.Entry<Tile, Unit> entry : board.getUnitMap().entrySet()) {
            Unit unit = entry.getValue();
            // Check if the unit belongs to the enemy and is not the avatar
            if (unit.getOwner() == getOpponentPlayer()) {
                enemyTiles.add(entry.getKey());
            }
        }
        return enemyTiles;
    }


    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void triggerProvoke(ActorRef out) {
        // Get the unit map from the board
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(getBoard().getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the OpeningGambit ability
            if (unit.getAbility() instanceof Provoke) {
                // Trigger the ability
                unit.getAbility().triggerAbility(out, this, tile);
            }
        }
    } 


}
