package events;


import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import structures.HumanPlayer;

import java.util.ArrayList;
import java.util.List;

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
                   handleMovement(gameState,clickedTile,selectedUnit);
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
            if (!unitOnTile.hasMoved()) {
                highlightValidTiles(tilex, tiley, gameState, out);
                gameState.setSourceTile(clickedTile);
                gameState.setSelectedUnit(unitOnTile);
            } else if (!unitOnTile.hasAttacked()) {
                highlightValidAttackTiles(unitOnTile, gameState, out);
                gameState.setSourceTile(clickedTile);
                gameState.setSelectedUnit(unitOnTile);
            }
        }
    }


    private void handleCreatureCardClick(ActorRef out, GameState gameState, Tile clickedTile, Card selectedCard) {
        if (gameState.isHighlightedTile(clickedTile)) { // Check if the clicked tile is valid for summoning
            summonCreature(out, gameState, clickedTile);

            if (gameState.getCurrentPlayer() instanceof HumanPlayer) {
                HumanPlayer currentPlayer = (HumanPlayer) gameState.getCurrentPlayer();
                currentPlayer.playCard(selectedCard, out);
            }
            gameState.clearAllHighlights(out); // Clear highlights after summoning
        } else { // Clicked on an invalid tile, reset selection
            gameState.clearAllHighlights(out);
            gameState.setSelectedCard(null);
        }
    }

    private void handleSpellCardClick(ActorRef out, GameState gameState, Tile clickedTile, Unit unitOnTile) {
        // Handle spell-specific logic (e.g., cast spell on the clicked tile)
        // For now, just clear highlights
        gameState.clearAllHighlights(out);
    }

    private void summonCreature(ActorRef out, GameState gameState, Tile tile) {
        Card selectedCard = gameState.getSelectedCard();
        Unit newUnit = BasicObjectBuilders.loadUnit(selectedCard.getUnitConfig(), gameState.getNextUnitId(), Unit.class);
        newUnit.setOwner(gameState.getCurrentPlayer());
        gameState.getBoard().placeUnitOnTile(newUnit, tile,false);
        newUnit.setHasMoved(true);
        newUnit.setHasAttacked(true);
    }

    private void highlightValidTiles(int tileX, int tileY, GameState gameState, ActorRef out) {
        gameState.clearAllHighlights(out);

        int cardinalRange = 2;
        int diagonalRange = 1;

        // Define movement ranges
        int[][] validDirections = {{-1, 0}, {1, 0}, // Left, Right
                {0, -1}, {0, 1}, // Up, Down
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals, both x and y not 0
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

                    //if the tile is blocked by another unit
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

    private void highlightValidAttackTiles(Unit unit, GameState gameState, ActorRef out) {
        gameState.clearAllHighlights(out);
        Tile unitTile = gameState.getBoard().getTileForUnit(unit);
        List<Tile> adjacentTiles = getAdjacentTiles(gameState, unitTile);

        // Highlight adjacent tiles with enemy units
        for (Tile tile : adjacentTiles) {
            Unit unitOnTile = gameState.getBoard().getUnitOnTile(tile);

            if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer()) {
                BasicCommands.drawTile(out, tile, 2); // Highlight mode = 2 (Red)
                gameState.addHighlightedTile(tile); // Track highlighted tiles
                System.out.println("Highlighting tile for attack: " + tile.getTilex() + ", " + tile.getTiley()); // Debugging
            }
        }

        try {
            Thread.sleep(200); // Simulate delay for highlighting
        } catch (InterruptedException e) {
            System.out.println("Error");
        }
    }

    private boolean isAdjacentTile(Tile tile1, Tile tile2) {
        int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
        int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
        return dx <= 1 && dy <= 1; // Adjacent if within 1 tile in any direction
    }

    private List<Tile> getAdjacentTiles(GameState gameState, Tile tile) {
        List<Tile> adjacentTiles = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, // Left, Right
                {0, -1}, {0, 1}, // Up, Down
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // Diagonals
        };

        for (int[] dir : directions) {
            int newX = tile.getTilex() + dir[0];
            int newY = tile.getTiley() + dir[1];

            // Check if the new coordinates are within the board bounds
            if (newX >= 0 && newX < 9 && newY >= 0 && newY < 5) {
                adjacentTiles.add(gameState.getBoard().getTile(newX, newY));
            }
        }

        return adjacentTiles;
    }

    private Tile findPotentialAdjacentTile(GameState gameState, Tile targetTile) {
        List<Tile> adjacentTiles = getAdjacentTiles(gameState, targetTile);

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

        //if not adjacent to the target
        if (!isAdjacentTile(attackerTile, targetTile)) {
            // Move to an adjacent tile
            Tile adjacentTile = findPotentialAdjacentTile(gameState, targetTile);
            if (adjacentTile == null) {
                return;
            }
            // Move the attacker to the adjacent tile
            gameState.getBoard().placeUnitOnTile(attacker, adjacentTile,false);

            // Wait for movement to complete
            try {
                Thread.sleep(200); // Simulate movement delay
            } catch (InterruptedException e) {

            }
        }
        // Perform the attack
        BasicCommands.playUnitAnimation(out, attacker, UnitAnimationType.attack);
        target.takeDamage(attacker.getAttackPower());
        BasicCommands.setUnitHealth(out, target, target.getCurrentHealth());

        /*
         * // Check if the target is defeated if (target.getCurrentHealth() <= 0) {
         * gameState.getBoard().removeUnit(target); // Remove from the board
         * BasicCommands.deleteUnit(out, target); // Remove from the UI
         *
         * }
         */

        // Mark the attacker as having moved and attacked
        attacker.setHasMoved(true);
        attacker.setHasAttacked(true);
        gameState.clearAllHighlights(out); // Clear highlights after the attack
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
            gameState.getBoard().placeUnitOnTile(selectedUnit, targetTile, yfirst);
        } else {
            // Non-diagonal movement, place the unit directly
            gameState.getBoard().placeUnitOnTile(selectedUnit, targetTile,false);
        }

        // Mark the unit as moved
        selectedUnit.setHasMoved(true);

        //reset selection
        gameState.setSourceTile(null);
        gameState.setSelectedUnit(null);
    }


}
