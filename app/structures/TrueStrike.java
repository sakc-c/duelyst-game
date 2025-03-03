
package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.List;

public class TrueStrike implements SpellEffect {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        // Clear all existing highlights
        gameState.clearAllHighlights(out);

        // Get tiles occupied by enemy units
        List<Tile> enemyTiles = gameState.getTilesOccupiedByEnemyPlayer();

        // Highlight tiles with enemy creatures (excluding avatar)
        for (Tile enemyTile : enemyTiles) {
            Unit unitOnTile = gameState.getBoard().getUnitOnTile(enemyTile);
            if (unitOnTile != null && !unitOnTile.isAvatar()) {
                BasicCommands.drawTile(out, enemyTile, 2); // Highlight mode = 2 (Red)
                gameState.addHighlightedTile(enemyTile); // Track highlighted tiles
            }
        }
    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        // Check if the target tile has an enemy unit
        Unit targetUnit = gameState.getBoard().getUnitOnTile(targetTile);
            int damage = 2;
            int updatedHealth = targetUnit.getCurrentHealth() - damage;
            targetUnit.setCurrentHealth(updatedHealth);
            System.out.println(updatedHealth);
            // Deal 2 damage to the enemy unit & update UI
            BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getCurrentHealth());

            // Check if the unit is dead after taking damage
            if (targetUnit.getCurrentHealth() <= 0) {
                gameState.getBoard().removeUnitFromTile(targetTile, out);

                try {
                    Thread.sleep(500); // 500ms delay
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


    }

}