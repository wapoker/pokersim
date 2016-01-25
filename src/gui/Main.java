package gui;

import human.NI;
import ai.AI;

public class Main {
	
	private static final int PLAYERS = 2, START_CHIPS = 1000, SB = 10, BB = 20;
	
	private int activePlayers = PLAYERS;
	private int sbPos = 0, curInput = 0;
	private Player[] players = new Player[PLAYERS];
	private boolean[] deck = new boolean[52];
	private int[][] hands = new int[PLAYERS][2];
	private int[] chips = new int[PLAYERS];
	private int[] community = new int[5];
	private int[] input = new int[PLAYERS];
	
	public static void main(String[] args) {
		new Main();
	}
	
	public Main() {
		initGame();
		while(chips[0] > 0 && chips[1] > 0)
			runRound();
	}

	/**
	 * initializes the game by giving all the chips to the players
	 * */
	private void initGame() {
		players[0] = new NI();
		players[1] = new AI();
		for(int i = 0; i < PLAYERS; i++) {
			chips[i] = START_CHIPS;
		}
	}

	/**
	 * initializes a round by creating a new deck and dealing out the hand cards
	 * */
	private void initRound() {
		println("\n=============================");
		for(int i = 0; i < 52; i++)
			deck[i] = true;
		for(int i = 0; i < 5; i++)
			community[i] = -1;
		for(int i = 0; i < PLAYERS; i++) {
			for(int j = 0; j < 2; j++) {
				hands[i][j] = drawCard();
			}
		}
	}
	
	/**
	 * runs one full round from dealing cards to paying out the pot
	 * */
	private void runRound() {
		initRound();
		activePlayers = PLAYERS;
		curInput = 0;
		input = new int[PLAYERS];
		printGameState();
		for(int i = 0; i < 4; i++) {
			simulateBettingRound();
			if(i == 0) {
				community[0] = drawCard();
				community[1] = drawCard();
				community[2] = drawCard();
			}
			if(activePlayers == 1) {
				for(int j = (i==0?3:3+i); j < 5; j++)
					community[j] = drawCard();
				i = 3;
			}
			if(i == 1 || i == 2)
				community[2 + i] = drawCard();
			if(i == 3) {
				determineWinner();
				sbPos = (sbPos+1)%PLAYERS;
			}
			printGameState();
		}
	}
	
	/**
	 * simulates a full betting round
	 * @param preflopPhase to determine whether blinds are required
	 * */
	private void simulateBettingRound() {
		int activePlayer = sbPos;
		int actionsSelected = 0;
		do {
			int preferredInput = 0;
			// select action
			if(curInput == 0)
				preferredInput = SB;
			else if(curInput == SB)
				preferredInput = BB;
			else {
				//preferredInput = curInput;
				preferredInput = players[activePlayer].getAction(getGameStateArray(activePlayer));
				actionsSelected++;
			
				// check validity of action
				if(preferredInput > curInput && !(preferredInput >= curInput + BB)) {
					System.err.println("Die Mindesterhöhung beträgt 1BB!");
					System.exit(0);
				}
				if(preferredInput < curInput-input[activePlayer] && preferredInput != input[activePlayer]) {
					System.err.println("Mindesteinsatz bezahlen oder folden! [" + preferredInput + "<" + curInput + "]");
					System.exit(0);
				}
			}
			
			chips[activePlayer] -= preferredInput - input[activePlayer];
			input[activePlayer] = preferredInput;
			
			// interpret action
			if(input[activePlayer] >= curInput)
				curInput = input[activePlayer];
			else if(input[activePlayer] < curInput) {
				hands[activePlayer][0] = -1;
				hands[activePlayer][1] = -1;
				activePlayers--;
			}

			// end bet round
			if((input[activePlayer] == curInput && (actionsSelected >= activePlayers)) || activePlayers == 1)
				return;
			
			// select next player
			do {
				activePlayer = (activePlayer+1)%PLAYERS;
			} while(hands[activePlayer][0] == -1);
		} while(true);
	}
	
	
	/**
	 * brings all visible information of the player, who is about to make a input decision, into a String
	 * @return String representing known features of the current gameState of the player
	 * */
	private double[] getGameStateArray(int player) {
		double[] gameState = new double[13];
		gameState[0] = BB;
		gameState[1] = sbPos;
		gameState[2] = hands[player][0];
		gameState[3] = hands[player][1];
		gameState[4] = chips[player];
		gameState[5] = input[player];
		gameState[6] = chips[(player+1)%2];
		gameState[7] = input[(player+1)%2];
		for(int i = 0; i < 5; i++)
			gameState[i + 8] = community[i];
		return gameState;
	}
	
