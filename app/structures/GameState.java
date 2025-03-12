package structures;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import commands.BasicCommands;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 *
 * @author Dr. Richard McCreadie
 */
public class GameState {
    private int currentTurn;
    @JsonIgnore
    private HumanPlayer player1;

    @JsonIgnore
    private AIController player2;
    private boolean isHumanTurn;
    private boolean gameInitialized;
    private Board board;
    private List<Tile> highlightedTiles;  // Track blue highlighted tiles
    private List<Tile> redHighlightedTiles; //Track red highlighted tiles
    private Map<Unit, Unit> provokeEffects; // Maps affected units to the source Provoke unit
    private Tile sourceTile; // Track the source tile for highlighting
    private Unit selectedUnit;
    private Card selectedCard;
    private int nextUnitId = 2;


    public GameState() {
        this.currentTurn = 1;
        this.isHumanTurn = true; //start with player's turn
        this.gameInitialized = false; //needs to be initialised
        highlightedTiles = new CopyOnWriteArrayList<>();
        redHighlightedTiles = new CopyOnWriteArrayList<>();
        this.provokeEffects = new HashMap<>();

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
        setSourceTile(null);
        setSelectedCard(null);
        setSelectedUnit(null);
    }

    public void resetHasMovedFlags() {
        for (Unit unit : board.getUnitMap().values()) {
            unit.setHasMoved(false);
            unit.setHasAttacked(false);
        }
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
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
            // Check if the unit belongs to the enemy
            if (unit.getOwner() == getOpponentPlayer()) {
                enemyTiles.add(entry.getKey());
            }
        }
        return enemyTiles;
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
                unit.getAbility().triggerAbility(out, this, tile);
            }
        }
    }

    public synchronized void getValidMovementTiles(int tileX, int tileY, ActorRef out) {
        Tile tile1 = board.getTile(tileX, tileY);
        Unit unit = board.getUnitOnTile(tile1);

        if (unit.getAbility() instanceof Flying && out != null) {
            unit.getAbility().triggerAbility(out, this, tile1);
        }

        Tile lastTile = null;

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
                    Tile tile = board.getTile(newX, newY);
                    Unit unitOnTile = board.getUnitOnTile(tile);

                    // If the tile is blocked by another unit
                    if (unitOnTile != null) {
                        // Highlight enemy units in red
                        if (unitOnTile.getOwner() == getOpponentPlayer()) {
                            //BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                            redHighlightedTiles.add(tile);
                        }
                        break; // Stop further movement in this direction
                    } else {
                        // Highlight empty tiles
                        //BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
                        addHighlightedTile(tile);
                        lastTile = tile; // Track the last valid tile in this direction
                    }
                }
            }

            // Check adjacent tiles of the last tile in this direction
            if (lastTile != null && !redHighlightedTiles.contains(lastTile)) {
                List<Tile> adjacentTiles = board.getAdjacentTiles(this, lastTile);

                // Highlight adjacent tiles with enemy units
                for (Tile tile : adjacentTiles) {
                    Unit unitOnTile = board.getUnitOnTile(tile);

                    if (unitOnTile != null && unitOnTile.getOwner() == getOpponentPlayer()) {
                        //BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                        redHighlightedTiles.add(tile); // Track highlighted tiles

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

    public List<Tile> getRedHighlightedTiles() {
        return redHighlightedTiles;
    }

    public void handleCreatureCardClick(ActorRef out, Tile clickedTile, Card selectedCard) {
        if (isHighlightedTile(clickedTile)) { // Check if the clicked tile is valid for summoning
            if (selectedCard == null) {
                System.out.println("Error: No card selected for summoning!");
                return;
            }
            selectedCard.summonCreature(out, this, clickedTile);

            Player currentPlayer = getCurrentPlayer();
            if (currentPlayer instanceof HumanPlayer) {
                currentPlayer.playCard(selectedCard, out, this);
            }
            clearAllHighlights(out); // Clear highlights after summoning
        } else { // Clicked on an invalid tile, reset selection
            BasicCommands.addPlayer1Notification(out, "not a valid tile", 2);
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
    }

    public void handleSpellCardClick(ActorRef out, Tile clickedTile) {
        if (selectedCard != null && !selectedCard.isCreature()) {
            SpellEffect spellEffect = SpellEffectMap.getSpellEffectForCard(selectedCard.getCardname());
            if (spellEffect != null && (redHighlightedTiles.contains(clickedTile) || highlightedTiles.contains(clickedTile))) {
                spellEffect.applyEffect(out, this, clickedTile);

                // Remove the card from the player's hand and update the UI
                Player currentPlayer = getCurrentPlayer();
                if (currentPlayer instanceof HumanPlayer) {
                    currentPlayer.playCard(selectedCard, out, this);
                }
            }
        }
        clearAllHighlights(out);
    }

    public synchronized void getValidAttackTiles(Tile unitTile) {
        List<Tile> adjacentTiles = getBoard().getAdjacentTiles(this, unitTile);
        Unit unit = getBoard().getUnitOnTile(unitTile);

        // Highlight adjacent tiles with enemy units
        for (Tile tile : adjacentTiles) {
            Unit unitOnTile = getBoard().getUnitOnTile(tile);

            if (unitOnTile != null && unitOnTile.getOwner() == getOpponentPlayer() && unit.canAttack(unitOnTile)) {
                redHighlightedTiles.add(tile);// Track highlighted tiles

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

    public void handleAttack(ActorRef out, Unit target) {
        Unit attacker = getSelectedUnit();
        if (attacker == null) {
            System.out.println("Error: No attacker selected.");
            return;
        }

        Tile attackerTile = getBoard().getTileForUnit(attacker);
        Tile targetTile = getBoard().getTileForUnit(target);

        // Move the attacker to an adjacent tile if necessary
        if (!getBoard().isAdjacentTile(attackerTile, targetTile)) {
            moveAttackerToAdjacentTile(out, attacker, targetTile);
        }

        // Perform the attack
        BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
        try {
            Thread.sleep(1000); // Delay for animation
        } catch (InterruptedException e) {
            System.out.println("Error during attack animation delay");
        }
        target.takeDamage(attacker.getAttackPower());

        // Handle the states after attack
        handleUnitStates(out, attacker, target);

        // Mark the attacker as having attacked
        attacker.setHasAttacked(true);
        clearAllHighlights(out);
    }

    private void moveAttackerToAdjacentTile(ActorRef out, Unit attacker, Tile targetTile) {
        Tile adjacentTile = findPotentialAdjacentTile(targetTile);
        if (adjacentTile != null) {
            handleMovement(adjacentTile, attacker);
        }

        // Simulate movement delay
        try {
            Thread.sleep(2500); // Delay for animation
        } catch (InterruptedException e) {
            System.out.println("Error during movement delay");
        }
    }

    private void handleUnitStates(ActorRef out, Unit attacker, Unit target) {
        attacker.setHasMoved(true);
        attacker.setHasAttacked(true);

        if (target.getCurrentHealth() <= 0) {
            handleUnitDeath(out, target);

            if (target.isAvatar()) {
                Player winner = attacker.getOwner();
                endGame(winner, out);
            }
        } else {
            handleCounterattack(out, attacker, target);
            BasicCommands.setUnitHealth(out, target, target.getCurrentHealth());
        }

        if (target != null && target.isAvatar()) {
            handleAvatarHit(out, target);
        }
    }

    private void handleCounterattack(ActorRef out, Unit attacker, Unit target) {
        BasicCommands.playUnitAnimation(out, target, UnitAnimationType.attack);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted during sleep", e);
        }
        target.counterDamage(attacker);
        BasicCommands.playUnitAnimation(out, target, UnitAnimationType.idle);

        if (attacker.getCurrentHealth() <= 0) {
            handleUnitDeath(out, attacker);

            if (attacker.isAvatar()) {
                Player winner = target.getOwner();
                endGame(winner, out);
            }
        } else {
            BasicCommands.setUnitHealth(out, attacker, attacker.getCurrentHealth());
            if (attacker.isAvatar()) {
                handleAvatarHit(out, attacker);
            }
        }
    }

    private void handleUnitDeath(ActorRef out, Unit unit) {
        if (unit.getAbility() instanceof Provoke) {
            removeProvokeEffect(unit, out);
        }
        Tile unitTile = getBoard().getTileForUnit(unit);
        getBoard().removeUnitFromTile(unitTile, out);
        triggerDeathwatchAbilities(out);
    }

    private void handleAvatarHit(ActorRef out, Unit avatar) {
        avatar.triggerOnHitEffect(out, this); // Trigger on-hit effects

        // Update player health
        Player owner = avatar.getOwner();
        if (owner == getCurrentPlayer()) {
            Ability ability = avatar.getAbility();
            if (ability != null) {
                ability.triggerAbility(out, this, getBoard().getTileForUnit(avatar));
            }
        }
        owner.setHealth(avatar.getCurrentHealth());

        if (owner == player1) {
            BasicCommands.setPlayer1Health(out, owner);
        } else if (owner == player2) {
            BasicCommands.setPlayer2Health(out, owner);
        }
    }

    private boolean hasUnitOnXAxis(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile to the left (one and two spaces)
        for (int i = 1; i <= 2; i++) {
            if (startX - i >= 0) {
                Tile leftTile = getBoard().getTile(startX - i, startY);
                if (leftTile != null && getBoard().getUnitOnTile(leftTile) != null) {
                    return true; // Unit found to the left
                }
            }
        }

        // Check the tile to the right (one and two spaces)
        for (int i = 1; i <= 2; i++) {
            if (startX + i < 9) {
                Tile rightTile = getBoard().getTile(startX + i, startY);
                if (rightTile != null && getBoard().getUnitOnTile(rightTile) != null) {
                    return true; // Unit found to the right
                }
            }
        }

        return false; // No unit found on the adjacent tiles
    }

    private boolean hasUnitOnYAxis(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile above (one and two spaces)
        for (int i = 1; i <= 2; i++) {
            if (startY - i >= 0) {
                Tile upperTile = getBoard().getTile(startX, startY - i);
                if (upperTile != null && getBoard().getUnitOnTile(upperTile) != null) {
                    return true; // Unit found above
                }
            }
        }

        // Check the tile below (one and two spaces)
        for (int i = 1; i <= 2; i++) {
            if (startY + i < 5) {
                Tile lowerTile = getBoard().getTile(startX, startY + i);
                if (lowerTile != null && getBoard().getUnitOnTile(lowerTile) != null) {
                    return true; // Unit found below
                }
            }
        }

        return false; // No unit found on the adjacent tiles
    }


    public void handleMovement(Tile targetTile, Unit selectedUnit) {
        // Check if targetTile is null to avoid NullPointerException
        if (targetTile == null) {
            System.out.println("Error: Target tile is null.");
            return; // Return early if targetTile is invalid
        }

        // Check if the target tile is already occupied by another unit
        if (getBoard().getUnitOnTile(targetTile) != null) {
            System.out.println("Error: Target tile is already occupied by another unit.");
            return;
        }

        Tile startTile = getSourceTile();

        // Calculate the difference in x and y coordinates
        int dx = Math.abs(targetTile.getTilex() - startTile.getTilex());
        int dy = Math.abs(targetTile.getTiley() - startTile.getTiley());

        // Check if the movement is valid (two spaces in any direction or one space diagonally)
        boolean isValidMove = (dx == 2 && dy == 0) || (dx == 0 && dy == 2) || (dx == 1 && dy == 1);

        if (!isValidMove) {
            System.out.println("Error: Invalid movement. Units can move two spaces in any direction or one space diagonally.");
            return;
        }

        // Check for obstacles adjacent on x-axis and y-axis if moving diagonally
        if (dx == 1 && dy == 1) {
            boolean hasUnitOnX = hasUnitOnXAxis(startTile);
            boolean hasUnitOnY = hasUnitOnYAxis(startTile);

            // If both axes are blocked, prevent diagonal movement
            if (hasUnitOnX && hasUnitOnY) {
                System.out.println("Error: Cannot move diagonally due to obstacles on both x-axis and y-axis.");
                return;
            }

            // Prioritize x-axis if y-axis is blocked
            if (hasUnitOnY && !hasUnitOnX) {
                // Move x-axis first
                getBoard().placeUnitOnTile(this, selectedUnit, targetTile, false);
            } else if (hasUnitOnX && !hasUnitOnY) {
                // Move y-axis first if x-axis is blocked
                getBoard().placeUnitOnTile(this, selectedUnit, targetTile, true);
            } else {
                // If neither axis is blocked, prefer moving x-axis first
                getBoard().placeUnitOnTile(this, selectedUnit, targetTile, false);
            }
        } else {
            // Non-diagonal or straight movement, place the unit directly
            getBoard().placeUnitOnTile(this, selectedUnit, targetTile, false);
        }

        // Mark the unit as moved
        selectedUnit.setHasMoved(true);

        // Reset selection
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

    public void endGame(Player winner, ActorRef out) {
        // Notify the players that the game has ended
        BasicCommands.addPlayer1Notification(out, winner == player1 ? "Player 1 Wins!" : "Player 2 Wins!", 20);

        // Disable further moves or actions
        this.isHumanTurn = false; // Stop the game loop
        this.gameInitialized = false; // Mark the game as ended

        // Clear all highlights and selections
        clearAllHighlights(out);
        setSelectedCard(null);
        setSelectedUnit(null);
        setSourceTile(null);

        clearPlayerHand(player1, out);
        clearPlayerHand(player2, out);

        // disable movement of units
        for (Map.Entry<Tile, Unit> entry : Board.getUnitMap().entrySet()) {
            Unit unit = entry.getValue();
            unit.setCanMove(false); // Disable movement

        }
        player2.setMana(0);
        BasicCommands.setPlayer2Mana(out, player2);
        player1.setMana(0);
        BasicCommands.setPlayer1Mana(out, player1);
    }

    // clear a player's hand
    private void clearPlayerHand(Player player, ActorRef out) {
        List<Card> hand = player.getHand(); // get player's hand
        if (hand == null || hand.isEmpty()) {
            return;
        }
        //clear cards from UI
        for (int i = 0; i < hand.size(); i++) {
            // Clear the card from the UI by drawing a null card
            BasicCommands.deleteCard(out, i + 1); //delete card from position
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Clear the hand list
        hand.clear();
    }

    public boolean isGameInitialized() {
        return gameInitialized;
    }

    public void addProvokeEffect(Unit enemyUnit, Unit sourceUnit) {
        provokeEffects.put(enemyUnit, sourceUnit);
    }

    public void removeProvokeEffect(Unit sourceUnit, ActorRef out) {
        // Iterate through the map to find all units affected by the sourceUnit
        provokeEffects.entrySet().removeIf(entry -> {
            Unit affectedUnit = entry.getKey();
            Unit provokeUnit = entry.getValue();

            // If the sourceUnit is the Provoke unit, reset the affected unit's state
            if (provokeUnit.equals(sourceUnit)) {
                BasicCommands.addPlayer1Notification(out, "Provoke disabled", 3);
                affectedUnit.setCanMove(true); // Allow movement
                affectedUnit.setValidAttackTargets(null); // Reset attack targets
                return true; // Remove this entry from the map
            }
            return false; // Keep the entry in the map
        });
    }

    /**
     * Checks if a unit is already affected by any Provoke ability.
     *
     * @param unit The unit to check.
     * @return True if the unit is already provoked, false otherwise.
     */
    public boolean isUnitProvoked(Unit unit) {
        return provokeEffects.containsKey(unit);
    }


    public void addRedHighlightedTile(Tile enemyTile) {
        redHighlightedTiles.add(enemyTile);
    }
}
