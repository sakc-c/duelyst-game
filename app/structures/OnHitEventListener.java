package structures;

import akka.actor.ActorRef;

public interface OnHitEventListener {
    void onHit(ActorRef out, GameState gameState);
}
