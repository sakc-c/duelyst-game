package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;

/* Implementation of BeamShock - Identify enemy units on the board which are not the avatar
* and stun one of those units for one turn */


public class BeamShock implements SpellEffect {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        gameState.clearAllHighlights(out);
        //iterate to check tiles occupied by units
        for (Tile currentTile : gameState.getTilesOccupiedByEnemyPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(currentTile);
            if (unit != null && !unit.isAvatar()) {
                gameState.addRedHighlightedTile(currentTile);
            }
        }

    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        //Stun enemy units for one turn
        BasicCommands.drawTile(out, targetTile, 2);
        BasicCommands.addPlayer1Notification(out,"unit is stunned",3);

        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);
        unit.setStunned(true);
        BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);
        BasicCommands.drawTile(out, targetTile, 0);
    }
}
