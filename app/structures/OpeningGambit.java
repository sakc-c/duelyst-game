package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.List;

public class OpeningGambit implements Ability {
    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);
        String unitName = sourceUnit.getName();

        switch (unitName) {
            case "Gloom Chaser":
                triggerGloomChaserAbility(out, gameState, sourceTile);
                break;
            case "Nightsorrow Assassin":
                triggerNightsorrowAssassinAbility(out, gameState, sourceTile);
                break;
            case "Silverguard Squire":
                triggerSilverguardAbility(out, gameState, sourceTile);
                break;
        }
    }

    private void triggerSilverguardAbility(ActorRef out, GameState gameState, Tile sourceTile) {

    }

    private void triggerGloomChaserAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        // Get the tile directly behind the Gloom Chaser (to its left for the human player)
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);
        boolean isHumanPlayer = sourceUnit.getOwner() instanceof HumanPlayer;
        Tile behindTile = gameState.getBoard().getTileBehind(sourceTile, isHumanPlayer);

        // Check if the tile is empty
        if (behindTile != null && gameState.getBoard().getUnitOnTile(behindTile) == null) {
            // Summon a Wraithling on the behindTile
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
            wraithling.setOwner(sourceUnit.getOwner());
            wraithling.setCurrentHealth(1); // Wraithlings have 1 health
            wraithling.setAttackPower(1);   // Wraithlings have 1 attack
            gameState.getBoard().placeUnitOnTile(gameState,wraithling, behindTile, false);

            // Add a small delay for place Unit to complete
            try {
                Thread.sleep(200); // 200ms delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update the UI
            BasicCommands.setUnitHealth(out, wraithling, wraithling.getCurrentHealth());
            BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttackPower());
            BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);
        }
    }

    private void triggerNightsorrowAssassinAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        // Get all adjacent tiles
        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState,sourceTile);

        // Find an enemy unit below max health
        for (Tile adjacentTile : adjacentTiles) {
            Unit enemyUnit = gameState.getBoard().getUnitOnTile(adjacentTile);
            if (enemyUnit != null && enemyUnit.getOwner() == gameState.getOpponentPlayer() && enemyUnit.getCurrentHealth() < enemyUnit.getMaxHealth()) {
                // Destroy the enemy unit
                gameState.getBoard().removeUnitFromTile(adjacentTile, out);
                break; // Only destroy one unit
            }
        }
    }
}
