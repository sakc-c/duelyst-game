package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.List;
import java.util.Random;

public class Deathwatch implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);
        String unitName = sourceUnit.getName();

        switch (unitName) {
            case "Bad Omen":
                int updatedattack = sourceUnit.getAttackPower() + 1;
                sourceUnit.setAttackPower(updatedattack); // Permanent +1 Attack
                BasicCommands.setUnitAttack(out,sourceUnit, updatedattack);
                break;
            case "Shadow Watcher":
                sourceUnit.setAttackPower(sourceUnit.getAttackPower() + 1); // Permanent +1 Attack
                sourceUnit.setCurrentHealth(sourceUnit.getCurrentHealth() + 1); // Permanent +1 Health
                break;
            case "Bloodmoon Priestess": //summon wraithling on randomly selected unoccupied tile
                List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState,sourceTile);

                // Check if there are any adjacent tiles
                if (!adjacentTiles.isEmpty()) {
                    // Pick a random tile from the list
                    Random random = new Random();
                    Tile randomAdjacentTile = adjacentTiles.get(random.nextInt(adjacentTiles.size()));

                    // Summon a Wraithling on the random adjacent tile
                    Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
                    wraithling.setOwner(sourceUnit.getOwner()); // Set the owner of the Wraithling
                    gameState.getBoard().placeUnitOnTile(wraithling, randomAdjacentTile,false);
                }
                break;
            case "Shadowdancer":
                // Deal 1 damage to the enemy avatar and heal the player by 1
                Unit enemyAvatar = gameState.getOpponentPlayer().getAvatar();
                enemyAvatar.setCurrentHealth(enemyAvatar.getCurrentHealth() - 1);

                Unit currentAvatar = gameState.getCurrentPlayerAvatar();
                gameState.getCurrentPlayer().setHealth(currentAvatar.getCurrentHealth() + 1);
                break;
        }
    }
}