	/**
	 * determines which players win and pays pot out to them
	 * */
	private void determineWinner() {
		
		double score[] = new double[PLAYERS];
		int maxPot = 0;
		
		for(int i = 0; i < PLAYERS; i++) {
			maxPot += input[i];
		}
		
		while(maxPot > 0) {
			double maxScore = 0;
			int maxScorePlayers = 0;
			
			for(int i = 0; i < PLAYERS; i++) {
				score[i] = calcScore(i);
				if(score[i] > maxScore) {
					maxScore = score[i];
					maxScorePlayers = 1;
				} else if(score[i] == maxScore)
					maxScorePlayers++;
			}
			
			int lowestInput = -1;
			
			for(int i = 0; i < PLAYERS; i++) {
				if(score[i] == maxScore && (input[i] < lowestInput || lowestInput == -1))
					lowestInput = input[i];
			}
			
			int pot = 0;
			
			for(int i = 0; i < PLAYERS; i++) {
				if(input[i] > lowestInput) {
					pot += lowestInput;
					input[i] -= lowestInput;
				} else {
					pot += input[i];
					input[i] = 0;
				}
			}
			
			for(int i = 0; i < PLAYERS; i++) {
				if(score[i] == maxScore)
					chips[i] += pot / maxScorePlayers;
			}
			
			maxPot -= pot;
		}
	}
	
	/**
	 * calculates a score representing a players hand strength at the current game state
	 * @param player id of the player who's hand will be used for calculation
	 * @return score (0-9) representing how well a player's hand + community cards match (the higher the better)
	 * */
	private double calcScore(int player) {
		boolean[] cards = new boolean[52];
		
		// bereits gefoldet -> score = 0
		if(hands[player][0] == -1)
			return 0;
		
		for(int j = 0; j < 2; j++)
			cards[hands[player][j]] = true; 
		for(int j = 0; j < community.length && community[j] != -1; j++)
			cards[community[j]] = true; 

		// straight/royal flush
		for(int highestCard = 12; highestCard >= 3; highestCard--) {
			for(int color = 0; color < 4; color++) {
				for(int j = highestCard; j >= highestCard-4; j--) {
					if(j == -1)
						j = 12;
					if(!cards[j * 4 + color])
						break;
					if(j == highestCard-4) {
						return highestCard == 12 ? 9 : 8 + highestCard / 13.0;
					}
				}
			}
		}
		
		// four of a kind
		if(hasNOfAKind(cards, 4, -1) != -1)
			return 7 + hasNOfAKind(cards, 4, -1) / 13.0;
		
		// full house
		int firstpair = hasNOfAKind(cards, 3, 1);
		if(firstpair != -1) {
			if(hasNOfAKind(cards, 2, firstpair) != -1)
				return 6 + firstpair / 13.0 + hasNOfAKind(cards, 2, firstpair) / (13.0 * 13.0);
		}
		
		// flush
		for(int color = 0; color < 4; color++) {
			int amount = 0;
			for(int i = 0; i < 13; i++)
				if(cards[i * 4 + color])
					amount++;
			if(amount >= 5)
				return 5; // TODO subvalues
		}
		
		// straight
		for(int highestCard = 12; highestCard >= 3; highestCard--) {
			for(int j = highestCard; j >= highestCard-4; j--) {
				if(j == -1)
					j = 12;
				if(!cards[j * 4] && !cards[j * 4+1] && !cards[j * 4+2] && !cards[j * 4+3])
					break;
				if(j == highestCard-4)
					return 4 + highestCard / 13.0;
			}
		}
		
		// three of a kind
		if(hasNOfAKind(cards, 3, -1) != -1)
			return 3 + hasNOfAKind(cards, 3, -1) / 13.0;
		
		// (double) pair
		firstpair = hasNOfAKind(cards, 2, -1);
		if(firstpair != -1) {
			return (hasNOfAKind(cards, 2, firstpair) != -1)
					? 2 + firstpair / 13.0 + hasNOfAKind(cards, 2, firstpair) / (13.0 * 13.0)
					: 1 + firstpair / 13.0;
		}
		
		// high card
		return hasNOfAKind(cards, 1, -1)/13.0;
	}

