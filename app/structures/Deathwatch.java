package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import java.util.ArrayList;
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
                BasicCommands.addPlayer1Notification(out,"Deathwatch Triggered", 3);
                break;
            case "Shadow Watcher":
                sourceUnit.setAttackPower(sourceUnit.getAttackPower() + 1); // Permanent +1 Attack
                sourceUnit.setCurrentHealth(sourceUnit.getCurrentHealth() + 1); // Permanent +1 Health
                BasicCommands.setUnitAttack(out,sourceUnit, sourceUnit.getAttackPower());
                BasicCommands.setUnitHealth(out,sourceUnit,sourceUnit.getAttackPower());
                BasicCommands.addPlayer1Notification(out,"Deathwatch Triggered", 3);
                break;
            case "Bloodmoon Priestess": //summon wraithling on randomly selected unoccupied tile
                Unit unit = gameState.getBoard().getUnitOnTile(sourceTile);
                Player currentPlayer = unit.getOwner();

                // Get adjacent tiles
                List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(sourceTile);

                // Filter unoccupied tiles
                List<Tile> unoccupiedTiles = new ArrayList<>();
                for (Tile tile : adjacentTiles) {
                    if (gameState.getBoard().getUnitOnTile(tile) == null) {
                        unoccupiedTiles.add(tile);
                    }
                }

                // If there are unoccupied tiles, summon a Wraithling on a random one
                if (!unoccupiedTiles.isEmpty()) {
                    Random random = new Random();
                    Tile randomTile = unoccupiedTiles.get(random.nextInt(unoccupiedTiles.size()));

                    // Summon a Wraithling
                    System.out.println("current player is" + currentPlayer);
                    Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, gameState.getNextUnitId(), Unit.class);
                    wraithling.setOwner(currentPlayer);
                    wraithling.setCurrentHealth(1); // Wraithlings have 1 health
                    wraithling.setAttackPower(1);   // Wraithlings have 1 attack
                    gameState.getBoard().placeUnitOnTile(gameState,wraithling, randomTile, false);

                    // Add a small delay for place Unit to complete
                    try {
                        Thread.sleep(500); // 500ms delay
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Update the UI
                    BasicCommands.setUnitHealth(out, wraithling, wraithling.getCurrentHealth());
                    BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttackPower());
                    BasicCommands.playUnitAnimation(out, wraithling, UnitAnimationType.idle);
                }
                BasicCommands.addPlayer1Notification(out,"Deathwatch Triggered", 3);
                break;
            case "Shadowdancer":
                // Deal 1 damage to the enemy avatar and heal the player by 1
                Unit shadow = gameState.getBoard().getUnitOnTile(sourceTile);
                Player current = shadow.getOwner();

                Player opponent = null;
                if (current == gameState.getPlayer1()) {
                    opponent = gameState.getPlayer2();
                } else if (current == gameState.getPlayer2()) {
                    opponent = gameState.getPlayer1();
                }

                Unit enemyAvatar = null;
                if (opponent != null) {
                    enemyAvatar = opponent.getAvatar();
                }

                int updatedHealth = enemyAvatar.getCurrentHealth() - 1;
                enemyAvatar.setCurrentHealth(updatedHealth);
                BasicCommands.setUnitHealth(out, enemyAvatar,updatedHealth);

                opponent.setHealth(updatedHealth);
                if (opponent == gameState.getPlayer1()) {
                    BasicCommands.setPlayer1Health(out,opponent);
                } else if (opponent == gameState.getPlayer2()) {
                    BasicCommands.setPlayer2Health(out,opponent);
                }

                Unit currentAvatar = current.getAvatar();

                int updatedHealthCurrent = currentAvatar.getCurrentHealth() + 1;
                if (updatedHealthCurrent > 20) {
                    updatedHealthCurrent = 20;
                }
                currentAvatar.setCurrentHealth(updatedHealthCurrent);
                BasicCommands.setUnitHealth(out, currentAvatar,updatedHealthCurrent);

                current.setHealth(updatedHealthCurrent);
                if (current == gameState.getPlayer1()) {
                    BasicCommands.setPlayer1Health(out,current);
                } else if (opponent == gameState.getPlayer2()) {
                    BasicCommands.setPlayer2Health(out,current);
                }
                BasicCommands.addPlayer1Notification(out,"Deathwatch Triggered", 3);
                break;
        }
    } 
}
