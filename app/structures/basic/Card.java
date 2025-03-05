package structures.basic;


import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.*;
import utils.BasicObjectBuilders;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the base representation of a Card which is rendered in the player's hand.
 * A card has an id, a name (cardname) and a manacost. A card then has a large and mini
 * version. The mini version is what is rendered at the bottom of the screen. The big
 * version is what is rendered when the player clicks on a card in their hand.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Card {
	
	int id;
	
	String cardname;
	int manacost;
	
	MiniCard miniCard;
	BigCard bigCard;
	
	boolean isCreature;
	String unitConfig;
	
	public Card() {};

	public Card(int id, String cardname, int manacost, MiniCard miniCard, BigCard bigCard, boolean isCreature, String unitConfig) {
		super();
		this.id = id;
		this.cardname = cardname;
		this.manacost = manacost;
		this.miniCard = miniCard;
		this.bigCard = bigCard;
		this.isCreature = isCreature;
		this.unitConfig = unitConfig;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getCardname() {
		return cardname;
	}
	public void setCardname(String cardname) {
		this.cardname = cardname;
	}
	public int getManacost() {
		return manacost;
	}
	public void setManacost(int manacost) {
		this.manacost = manacost;
	}
	public MiniCard getMiniCard() {
		return miniCard;
	}
	public void setMiniCard(MiniCard miniCard) {
		this.miniCard = miniCard;
	}
	public BigCard getBigCard() {
		return bigCard;
	}
	public void setBigCard(BigCard bigCard) {
		this.bigCard = bigCard;
	}
	public boolean getIsCreature() {
		return isCreature;
	}
	public void setIsCreature(boolean isCreature) {
		this.isCreature = isCreature;
	}
	public void setCreature(boolean isCreature) {
		this.isCreature = isCreature;
	}
	public boolean isCreature() {
		return isCreature;
	}
	public String getUnitConfig() {
		return unitConfig;
	}
	public void setUnitConfig(String unitConfig) {
		this.unitConfig = unitConfig;
	}


	public String getName() { return this.cardname;}

    public void summonCreature(ActorRef out, GameState gameState, Tile clickedTile) {
		//trigger openingGambit abilities of existing units on the board
		gameState.triggerOpeningGambit(out);

		Card selectedCard = gameState.getSelectedCard();
		Unit newUnit = BasicObjectBuilders.loadUnit(selectedCard.getUnitConfig(), gameState.getNextUnitId(), Unit.class);
      	if (newUnit != null) {
			newUnit.setOwner(gameState.getCurrentPlayer());
		}

		// Assign the ability to the unit
		Ability ability = CardAbilityMap.getAbilityForCard(selectedCard.getName());
		newUnit.setAbility(ability);
		newUnit.setName(selectedCard.getName());

		//place the unit on the board
		gameState.getBoard().placeUnitOnTile(gameState,newUnit, clickedTile, false);

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.out.println("Error");
		}

		//assign health and attack
		int health = selectedCard.getBigCard().getHealth();
		int attack = selectedCard.getBigCard().getAttack();
		newUnit.setAttackPower(attack);
		newUnit.setCurrentHealth(health);
		newUnit.setMaximumHealth(health);

		//set on UI
		BasicCommands.setUnitAttack(out,newUnit,attack);
		BasicCommands.setUnitHealth(out,newUnit, health);
    }

}
