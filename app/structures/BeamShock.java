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
        for (Tile currentTile: gameState.getTilesOccupiedByEnemyPlayer()){
            Unit unit = gameState.getBoard().getUnitOnTile(currentTile);
            //check if the unit is not null and not avtarUnit
            if (unit !=null && !unit.isAvatar()){
                BasicCommands.drawTile(out, currentTile, 1);//highlight the tile
            }

        }

    }

    @Override
    public void applyEffect(ActorRef out, GameState gameState, Tile targetTile) {
        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);
        if (unit == null || unit.isAvatar()){// exclude the player Avatar
            return;
        }
        //apply stunned effect so human player's unit cannot ove or attack for 1 turn
        unit.setCanMove(false);
        unit.setCanAttack(false);

        // Track stunned unit in GameState
        gameState.addStunnedUnit(unit);
        BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);

    }
}
