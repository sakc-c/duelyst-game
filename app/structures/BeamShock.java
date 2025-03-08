package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.SpellEffect;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;



public class BeamShock implements SpellEffect {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        gameState.clearAllHighlights(out);
        //iterate to check tiles occupied by units
        for (Tile currentTile : gameState.getTilesOccupiedByEnemyPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(currentTile);
            if (unit != null && !unit.isAvatar()) {
                //BasicCommands.drawTile(out, currentTile, 1); // Highlight mode = 1 (Blue)
                gameState.addHighlightedTile(currentTile);
            }
        }

    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);
        unit.setStunned(true);
        BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);
        BasicCommands.addPlayer1Notification(out,"unit is stunned",3);

    }
}
