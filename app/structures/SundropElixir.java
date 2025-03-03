package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.EffectAnimation;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

public class SundropElixir implements SpellEffect{
    @Override
    public void highlightValidTargets(ActorRef out, GameState gameState, Tile tile) {
        gameState.clearAllHighlights(out);
        // Get all AI-controlled unit tiles from GameState
        for (Tile currentTile : gameState.getTilesOccupiedByCurrentPlayer()) {
            Unit unit = gameState.getBoard().getUnitOnTile(currentTile);

            // Check if the unit is NOT the AI's avatar and is not at full health
            if (unit != null && !unit.isAvatar() && unit.getCurrentHealth() < unit.getMaxHealth()) {
                BasicCommands.drawTile(out, currentTile, 1); // Blue highlight for healing
                gameState.addHighlightedTile(currentTile);
            }
        }
    }

    @Override
    public void applyEffect (ActorRef out, GameState gameState, Tile targetTile) {

        Unit unit = gameState.getBoard().getUnitOnTile(targetTile);
        int amount = 4;
        unit.heal(amount);

        // Update the UI to reflect the new health
        BasicCommands.setUnitHealth(out, unit, unit.getCurrentHealth());

        // Play a healing effect animation
        EffectAnimation healingEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
        BasicCommands.playEffectAnimation(out, healingEffect, targetTile);

        // Play an idle animation after healing
        BasicCommands.playUnitAnimation(out, unit, UnitAnimationType.idle);
    }
}
