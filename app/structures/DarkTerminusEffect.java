package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import structures.basic.Tile;
import utils.StaticConfFiles;

public class DarkTerminusEffect implements SpellEffect {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        // Clear all existing highlights
        gameState.clearAllHighlights(out);

        // Iterate through all tiles on the board
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 5; y++) {
                Tile currentTile = gameState.getBoard().getTile(x, y);
                Unit unitOnTile = gameState.getBoard().getUnitOnTile(currentTile);

                // Highlight tiles with enemy creatures
                if (unitOnTile != null && unitOnTile.getOwner() == gameState.getOpponentPlayer() && !unitOnTile.isAvatar()) {
                    BasicCommands.drawTile(out, currentTile, 2); // Highlight mode = 2 (Red)
                    gameState.addHighlightedTile(currentTile); // Track highlighted tiles
                }
            }
        }
    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        Unit targetUnit = gameState.getBoard().getUnitOnTile(targetTile);

        // Check if the target is an enemy creature
        if (targetUnit != null && targetUnit.getOwner() == gameState.getOpponentPlayer() && !targetUnit.isAvatar()) {
            // Destroy the enemy creature
            gameState.getBoard().removeUnitFromTile(targetTile, out);
            BasicCommands.deleteUnit(out, targetUnit); // Remove the unit from the UI

            // Summon a Wraithling on the same tile
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
            wraithling.setOwner(gameState.getCurrentPlayer());

            // Place the Wraithling on the tile
            gameState.getBoard().placeUnitOnTile(wraithling, targetTile, false);

            // Update the UI
            BasicCommands.drawUnit(out, wraithling, targetTile);
            BasicCommands.setUnitHealth(out, wraithling, 1); // Wraithlings have 1 health
            BasicCommands.setUnitAttack(out, wraithling, 1); // Wraithlings have 1 attack
            BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);

            // Add a small delay for visual effect
            try {
                Thread.sleep(500); // 500ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