	/**
	 * checks if card array includes n or more cards of the same value
	 * @param cards array representing if a card of given ID is available for this function
	 * @param n amount of same valued cards asked for
	 * @param except card value which shall be ignored (necessary to check for fullhouse or double pair)
	 * @return value of the highest found cards which are n or more times there (0-12 or -1 if nothing found)
	 * */
	private int hasNOfAKind(boolean[] cards, int n, int except) {
		for(int i = 12; i >= 0; i--) {
			if(i == except)
				continue;
			int amount = 0;
			for(int j = 0; j < 4; j++)
				if(cards[i * 4 + j])
					amount++;
			if(amount >= n)
				return i;
		}
		return -1;
	}

	/**
	 * prints all important features of current game state into console
	 * */
	private void printGameState() {
		println("\n=============================");
		println("\nCOM: " + cardsToStr(community));
		for(int i = 0; i < PLAYERS; i++) {
			println("\n=== P" + i + " ===");
			println("CHP: " + chips[i] + "$");
			println("INP: " + input[i] + "$");
			println("HND: " + cardsToStr(hands[i]));
			println("SCR: " + Math.round(calcScore(i) * 13.0));
		}
	}

	/**
	 * creates String out of card ID to visualize card
	 * @param c ID of card (position in deck / 0-51)
	 * @return String representing given card
	 * */
	private static String cardIdToString(int c) {
		if(c == -1)
			return "[  ]";
		int cInt = c%4;
		int vInt = c/4+2;
		String cString = "", vString = vInt + "";
		
		if(vInt == 10)
			vString = "T";
		else if(vInt == 11)
			vString = "J";
		else if(vInt == 12)
			vString = "Q";
		else if(vInt == 13)
			vString = "K";
		else if(vInt == 14)
			vString = "A";
		
		if(cInt == 0)
			cString = "+";
		else if(cInt == 1)
			cString = "%";
		else if(cInt == 2)
			cString = "$";
		else
			cString = "&";
		return "[" + cString + vString + "]";
	}

	/**
	 * creates String out of card IDs to visualize cards
	 * @param c array of card IDs (position in deck / 0-51) in wished order
	 * @return String representing given cards
	 * */
	public static String cardsToStr(int[] c) {
		String str = "";
		for(int i = 0; i < c.length; i++) {
			str += cardIdToString(c[i]) + (i < c.length - 1 ? " " : "");
		}
		
		return str;
	}

	/**
	 * prints text into console
	 * @param text message which will be printed
	 * */
	private void println(String text) {
		System.out.println(text);
	}
	
	/**
	 * draws and removes random card from deck
	 * @return id of drawn card
	 * */
	private int drawCard() {
		while(true) {
			int c = (int)(Math.random() * 52);
			if(deck[c]) {
				deck[c] = false;
				return c;
			}
		}
	}
}