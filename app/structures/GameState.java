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
    private List<Tile> highlightedTiles;  // Track blue highlighted tiles
    private List<Tile> redHighlightedTiles; //Track red highlighted tiles
    private Tile sourceTile; // Track the source tile for highlighting
    private Unit selectedUnit;
    private Card selectedCard;
    private int nextUnitId = 2;


    public GameState() {
        this.currentTurn = 1;
        this.isHumanTurn = true; //start with player's turn
        this.gameInitialized = false; //needs to be initialised
        this.highlightedTiles = new ArrayList<>();
        this.redHighlightedTiles = new ArrayList<>();


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
        board.placeUnitOnTile(gameState, silver, board.getTile(3, 2), false);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("Error");
        }
        BasicCommands.setUnitHealth(out, silver, 2);
        BasicCommands.setUnitAttack(out, silver, 3);


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

    public List<Tile> getHighlightedTiles() {
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

        for (Tile tile : redHighlightedTiles) {
            BasicCommands.drawTile(out, tile, 0); // Reset highlight (mode = 0)
        }
        redHighlightedTiles.clear(); // Clear the set of highlighted tiles
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


    public void getValidMovementTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
        Tile tile1 = gameState.getBoard().getTile(tileX, tileY);
        Unit unit = gameState.getBoard().getUnitOnTile(tile1);

        if (unit.getAbility() instanceof Flying) {
            unit.getAbility().triggerAbility(out, gameState, tile1);
        }

        Tile lastTile = null;
        gameState.clearAllHighlights(out);

        int cardinalRange = 2;
        int diagonalRange = 1;

        // Define movement ranges
        int[][] validDirections = {
                {-1, 0}, {1, 0}, // Left, Right
                {0, -1}, {0, 1}, // Up, Down
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals
        };

        // Highlight directions
        for (int[] direction : validDirections) {
            int range = (direction[0] != 0 && direction[1] != 0) ? diagonalRange : cardinalRange;

            // Check tiles in this direction up to the movement range
            for (int step = 1; step <= range; step++) {
                int newX = tileX + direction[0] * step;
                int newY = tileY + direction[1] * step;

                // Check if the new coordinates are within the board bounds
                if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                    Tile tile = gameState.getBoard().getTile(newX, newY);
                    Unit unitOnTile = gameState.getBoard().getUnitOnTile(tile);

                    // If the tile is blocked by another unit
                    if (unitOnTile != null) {
                        // Highlight enemy units in red
                        if (unitOnTile.getOwner() == gameState.getOpponentPlayer()) {
                            //BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                            gameState.addRedHighlightedTile(tile);
                        }
                        break; // Stop further movement in this direction
                    } else {
                        // Highlight empty tiles
                        //BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
                        gameState.addHighlightedTile(tile);
                        lastTile = tile; // Track the last valid tile in this direction
                    }
                }
            }

            // Check adjacent tiles of the last tile in this direction
            if (lastTile != null) {
                List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState, lastTile);

                // Highlight adjacent tiles with enemy units
                for (Tile tile : adjacentTiles) {
                    Unit unitOnTile = gameState.getBoard().getUnitOnTile(tile);

                    if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer()) {
                        //BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                        gameState.addRedHighlightedTile(tile); // Track highlighted tiles

                    }
                }
            }
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("Error");
        }
    }

    private void addRedHighlightedTile(Tile tile) {
        redHighlightedTiles.add(tile);
    }

    public List<Tile> getRedHighlightedTiles() {
        return redHighlightedTiles;
    }

    public void handleCreatureCardClick(ActorRef out, Tile clickedTile, Card selectedCard) {
        if (isHighlightedTile(clickedTile)) { // Check if the clicked tile is valid for summoning
            selectedCard.summonCreature(out, this, clickedTile);

            Player currentPlayer = getCurrentPlayer();
            if (currentPlayer instanceof HumanPlayer) {
                ((HumanPlayer) currentPlayer).playCard(selectedCard, out);
            } else if (currentPlayer instanceof AIController) {
                ((AIController) currentPlayer).playCard(selectedCard, out);
            }

            clearAllHighlights(out); // Clear highlights after summoning
        } else { // Clicked on an invalid tile, reset selection
            clearAllHighlights(out);
            setSelectedCard(null);
        }
    }

    private void triggerDeathwatchAbilities(ActorRef out) {
        // Get the unit map from the board
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(board.getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the Deathwatch ability
            if (unit.getAbility() instanceof Deathwatch) {
                // Trigger the Deathwatch ability
                unit.getAbility().triggerAbility(out, this, tile);
            }
        }
    }

    public void triggerOpeningGambit(ActorRef out) {
        // Get the unit map from the board
        boolean triggered = false;
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(board.getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the OpeningGambit ability
            if (unit.getAbility() instanceof OpeningGambit) {
                // Trigger the ability
                unit.getAbility().triggerAbility(out, this, tile);
                triggered = true;
            }
        }
        if (triggered) {
            BasicCommands.addPlayer1Notification(out, "Opening Gambit Triggered", 3);
        }
    }

    public void handleSpellCardClick(ActorRef out, Tile clickedTile, Unit unitOnTile) {
        if (selectedCard != null && !selectedCard.isCreature()) {
            SpellEffect spellEffect = SpellEffectMap.getSpellEffectForCard(selectedCard.getCardname());
            if (spellEffect != null && isHighlightedTile(clickedTile)) {
                spellEffect.applyEffect(out, this, clickedTile);
                // Remove the card from the player's hand and update the UI
                if (getCurrentPlayer() == player1) {
                    HumanPlayer currentPlayer = (HumanPlayer) getCurrentPlayer();
                    currentPlayer.playCard(selectedCard, out); // Use playCard to remove the card
                } else if (getCurrentPlayer() == player2) {
                    //bhumika to add
                }
            }
        }
        clearAllHighlights(out);
    }

    public void getValidAttackTiles(Tile unitTile, ActorRef out) {
        List<Tile> adjacentTiles = getBoard().getAdjacentTiles(this, unitTile);
        Unit unit = getBoard().getUnitOnTile(unitTile);

        unit.getValidAttackTargets();

        // Highlight adjacent tiles with enemy units
        for (Tile tile : adjacentTiles) {
            Unit unitOnTile = getBoard().getUnitOnTile(tile);

            if (unitOnTile != null && unitOnTile.getOwner() == getOpponentPlayer() && unit.canAttack(unitOnTile)) {
                //BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                addRedHighlightedTile(tile); // Track highlighted tiles

            }
        }
    }

    private Tile findPotentialAdjacentTile(Tile targetTile) {
        List<Tile> adjacentTiles = getBoard().getAdjacentTiles(this, targetTile);

        for (Tile tile : adjacentTiles) {
            if (isHighlightedTile(tile)) {
                return tile;
            }
        }
        return null; // No highlighted adjacent tile found
    }

    //need to refactor handleAttack - does too many things. Can put some methods in Unit and Board?
    public void handleAttack(ActorRef out, Unit target) {
        Unit attacker = getSelectedUnit();

        Tile attackerTile = getBoard().getTileForUnit(attacker);
        Tile targetTile = getBoard().getTileForUnit(target);

        // If not adjacent to the target, move to an adjacent tile
        if (!getBoard().isAdjacentTile(attackerTile, targetTile)) {
            Tile adjacentTile = findPotentialAdjacentTile(targetTile);
            // Move the attacker to the adjacent tile
            getBoard().placeUnitOnTile(this, attacker, adjacentTile, false);

            // Simulate movement delay. 2500 is important to pause the code here before proceeding further which might set attacker to null.
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                System.out.println("error");
            }
        }

        // Perform the attack
        BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
        target.takeDamage(attacker.getAttackPower());

        // Check if target is dead
        if (target.getCurrentHealth() <= 0) {
            getBoard().removeUnitFromTile(targetTile, out);
            triggerDeathwatchAbilities(out);

            if (target.isAvatar()) {
                Player winner = attacker.getOwner();
                //gameState.declareWin(winner);
            }
            target = null;
        } else {
            BasicCommands.setUnitHealth(out, target, target.getCurrentHealth());
        }

        if (target != null && target.isAvatar()) {
            Player owner = target.getOwner();
            owner.setHealth(target.getCurrentHealth());
            if (owner == player1) {
                BasicCommands.setPlayer1Health(out, owner);
            } else if (owner == player2) {
                BasicCommands.setPlayer2Health(out, owner);
            }
        }

        // Mark the attacker as having moved and attacked
        attacker.setHasMoved(true);
        attacker.setHasAttacked(true);

        // Simulate delay before counterattack begins
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted during sleep", e);
        }

        // Counterattack logic
        if (target != null) {
            BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
            target.counterDamage(attacker);
            if (attacker.getCurrentHealth() <= 0) {
                Tile newAttackerTile = getBoard().getTileForUnit(attacker);
                getBoard().removeUnitFromTile(newAttackerTile, out);
                triggerDeathwatchAbilities(out);

                if (attacker.isAvatar()) {
                    Player winner = target.getOwner();
                    //gameState.declareWin(winner);
                }
                attacker = null;
            } else {
                BasicCommands.setUnitHealth(out, attacker, attacker.getCurrentHealth());
            }
        }

        if (attacker != null && attacker.isAvatar()) {
            attacker.triggerOnHitEffect(out, this); // Trigger "On Hit" effect if the attacker is the avatar
            Player owner = attacker.getOwner();
            owner.setHealth(attacker.getCurrentHealth());
            if (owner == player1) {
                BasicCommands.setPlayer1Health(out, owner);
            } else if (owner == player2) {
                BasicCommands.setPlayer2Health(out, owner);
            }
        }

        clearAllHighlights(out); // Clear highlights after the attack
    }

    private boolean hasUnitOnXAxis(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile to the left
        if (startX > 0) {
            Tile leftTile = getBoard().getTile(startX - 1, startY);
            if (leftTile != null && getBoard().getUnitOnTile(leftTile) != null) {
                return true; // Unit found to the left
            }
        }

        // Check the tile to the right
        if (startX < 8) {
            Tile rightTile = getBoard().getTile(startX + 1, startY);
            if (rightTile != null && getBoard().getUnitOnTile(rightTile) != null) {
                return true; // Unit found to the right
            }
        }

        return false; // No unit found on the adjacent tiles
    }


    public void handleMovement(Tile targetTile, Unit selectedUnit) {
        Tile startTile = getSourceTile();

        // Check if the movement is diagonal
        int dx = Math.abs(targetTile.getTilex() - startTile.getTilex());
        int dy = Math.abs(targetTile.getTiley() - startTile.getTiley());
        boolean isDiagonal = (dx != 0 && dy != 0);

        if (isDiagonal) {
            // Check for obstacles adjacent on x-axis
            boolean hasUnitOnX = hasUnitOnXAxis(startTile);

            // Move y-axis first if there's a unit on the side (x-axis)
            boolean yfirst = hasUnitOnX;
            getBoard().placeUnitOnTile(this, selectedUnit, targetTile, yfirst);
        } else {
            // Non-diagonal movement, place the unit directly
            getBoard().placeUnitOnTile(this, selectedUnit, targetTile, false);
        }

        // Non-diagonal movement, place the unit directly
        // Mark the unit as moved
        selectedUnit.setHasMoved(true);

        //reset selection
        setSourceTile(null);
        setSelectedUnit(null);
    }

    public void getValidSummonTile(ActorRef out) {
        // Clear all previously highlighted tiles
        clearAllHighlights(out);

        // Get all tiles occupied by the current player
        List<Tile> occupiedTiles = getTilesOccupiedByCurrentPlayer();

        // Define all 8 possible adjacent directions
        int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1}, // Top-left, Top, Top-right
                {0, -1}, {0, 1},  // Left,       Right
                {1, -1}, {1, 0}, {1, 1}   // Bottom-left, Bottom, Bottom-right
        };

        // Iterate through all occupied tiles
        for (Tile occupiedTile : occupiedTiles) {
            int tilex = occupiedTile.getTilex();
            int tiley = occupiedTile.getTiley();

            // Iterate through all directions
            for (int[] dir : directions) {
                int newX = tilex + dir[0];
                int newY = tiley + dir[1];

                // Check if the new coordinates are within the board bounds
                if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                    Tile tile = getBoard().getTile(newX, newY);

                    // Check if the tile is empty (no unit on it)
                    if (getBoard().getUnitOnTile(tile) == null) {
                        // Highlight the tile
                        //BasicCommands.drawTile(out, tile, 1); // Highlight with mode = 1
                        addHighlightedTile(tile); // Track highlighted tiles
                    }
                }
            }
        }
    }
}
