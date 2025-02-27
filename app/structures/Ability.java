package structures;

import akka.actor.ActorRef;
import structures.basic.Tile;
import structures.basic.Unit;

public interface Ability {
    void triggerAbility(ActorRef out, GameState gameState, Tile sourceTile, Tile targetTile);
}