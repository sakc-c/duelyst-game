package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.Player;

public class ZealAbility implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile, Tile targetTile) {
        // Not used for ZealAbility
    }

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile tile) {
        // This method is called when the avatar takes damage
        Player currentPlayer = gameState.getCurrentPlayer();

        // Iterate through all units on the board
        for (Unit unit : gameState.getBoard().getUnitsWithAbility(ZealAbility.class)) {
            // Check if the unit belongs to the current player
            if (unit.getOwner() == currentPlayer) {
                // Increase the unit's attack by +2
                int newAttack = unit.getAttackPower() + 2;
                unit.setAttackPower(newAttack);

                // this will update the UI
                if (out != null) {
                    BasicCommands.setUnitAttack(out, unit, newAttack);
                    try {
                        Thread.sleep(100); // Small delay for UI update
                    } catch (InterruptedException e) {
                        System.out.println("Error");
                    }
                }

                System.out.println("ZealAbility: " + unit.getName() + " attack increased to " + newAttack);
            }
        }
    }
}