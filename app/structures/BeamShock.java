package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;



public class BeamShock implements SpellEffect {

    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        gameState.clearAllHighlights(out);
        //iterate to check tiles occupied by units
        System.out.println("highlightValidTargets called for BeamShock");//Debug line
        for (Tile currentTile : gameState.getTilesOccupiedByCurrentPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(currentTile);
            if (unit != null && !unit.isAvatar()) {
                System.out.println("Highlighting unit on tile: " + currentTile);  // Debug line
                BasicCommands.drawTile(out, currentTile, 1); // Highlight mode = 1 (Blue)
                gameState.addHighlightedTile(currentTile);
            }
        }

    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);

        //apply stunned effect so human player's unit cannot move or attack for 1 turn
        unit.setCanMove(false);
        unit.setCanAttack(false);

        // Track stunned unit in GameState
        gameState.addStunnedUnit(unit);
        BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);

    }
}
