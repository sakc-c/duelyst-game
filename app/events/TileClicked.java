package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.*;
import structures.basic.*;
import utils.BasicObjectBuilders;
import structures.HumanPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * <p>
 * {
 * messageType = “tileClicked”
 * tilex = <x index of the tile>
 * tiley = <y index of the tile>
 * }
 *
 * @author Dr. Richard McCreadie
 */

// might need to make some helper methods given all these if conditions, will work on refactoring the code.
// Was trying to build logic and it being functional first.
public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        //current clicks
        Tile clickedTile = gameState.getBoard().getTile(tilex, tiley);
        Unit unitOnTile = gameState.getBoard().getUnitOnTile(clickedTile);

        //any stored data
        Card selectedCard = gameState.getSelectedCard();
        Tile sourceTile = gameState.getSourceTile();
        Unit selectedUnit = gameState.getSelectedUnit();

        if (selectedCard != null) {
            if (selectedCard.isCreature()) {
                handleCreatureCardClick(out, gameState, clickedTile, selectedCard);
                gameState.setSelectedCard(null);
            } else {
                handleSpellCardClick(out, gameState, clickedTile, unitOnTile);
                gameState.setSelectedCard(null);
            }
        } else if (sourceTile != null) {
            if (sourceTile.equals(clickedTile)) { //if same as clicked tile, clear highlights
                gameState.clearAllHighlights(out);
                gameState.setSourceTile(null);
                gameState.setSelectedUnit(null);
            } else if (selectedUnit != null) { //if selected unit exists
                //selected for attack
                if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer() && !selectedUnit.hasAttacked()) {
                    handleAttack(out, gameState, unitOnTile);
                }
                // Selected for movement
                else if (gameState.isHighlightedTile(clickedTile) && !selectedUnit.hasMoved()) {
                    handleMovement(gameState, clickedTile, selectedUnit);
                    gameState.clearAllHighlights(out);
                }
                // Clicked on a non-highlighted tile, reset selection
                else {
                    gameState.clearAllHighlights(out);
                    gameState.setSourceTile(null);
                    gameState.setSelectedUnit(null);
                }
            }
        }
        // If no card or unit is selected before, highlight valid tiles for the current player's unit
        else if (unitOnTile != null && unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
            if (!unitOnTile.hasMoved() && unitOnTile.canMove()) {
                highlightValidTiles(tilex, tiley, gameState, out);
                gameState.setSourceTile(clickedTile);
                gameState.setSelectedUnit(unitOnTile);
            } else if (!unitOnTile.hasAttacked()) {
                highlightValidAttackTiles(clickedTile, gameState, out);
                gameState.setSourceTile(clickedTile);
                gameState.setSelectedUnit(unitOnTile);
            }
        }
    }


    private void handleCreatureCardClick(ActorRef out, GameState gameState, Tile clickedTile, Card selectedCard) {
        if (gameState.isHighlightedTile(clickedTile)) { // Check if the clicked tile is valid for summoning
            summonCreature(out, gameState, clickedTile);

                Player currentPlayer = gameState.getCurrentPlayer();
                if (currentPlayer instanceof HumanPlayer){
                    ((HumanPlayer)currentPlayer).playCard(selectedCard, out);
                } else if (currentPlayer instanceof AIController) {
                    ((AIController)currentPlayer).playCard(selectedCard,out);
                }

            gameState.clearAllHighlights(out); // Clear highlights after summoning
        } else { // Clicked on an invalid tile, reset selection
            gameState.clearAllHighlights(out);
            gameState.setSelectedCard(null);
        }
    }

    private void handleSpellCardClick(ActorRef out, GameState gameState, Tile clickedTile, Unit unitOnTile) {
        Card selectedCard = gameState.getSelectedCard();
        if (selectedCard != null && !selectedCard.isCreature()) {
            SpellEffect spellEffect = SpellEffectMap.getSpellEffectForCard(selectedCard.getCardname());
            if (spellEffect != null && gameState.isHighlightedTile(clickedTile)) {
                spellEffect.applyEffect(out, gameState, clickedTile);
             // Remove the card from the player's hand and update the UI
                if (gameState.getCurrentPlayer() instanceof HumanPlayer) {
                    HumanPlayer currentPlayer = (HumanPlayer) gameState.getCurrentPlayer();
                    currentPlayer.playCard(selectedCard, out); // Use playCard to remove the card
                }
            }
        }
        gameState.clearAllHighlights(out);
    }

    private void summonCreature(ActorRef out, GameState gameState, Tile tile) {
        //trigger openingGambit abilities of existing units on the board
        triggerOpeningGambit(out,gameState);

        Card selectedCard = gameState.getSelectedCard();
        Unit newUnit = BasicObjectBuilders.loadUnit(selectedCard.getUnitConfig(), gameState.getNextUnitId(), Unit.class);
        newUnit.setOwner(gameState.getCurrentPlayer());

        // Assign the ability to the unit
        Ability ability = CardAbilityMap.getAbilityForCard(selectedCard.getName());
        newUnit.setAbility(ability);
        newUnit.setName(selectedCard.getName());

        //place the unit on the board
        gameState.getBoard().placeUnitOnTile(gameState,newUnit, tile, false);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            System.out.println("Error");
        }

        //assign health and attack
        int health = selectedCard.getBigCard().getHealth();
        int attack = selectedCard.getBigCard().getAttack();
        newUnit.setAttackPower(attack);
        newUnit.setCurrentHealth(health);
        newUnit.setMaximumHealth(health);

        //set on UI
        BasicCommands.setUnitAttack(out,newUnit,attack);
        BasicCommands.setUnitHealth(out,newUnit, health);
    }

    private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
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
                            BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                            gameState.addHighlightedTile(tile);
                        }
                        break; // Stop further movement in this direction
                    } else {
                        // Highlight empty tiles
                        BasicCommands.drawTile(out, tile, 1); // Highlight mode = 1
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
                        BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                        gameState.addHighlightedTile(tile); // Track highlighted tiles

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

    private void highlightValidAttackTiles(Tile unitTile, GameState gameState, ActorRef out) {
        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState, unitTile);
        Unit unit = gameState.getBoard().getUnitOnTile(unitTile);

        unit.getValidAttackTargets();

        // Highlight adjacent tiles with enemy units
        for (Tile tile : adjacentTiles) {
            Unit unitOnTile = gameState.getBoard().getUnitOnTile(tile);

            if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer() && unit.canAttack(unitOnTile)) {
                BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                gameState.addHighlightedTile(tile); // Track highlighted tiles

            }
        }
    }

    private boolean isAdjacentTile(Tile tile1, Tile tile2) {
        int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
        int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
        return dx <= 1 && dy <= 1; // Adjacent if within 1 tile in any direction
    }

    private Tile findPotentialAdjacentTile(GameState gameState, Tile targetTile) {
        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState, targetTile);

        for (Tile tile : adjacentTiles) {
            if (gameState.isHighlightedTile(tile)) {
                return tile;
            }
        }
        return null; // No highlighted adjacent tile found
    }
    private void handleAttack(ActorRef out, GameState gameState, Unit target) {
        Unit attacker = gameState.getSelectedUnit();

        Tile attackerTile = gameState.getBoard().getTileForUnit(attacker);
        Tile targetTile = gameState.getBoard().getTileForUnit(target);

        // If not adjacent to the target, move to an adjacent tile
        if (!isAdjacentTile(attackerTile, targetTile)) {
            Tile adjacentTile = findPotentialAdjacentTile(gameState, targetTile);
            if (adjacentTile == null) {
                return;
            }
            // Move the attacker to the adjacent tile
            gameState.getBoard().placeUnitOnTile(gameState,attacker, adjacentTile, false);

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
            gameState.getBoard().removeUnitFromTile(targetTile, out);
            triggerDeathwatchAbilities(out, gameState);

            if (target.isAvatar()) {
                Player winner = attacker.getOwner();
                //gameState.declareWin(winner);
            }
            target = null;
        } else {
            BasicCommands.setUnitHealth(out, target, target.getCurrentHealth());
        }

        if (target!=null && target.isAvatar()) {
            Player owner = target.getOwner();
            owner.setHealth(target.getCurrentHealth());
            if (owner == gameState.getPlayer1()) {
                BasicCommands.setPlayer1Health(out, owner);
            } else if (owner == gameState.getPlayer2()) {
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
                Tile newAttackerTile = gameState.getBoard().getTileForUnit(attacker);
                gameState.getBoard().removeUnitFromTile(newAttackerTile, out);
                triggerDeathwatchAbilities(out, gameState);

                if (attacker.isAvatar()) {
                    Player winner = target.getOwner();
                   //gameState.declareWin(winner);
                }
                attacker = null;
            } else {
                BasicCommands.setUnitHealth(out, attacker, attacker.getCurrentHealth());
            }
            // Trigger "On Hit" effect if the attacker is the avatar
            if (attacker != null && attacker == gameState.getCurrentPlayer().getAvatar()) {
                attacker.triggerOnHitEffect(out, gameState);
            }
        }

        if (attacker!=null && attacker.isAvatar()) {
            Player owner = attacker.getOwner();
            owner.setHealth(attacker.getCurrentHealth());
            if (owner == gameState.getPlayer1()) {
                BasicCommands.setPlayer1Health(out, owner);
            } else if (owner == gameState.getPlayer2()) {
                BasicCommands.setPlayer2Health(out, owner);
            }
        }

        gameState.clearAllHighlights(out); // Clear highlights after the attack
    }

    private void triggerDeathwatchAbilities(ActorRef out, GameState gameState) {
        // Get the unit map from the board
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(gameState.getBoard().getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the Deathwatch ability
            if (unit.getAbility() instanceof Deathwatch) {
                // Trigger the Deathwatch ability
                unit.getAbility().triggerAbility(out, gameState, tile);
            }
        }
    }

    private void triggerOpeningGambit(ActorRef out, GameState gameState) {
        // Get the unit map from the board
        boolean triggered = false;
        ConcurrentHashMap<Tile, Unit> unitMap = new ConcurrentHashMap<>(gameState.getBoard().getUnitMap());

        // Iterate through all units on the board
        for (Map.Entry<Tile, Unit> entry : unitMap.entrySet()) {
            Unit unit = entry.getValue();
            Tile tile = entry.getKey();

            // Check if the unit has the OpeningGambit ability
            if (unit.getAbility() instanceof OpeningGambit) {
                // Trigger the ability
                unit.getAbility().triggerAbility(out, gameState, tile);
                triggered = true;
            }
        }
        if (triggered) {
            BasicCommands.addPlayer1Notification(out,"Opening Gambit Triggered", 3);
        }
    }


    private boolean hasUnitOnXAxis(GameState gameState, Tile startTile) {
        int startX = startTile.getTilex();
        int startY = startTile.getTiley();

        // Check the tile to the left
        if (startX > 0) {
            Tile leftTile = gameState.getBoard().getTile(startX - 1, startY);
            if (leftTile != null && gameState.getBoard().getUnitOnTile(leftTile) != null) {
                return true; // Unit found to the left
            }
        }

        // Check the tile to the right
        if (startX < 8) {
            Tile rightTile = gameState.getBoard().getTile(startX + 1, startY);
            if (rightTile != null && gameState.getBoard().getUnitOnTile(rightTile) != null) {
                return true; // Unit found to the right
            }
        }

        return false; // No unit found on the adjacent tiles
    }

        

    private void handleMovement (GameState gameState, Tile targetTile, Unit selectedUnit) {
        Tile startTile = gameState.getSourceTile();

        // Check if the movement is diagonal
        int dx = Math.abs(targetTile.getTilex() - startTile.getTilex());
        int dy = Math.abs(targetTile.getTiley() - startTile.getTiley());
        boolean isDiagonal = (dx != 0 && dy != 0);

        if (isDiagonal) {
            // Check for obstacles adjacent on x-axis
            boolean hasUnitOnX = hasUnitOnXAxis(gameState, startTile);

            // Move y-axis first if there's a unit on the side (x-axis)
            boolean yfirst = hasUnitOnX;
            gameState.getBoard().placeUnitOnTile(gameState,selectedUnit, targetTile, yfirst);
        } else {
            // Non-diagonal movement, place the unit directly
            gameState.getBoard().placeUnitOnTile(gameState,selectedUnit, targetTile, false);
        }

            // Non-diagonal movement, place the unit directly
        // Mark the unit as moved
        selectedUnit.setHasMoved(true);

        //reset selection
        gameState.setSourceTile(null);
        gameState.setSelectedUnit(null);
    }
   
}
    
    



