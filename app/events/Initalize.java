package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import demo.CommandDemo;
import demo.Loaders_2024_Check;
import structures.GameState;
<<<<<<< HEAD
import structures.basic.Board;
=======
import commands.BasicCommands;
>>>>>>> origin/main

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * 
 * { 
 *   messageType = “initalize”
 * }
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// hello this is a change
		
		gameState.gameInitalised = true;
		
		gameState.something = true;

		Board board = new Board(out); // initalizing the board
		gameState.setBoard(board); // Store the board in GameState

		
		Player aiPlayer = gameState.getAIPlayer();

        aiPlayer.setHealth(20); 

        // Use BasicCommands to display the AI player's health on the UI
        BasicCommands.setPlayer2Health(out, aiPlayer);
		

		// User 1 makes a change
		//CommandDemo.executeDemo(out); // this executes the command demo, comment out this when implementing your solution
		//Loaders_2024_Check.test(out);
	}

}


