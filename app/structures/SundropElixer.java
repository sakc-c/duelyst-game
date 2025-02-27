package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

public class SundropElixer implements SpellEffect{
    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        gameState.clearAllHighlights(out);
        //iterate through all the tiles on the board to find AI's friendly units
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 5; y++) {
                Tile currentTile = gameState.getBoard().getTile(x, y);
                Unit unit = gameState.getBoard().getUnitOnTile(currentTile);

                // Check if the unit belongs to AI and is not at full health
                if (unit != null && unit.getOwner() == gameState.getOpponentPlayer() && unit.getCurrentHealth() <
                        unit.getMaxHealth()) {
                    BasicCommands.drawTile(out, currentTile, 1); // Blue highlight for healing
                    gameState.addHighlightedTile(currentTile);
                }
            }
        }
    }

    @Override
    public void applyEffect (ActorRef out, GameState gameState, Tile targetTile){
        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);
        if (unit == null || unit.getOwner() != gameState.getOpponentPlayer() || unit.getCurrentHealth() ==
                unit.getMaxHealth()) {
            return;
        }

        int healAmount = 4;
        unit.heal(healAmount);

        // Update the UI to reflect the new health
        BasicCommands.setUnitHealth(out, unit, unit.getCurrentHealth());

        // Play a healing effect animation
        EffectAnimation healingEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
        BasicCommands.playEffectAnimation(out, healingEffect, targetTile);

        // Play an idle animation after healing
        BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);

    }
}
