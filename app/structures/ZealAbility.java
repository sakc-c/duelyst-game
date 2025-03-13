package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.Player;

public class ZealAbility implements Ability, OnHitEventListener {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile tile) {
        // This method is called when any unit with this ability is placed on the board
        Player currentPlayer = gameState.getCurrentPlayer();
        Unit unit = gameState.getBoard().getUnitOnTile(tile);

            if (unit.getOwner() == currentPlayer) {
                Unit avatar = currentPlayer.getAvatar();
                avatar.addOnHitEventListener(this);

            }
        }

    //this is called when avatar takes hit
    @Override
    public void onHit(ActorRef out, GameState gameState) {

        // Get all units with ZealAbility for the current player
        for (Unit unit : gameState.getBoard().getUnitsWithAbility(ZealAbility.class)) {
            int newAttack = unit.getAttackPower() + 2;
            unit.setAttackPower(newAttack);

            // Update the UI with new attack power
            BasicCommands.setUnitAttack(out, unit, newAttack);
            BasicCommands.addPlayer1Notification(out, "Zeal Triggered", 3);
        }

        try {
            Thread.sleep(100); // Small delay for UI update
        } catch (InterruptedException e) {
            System.out.println("Error");
        }

    }
}