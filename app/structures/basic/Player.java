package structures.basic;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic representation of of the Player. A player
 * has health and mana.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public abstract class Player {

	int health;
	int mana;
	private Unit avatar;
	private int artifactRobustness = 0; // 0 means no artifact is equipped
    private boolean hasArtifact = false;
	private List<Card> hand;  // Cards that the player can play

//	private int id;
	
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
	}
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
		this.hand = new ArrayList<>();
	}
	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
        this.health = Math.min(health, 20);
	}
	public int getMana() {
		return mana;
	}
	public void setMana(int mana) {
		this.mana = mana;
	}

    public Unit getAvatar() {
		return avatar;
    }

	public void setAvatar(Unit avatar) {
		this.avatar = avatar;
	}
	
	public void equipArtifact(int robustness) {
        this.artifactRobustness = robustness;
        this.hasArtifact = true;
    }

    public void takeDamage(int damage) {
		if (hasArtifact) {
			// Reduce artifact robustness instead of player health
			artifactRobustness -= damage;

			// Check if the artifact is destroyed
			if (artifactRobustness <= 0) {
				hasArtifact = false; // Remove the artifact
				artifactRobustness = 0;
			}
		} else {
			// If no artifact, reduce player health
			this.health -= damage;
		}
    }

    public boolean hasArtifact() {
        return hasArtifact;
    }

    public int getArtifactRobustness() {
        return artifactRobustness;
    }

	public void displayHand(ActorRef out) {
		for (int i = 0; i < hand.size(); i++) {
			Card card = hand.get(i);
			BasicCommands.drawCard(out, card, i+1, 0);
			try { Thread.sleep(100); } catch (InterruptedException e) { System.out.println("error occured"); }
		}
	}

	public List<Card> getHand() {
		return hand;
	}

	public abstract void playCard (Card card, ActorRef out, GameState gameState);

	public abstract void drawCard();
}

