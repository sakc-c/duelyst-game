package structures;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.List;

public class Provoke implements Ability {

    @Override
    public void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile) {
        Unit sourceUnit = gameState.getBoard().getUnitOnTile(sourceTile);

        List<Tile> adjacentTiles = gameState.getBoard().getAdjacentTiles(gameState,sourceTile);

        List<Unit> provokeUnits = gameState.getBoard().getUnitsWithAbility(Provoke.class);

        for (Tile adjacentTile : adjacentTiles) {
            Unit enemyUnit = gameState.getBoard().getUnitOnTile(adjacentTile);

            if(enemyUnit != null && enemyUnit.getOwner() != sourceUnit.getOwner()) {
                BasicCommands.addPlayer1Notification(out,"Provoke Active", 3);
                // Prevent the enemy unit from moving
                enemyUnit.setCanMove(false);

                // Restrict the enemy unit's attacks to the source unit or other units with Provoke
                enemyUnit.setValidAttackTargets(provokeUnits);

            }
        }



    }
}
