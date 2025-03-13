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

    public void setBoard(ActorRef out, Board board) {
        this.board = board;

        Unit player1Avatar = player1.getAvatar();
        Tile tile1 = board.getTile(1, 2);
        player1Avatar.setPositionByTile(tile1);
        Board.getUnitMap().put(tile1, player1Avatar);
        BasicCommands.drawUnit(out, player1Avatar, tile1);

        Unit player2Avatar = player2.getAvatar();
        Tile tile2 = board.getTile(7, 2);
        player2Avatar.setPositionByTile(tile2);
        Board.getUnitMap().put(tile2, player2Avatar);
        BasicCommands.drawUnit(out, player2Avatar, tile1);
    }

    public void nextTurn() {
        for (Map.Entry<Tile, Unit> entry : Board.getUnitMap().entrySet()) {
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
        for (Unit unit : Board.getUnitMap().values()) {
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
        for (Map.Entry<Tile, Unit> entry : Board.getUnitMap().entrySet()) {
            if (entry.getValue().getOwner() == getCurrentPlayer()) {
                occupiedTiles.add(entry.getKey());
            }
        }
        return occupiedTiles;
    }

    public List<Tile> getTilesOccupiedByEnemyPlayer() {
        List<Tile> enemyTiles = new ArrayList<>();
        for (Map.Entry<Tile, Unit> entry : Board.getUnitMap().entrySet()) {
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
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(Board.getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the Provoke Ability
            for (Ability ability : unit.getAbilities()) {
                if (ability instanceof Provoke) {
                    ability.triggerAbility(out, this, tile);
                }
            }
        }
    }

    public synchronized void getValidMovementTiles(int tileX, int tileY, ActorRef out) {
        Tile tile1 = board.getTile(tileX, tileY);
        Unit unit = board.getUnitOnTile(tile1);

        unit.getAbilities().stream()
                .filter(ability -> ability instanceof Flying && out != null)
                .forEach(ability -> ability.triggerAbility(out, this, tile1));

        Tile lastTile = null;

        int cardinalRange = 2;
        int diagonalRange = 1;

        // Define movement directions
        int[][] validDirections = {
                {-1, 0}, {1, 0},  // Left, Right
                {0, -1}, {0, 1},  // Up, Down
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1}  // Diagonal Moves
        };

        for (int[] direction : validDirections) {
            boolean isDiagonal = (direction[0] != 0 && direction[1] != 0);
            int range = isDiagonal ? diagonalRange : cardinalRange;

            // **Check diagonal validity before proceeding**
            if (isDiagonal) {
                int requiredX = tileX + direction[0];  // Required x-direction (left/right)
                int requiredY = tileY + direction[1];  // Required y-direction (up/down)

                // Check bounds before accessing board.getTile()
                if (requiredX < 0 || requiredX >= 9 || requiredY < 0 || requiredY >= 5) {
                    continue;
                }

                // Both required cardinal directions must be open
                boolean canMoveX = board.getUnitOnTile(board.getTile(tileX + direction[0], tileY)) == null;
                boolean canMoveY = board.getUnitOnTile(board.getTile(tileX, tileY + direction[1])) == null;

                if (!canMoveX && !canMoveY) {
                    continue; // **Skip this diagonal direction**
                }
            }

            for (int step = 1; step <= range; step++) {
                int newX = tileX + direction[0] * step;
                int newY = tileY + direction[1] * step;

                if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                    Tile tile = board.getTile(newX, newY);
                    Unit unitOnTile = board.getUnitOnTile(tile);

                    if (unitOnTile != null) {
                        if (unitOnTile.getOwner() == getOpponentPlayer()) {
                            redHighlightedTiles.add(tile);  // Highlight enemy units in red
                        }
                        break; // **Stop further movement in this direction**
                    } else {
                        addHighlightedTile(tile); // Highlight valid movement tile
                        lastTile = tile;
                    }
                }
            }

            // Check adjacent tiles of the last tile in this direction
            if (lastTile != null && !redHighlightedTiles.contains(lastTile)) {
                List<Tile> adjacentTiles = board.getAdjacentTiles(lastTile);

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
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(Board.getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the Deathwatch ability and triggers
            unit.getAbilities().stream()
                    .filter(ability -> ability instanceof Deathwatch)
                    .forEach(ability -> ability.triggerAbility(out, this, tile));
        }
    }

    public void triggerOpeningGambit(ActorRef out) {
        // Get the unit map from the board
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(Board.getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the OpeningGambit ability
            unit.getAbilities().stream()
                    .filter(ability -> ability instanceof OpeningGambit)
                    .forEach(ability -> ability.triggerAbility(out, this, tile));
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
        List<Tile> adjacentTiles = getBoard().getAdjacentTiles(unitTile);
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
        List<Tile> adjacentTiles = getBoard().getAdjacentTiles(targetTile);

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
            boolean movementSuccessful = moveAttackerToAdjacentTile(out, attacker, targetTile);
            if (!movementSuccessful) {
                clearAllHighlights(out);
                BasicCommands.addPlayer1Notification(out, "movement to adjacent tile not possible", 3);
                return; // Exit the method if movement fails
            }
        }

        // Perform the attack
        BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
        try {
            Thread.sleep(1000); // Delay for animation
        } catch (InterruptedException e) {
            System.out.println("Error during attack animation delay");
        }
        BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.idle);
        target.takeDamage(attacker.getAttackPower(), out);

        // Handle the states after attack
        handleUnitStates(out, attacker, target);

        // Mark the attacker as having attacked
        attacker.setHasAttacked(true);
        clearAllHighlights(out);
    }

    private boolean moveAttackerToAdjacentTile(ActorRef out, Unit attacker, Tile targetTile) {
        Tile adjacentTile = findPotentialAdjacentTile(targetTile);

        if (adjacentTile != null) {
            boolean movement = handleMovement(out, adjacentTile, attacker);
            if (!movement) {
                return false;
            }

            // Simulate movement delay
            try {
                Thread.sleep(2500); // Delay for animation
            } catch (InterruptedException e) {
                System.out.println("Error during movement delay");
            }

            return true; // Movement was successful
        } else {
            System.out.println("No adjacent tile available for movement.");
            return false; // Movement failed
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
        target.counterDamage(attacker, out);
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
        if (unit.getAbilities().stream().anyMatch(ability -> ability instanceof Provoke)) {
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
            avatar.getAbilities().forEach(ability ->
                    ability.triggerAbility(out, this, getBoard().getTileForUnit(avatar))
            );
        }
        owner.setHealth(avatar.getCurrentHealth());

        if (owner == player1) {
            BasicCommands.setPlayer1Health(out, owner);
        } else if (owner == player2) {
            BasicCommands.setPlayer2Health(out, owner);
        }
    }

    public boolean handleMovement(ActorRef out, Tile targetTile, Unit selectedUnit) {
        if (targetTile == null || getBoard().getUnitOnTile(targetTile) != null) {
            System.out.println("Error: Invalid target tile.");
            return false;
        }

        Tile startTile = getSourceTile();
        int dx = targetTile.getTilex() - startTile.getTilex();
        int dy = targetTile.getTiley() - startTile.getTiley();
        boolean isDiagonal = (dx != 0 && dy != 0);
        boolean moved = false; // Track if movement was successful

        if (isDiagonal) {
            boolean left = hasUnitOnLeft(startTile);
            boolean right = hasUnitOnRight(startTile);
            boolean top = hasUnitOnTop(startTile);
            boolean bottom = hasUnitOnBottom(startTile);

            if (left && right && top && bottom) {
                notifyBlocked(out, "All sides are blocked.");
            } else if (left && right) {
                moved = moveYFirst(selectedUnit, targetTile, dy, top, bottom);
            } else if (right) {
                // If left is open and trying to move left (dx < 0),
                if (dx < 0 && !left) {
                    moved = moveXFirst(selectedUnit, targetTile);
                } else {
                    moved = (top && bottom) ? notifyBlocked(out, "Right, top, and bottom blocked.") : moveYFirst(selectedUnit, targetTile, dy, top, bottom);
                }
            } else if (left) {
                // If right is open and you're trying to move right (dx > 0),
                if (dx > 0 && !right) {
                    moved = moveXFirst(selectedUnit, targetTile);
                } else {
                    moved = (top && bottom) ? moveXFirst(selectedUnit, targetTile) : moveYFirst(selectedUnit, targetTile, dy, top, bottom);
                }
            } else {
                moved = moveXFirst(selectedUnit, targetTile);
            }
        } else {
            System.out.println("Non-diagonal movement.");
            getBoard().placeUnitOnTile(this, selectedUnit, targetTile, false);
            moved = true;
        }

        if (moved) {
            selectedUnit.setHasMoved(true);
            setSourceTile(null);
            setSelectedUnit(null);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Moves vertically if possible.
     * Returns true if move is successful, false if blocked.
     */
    private boolean moveYFirst(Unit unit, Tile target, int dy, boolean top, boolean bottom) {
        if (!top && !bottom) {
            System.out.println("Moving along y-axis.");
            getBoard().placeUnitOnTile(this, unit, target, true);
            return true;
        } else if (top && !bottom && dy > 0) {
            System.out.println("Moving down.");
            getBoard().placeUnitOnTile(this, unit, target, true);
            return true;
        } else if (!top && bottom && dy < 0) {
            System.out.println("Moving up.");
            getBoard().placeUnitOnTile(this, unit, target, true);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Moves along the x-axis.
     * Always successful, so returns true.
     */
    private boolean moveXFirst(Unit unit, Tile target) {
        System.out.println("Moving along x-axis.");
        getBoard().placeUnitOnTile(this, unit, target, false);
        return true;
    }

    /**
     * Notifies the player that movement is blocked.
     * Returns false so we can track failed movement.
     */
    private boolean notifyBlocked(ActorRef out, String message) {
        System.out.println("Cannot move: " + message);
        BasicCommands.addPlayer1Notification(out, "Cannot move: " + message, 2);
        return false;
    }

    private boolean hasUnitOnTop(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile above
        if (startY > 0) {
            Tile topTile = getBoard().getTile(startX, startY - 1);
            return topTile != null && getBoard().getUnitOnTile(topTile) != null; // Unit found above
        }

        return false; // No unit found above
    }

    private boolean hasUnitOnBottom(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile below
        if (startY < 4) {
            Tile bottomTile = getBoard().getTile(startX, startY + 1);
            return bottomTile != null && getBoard().getUnitOnTile(bottomTile) != null; // Unit found below
        }

        return false; // No unit found below
    }

    private boolean hasUnitOnLeft(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile to the left
        if (startX > 0) {
            Tile leftTile = getBoard().getTile(startX - 1, startY);
            return leftTile != null && getBoard().getUnitOnTile(leftTile) != null; // Unit found to the left
        }

        return false; // No unit found to the left
    }

    private boolean hasUnitOnRight(Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile to the right
        if (startX < 8) {
            Tile rightTile = getBoard().getTile(startX + 1, startY);
            return rightTile != null && getBoard().getUnitOnTile(rightTile) != null; // Unit found to the right
        }

        return false; // No unit found to the right
    }


    public void getValidSummonTile(ActorRef out) {
        // Clear all previously highlighted tiles
        clearAllHighlights(out);

        // Get all tiles occupied by the current player
        List<Tile> occupiedTiles = getTilesOccupiedByCurrentPlayer();

        // Define all 8 possible adjacent directions
        int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, // Top-left, Top, Top-right
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
            unit.setHasAttacked(true);  //mark as attacked so it cannot attack (disabling it in a way)

        }
        player2.setMana(0);
        BasicCommands.setPlayer2Mana(out, player2);
        player1.setMana(0);
        BasicCommands.setPlayer1Mana(out, player1);
        player1.setHealth(0);
        player2.setHealth(0);
        BasicCommands.setPlayer1Health(out, player1);
        BasicCommands.setPlayer2Health(out, player2);
    }

    // clear a player's hand
    private void clearPlayerHand(Player player, ActorRef out) {
        List<Card> hand = player.getHand(); // get player's hand
        if (hand == null || hand.isEmpty()) {
            return;
        }
        for (int i = 0; i < hand.size(); i++) {
            BasicCommands.deleteCard(out, i + 1); //delete card from position
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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
