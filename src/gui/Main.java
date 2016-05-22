package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main {
	
	private static final int PLAYERS = 2, START_CHIPS = 1000, SB = 10, BB = 20;
	private static final boolean LOG = false, PAINT = true;
	
	private int activePlayers = PLAYERS;
	private int sbPos = 0, curInput = 0;
	private Player[] players = new Player[PLAYERS];
	private boolean[] deck = new boolean[52];
	private int[][] hands = new int[PLAYERS][2];
	private int[] chips = new int[PLAYERS];
	private int[] community = new int[5];
	private int[] input = new int[PLAYERS];
	private int[][] actions = new int[2][4];
	private int[] pots = new int[4];
	
	private JFrame frame;
	private JPanel panel;
	
	private static final String SPRITE_PATH = "src/sprites/";
	private Image[] sprites;
	
	int[] chipValues = {1, 5, 25, 50, 100};
	
	private int roundNr = 0;
	private static String gameID = "";
	
	public static void main(String[] args) {
		new Main();
	}
	
	public Main() {
		if(PAINT) {
			initFrame();
			initSprites();
		}
		initGame();
		while(chips[0] > 0 && chips[1] > 0) runRound();
		System.out.println("\n[INF] ===  GAME ENDS: " + (chips[0] == 0 ? players[0].getName() : players[1].getName()) + " HAS NO MONEY  ===");
	}
	
	private void initFrame() {
		frame = new JFrame("Poker Simulation GUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		
		panel = new JPanel(null);
		frame.add(panel);
	}

	/**
	 * initializes the game by giving all the chips to the players
	 * */
	private void initGame() {
		players[0] = new ai.AI();
		players[1] = new sBot.NI();
		for(int i = 0; i < PLAYERS; i++) {
			chips[i] = START_CHIPS;
		}
	}

	/**
	 * initializes a round by creating a new deck and dealing out the hand cards
	 * */
	private void initRound() {
		// TODO REMOVE
		{
			chips[0] = START_CHIPS;
			chips[1] = START_CHIPS;
			sbPos = 1;
		}
		
		
		log("\n[INF] ===  NEW ROUND STARTS  ===");
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
		//printGameState();
		for(int i = 0; i < 4; i++) {
			simulateBettingRound(i);
			if(activePlayers == 1) { // ends round
				for(int j = (i==0?3:3+i); j < 5; j++)
					community[j] = drawCard();
				i = 3;
			}
			if(i == 0) { // flop
				log("[INF] ===  FLOP  ===");
				community[0] = drawCard();
				community[1] = drawCard();
				community[2] = drawCard();
			}
			if(i == 1 || i == 2) { // turn / river
				if(activePlayers >= 2)
					log("[INF] ===  " + (i==1 ? "TURN" : "RIVER") + "  ===");
				community[2 + i] = drawCard();
			}
			if(i == 3) { // ends round
				int[] features0 = {
						sbPos, hands[0][0], hands[0][1],
						chips[0], input[0], chips[1], input[1],
						community[0], community[1], community[2], community[3], community[4],
						actions[0][0], actions[0][1], actions[0][2], actions[0][3],
						actions[1][0], actions[1][1], actions[1][2], actions[1][3],
						hands[1][0], hands[1][1],
						pots[0], pots[1], pots[2], pots[3]
				}; // TODO why not getDataPointArray()?
				int[] features1 = {
						sbPos, hands[1][0], hands[1][1],
						chips[1], input[1], chips[0], input[0],
						community[0], community[1], community[2], community[3], community[4],
						actions[1][0], actions[1][1], actions[1][2], actions[1][3],
						actions[0][0], actions[0][1], actions[0][2], actions[0][3],
						hands[0][0], hands[0][1],
						pots[0], pots[1], pots[2], pots[3]
				}; // TODO add all features
				players[0].addRoundEndState(features0);
				players[1].addRoundEndState(features1);
				determineWinner();
				sbPos = (sbPos+1)%PLAYERS;
			}
			
			//printGameState();
		}
	}
	
	/**
	 * simulates a full betting round
	 * @param phase required to log data in pots[phase] and actions[player][phase]
	 * */
	private void simulateBettingRound(int phase) {
		
		gameID = roundNr/4 + "_" + phase ;
		
		int activePlayer = sbPos;
		int actionsSelected = 0;

		// TODO remove
		if(roundNr++ == 997)
			ai.AI.READY = true;
		
		do {
			paintGameState(getDataPointArray(0));
			int preferredInput = 0;
			// select action
			if(curInput == 0)
				preferredInput = SB;
			else if(curInput == SB)
				preferredInput = BB;
			else {
				preferredInput = players[activePlayer].getAction(getDataPointArray(activePlayer));
				actionsSelected++;
			
				// check validity of action
				if(preferredInput > curInput && !(preferredInput >= curInput + BB)) {
					System.err.println("Fehler mit Spieler [" + activePlayer + "]");
					System.err.println("Die Mindesterhöhung beträgt 1BB! [" + (preferredInput-curInput) + " < " + BB + "]");
					System.exit(0);
				}
				if(preferredInput < curInput-input[activePlayer] && preferredInput != input[activePlayer]) {
					System.err.println("Fehler mit Spieler [" + activePlayer + "]");
					System.err.println("Mindesteinsatz bezahlen oder folden! [" + preferredInput + "<" + curInput + "]");
					System.exit(0);
				}
				if(preferredInput-input[activePlayer] > chips[activePlayer]) {
					System.err.println("Fehler mit Spieler [" + activePlayer + "]");
					System.err.println("Die Maximalerhöhung ist ein All-In! [" + preferredInput + " > " + chips[activePlayer] + "]");
					System.exit(0);
				}
			}
			
			logAction(players[activePlayer].getName(), preferredInput, input[activePlayer], input[(activePlayer+1)%2]);
			
			chips[activePlayer] -= preferredInput - input[activePlayer];
			
			// interpret action
			if(preferredInput >= curInput) { // call/bet/raise
				input[activePlayer] = preferredInput;
				actions[activePlayer][phase] = activePlayer == curInput ? -1 : ((phase == 0) || curInput > pots[phase-1] ? 1 : 0);
				curInput = input[activePlayer];
			} else { // fold
				actions[activePlayer][phase] = 0;
				hands[activePlayer][0] = -1;
				hands[activePlayer][1] = -1;
				activePlayers--;
			}

			// end bet round
			if((input[activePlayer] == curInput && (actionsSelected >= activePlayers)) || activePlayers == 1) {
				pots[phase] = input[0] + input[1];
				return;
			}
			
			// select next player
			do {
				activePlayer = (activePlayer+1)%PLAYERS;
			} while(hands[activePlayer][0] == -1);
		} while(true);
	}
	
	
	private void logAction(String playerName, int preferredInput, int inputP, int inputO) {
		String actionMsg = null;
		if(preferredInput < inputO)
			actionMsg = "FOLD";
		else if(preferredInput == inputP)
			actionMsg = "CHECK";
		else if(preferredInput > inputO) {
			if(preferredInput == SB) actionMsg = "SB  (" + SB + ")";
			else if(preferredInput == BB) actionMsg = "BB  (" + BB + ")";
			else actionMsg = "BET (" + preferredInput + ")";
		} else
			actionMsg = "CALL";
		
		log("[ACT]["+playerName+"]: " + actionMsg);
	}
	
	/**
	 * brings all visible information of the player, who is about to make a input decision, into a String
	 * @return String representing known features of the current gameState of the player
	 * */
	private int[] getDataPointArray(int player) {
		
		// list -> see ai.DataPoint

		/*
		 * 00    ... SB-Pos
		 * 01/02 ... Hand Spieler
		 * 03/05 ... Chips Spieler/Gegner
		 * 04/06 ... Einsatz Spieler/Gegner
		 * 07-11 ... Communitykarten
		 * 12-15 ... Aktionen Spieler
		 * 16-19 ... Aktionen Gegner
		 * 20-21 ... Hand Gegner (f. Ref.-Zustände -> HS-Berechnung)
		 * 22-24 ... Pots
		 * */
		
		int[] features = new int[25];
		features[0] = sbPos;
		features[1] = hands[player][0];
		features[2] = hands[player][1];
		features[3] = chips[player];
		features[4] = input[player];
		features[5] = chips[(player+1)%2];
		features[6] = input[(player+1)%2];
		for(int i = 0; i < 5; i++)
			features[7+i] = community[i];
		for(int i = 0; i < 4; i++)
			features[12+i] = actions[player][i];
		for(int i = 0; i < 4; i++)
			features[16+i] = actions[(player+1)%2][i];
		features[20] = hands[(player+1)%2][0];
		features[21] = hands[(player+1)%2][1];
		for(int i = 0; i < 3; i++)
			features[22+i] = pots[i];
		return features;
	}
	
	/**
	 * determines which players win and pays pot out to them
	 * */
	private void determineWinner() {

		double score[] = new double[PLAYERS];
			
		for(int i = 0; i < PLAYERS; i++) {
			score[i] = calcScore(hands[i], community);
		}
		
		if(score[0] == score[1]) {
			chips[0] += input[0];
			chips[1] += input[1];
			input[0] = 0;
			input[1] = 0;
			return;
		}
		
		if(hands[0][0] == -1) {
			chips[1] += input[0] + input[1]; 
				input[0] = 0;
				input[1] = 0;
				return;
		}
		
		if(hands[1][0] == -1) {
			chips[0] += input[0] + input[1]; 
				input[0] = 0;
				input[1] = 0;
				return;
		}

		int winner = score[0] > score[1] ? 0 : 1;
		int loser = (winner+1)%2;

		int winWinner = input[loser] > input[winner] ? input[winner] * 2 : input[0] + input[1];
		int winLoser = input[loser] > input[winner] ? input[loser]-input[winner] : 0;
		
		input[0] = 0;
		input[1] = 0;
		
		chips[winner] += winWinner;
		chips[loser] += winLoser;
	}
	
	/**
	 * calculates a score representing a players hand strength at the current game state
	 * @param player id of the player who's hand will be used for calculation
	 * @return score (0-9) representing how well a player's hand + community cards match (the higher the better)
	 * */
	public static double calcScore(int[] hand, int[] community) {
		boolean[] cards = new boolean[52];
		
		// bereits gefoldet -> score = 0
		if(hand[0] == -1)
			return 0;
		
		for(int j = 0; j < 2; j++)
			cards[hand[j]] = true; 
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
			double subvalue = 0;
			int amount = 0;
			for(int i = 12; i >= 0; i--)
				if(cards[i * 4 + color]) {
					subvalue += i / Math.pow(13.0, ++amount);
				}
			if(amount >= 5)
				return 5 + subvalue;
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
	private static int hasNOfAKind(boolean[] cards, int n, int except) {
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
		log("\n=============================");
		log("\nCOM: " + cardsToStr(community));
		for(int i = 0; i < PLAYERS; i++) {
			log("\n=== P" + i + " ===");
			log("CHP: " + chips[i] + "$");
			log("INP: " + input[i] + "$");
			log("HND: " + cardsToStr(hands[i]));
			log("SCR: " + Math.round(calcScore(hands[i], community) * 13.0));
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
	private void log(String text) {
		if(LOG)
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
	
	private void paintGameState(int[] features) {
		ai.DataPoint dp = new ai.DataPoint(features);
		BufferedImage bf = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = bf.getGraphics();
		g.setColor(new Color(0, 127, 63));
		g.fillRect(0, 0, 800, 600);
		
		g.drawImage(paintHand(dp.getHand(true), dp.getHand(false)), 200, 30, null);
		g.drawImage(paintChips(chips[0]), 150, 100, null);
		g.drawImage(paintChips(input[0]-(dp.getPhase()==0?0:pots[dp.getPhase()-1]/2)), 450, 100, null);
		
		g.drawImage(paintHand(dp.getFeatureValues()[20], dp.getFeatureValues()[21]), 200, 430, null);
		g.drawImage(paintChips(chips[1]), 150, 330, null);
		g.drawImage(paintChips(input[1]-(dp.getPhase()==0?0:pots[dp.getPhase()-1]/2)), 450, 330, null);
		
		g.drawImage(paintComm(dp.getCommCards()), 50, 240, null);
		g.drawImage(paintChips(pots[dp.getPhase()]), 550, 220, null);
		
		panel.getGraphics().drawImage(bf, 0, 0, null);
	}
	
	private void initSprites() {
		sprites = new Image[100];
		for(int v = 2; v < 15; v++)
			for(int c = 0; c < 4; c++)
				sprites[v*4-8+c] = readImage("Cards/" + c + "_" + v);
		sprites[80] = readImage("Chips/chipWhiteBlue_side");
		sprites[81] = readImage("Chips/chipRedWhite_side");
		sprites[82] = readImage("Chips/chipGreenWhite_side");
		sprites[83] = readImage("Chips/chipBlueWhite_side");
		sprites[84] = readImage("Chips/chipBlackWhite_side");
		sprites[85] = readImage("Cards/cardBack_red5");
	}
	
	private Image readImage(String subPath) {
		try { return ImageIO.read(new File(SPRITE_PATH + subPath + ".png")); }
		catch (IOException e) { e.printStackTrace(); }
		return null;
	}
	
	private Image paintHand(int c1, int c2) {
		Image img = new BufferedImage(160, 140, BufferedImage.TRANSLUCENT);
		Graphics g = img.getGraphics();
		g.drawImage(sprites[c1], 0, 0, 70, 95, null);
		g.drawImage(sprites[c2], 26, 10, 70, 95, null);
		return img;
	}
	
	private Image paintComm(int c[]) {
		Image img = new BufferedImage(400, 100, BufferedImage.TRANSLUCENT);
		Graphics g = img.getGraphics();
		for(int i = 0; i < 5; i++)
			g.drawImage(c[i] == -1 ? sprites[85] : sprites[c[i]], 75 * i, 0, 70, 95, null);
		return img;
	}
	
	private Image paintChips(int amount) {
		Image img = new BufferedImage(250, 100, BufferedImage.TRANSLUCENT);
		Graphics g = img.getGraphics();
		for(int i = 4; i >= 0; i--) {
			
			for(int j = 0; j < (amount%(i==4?99999:chipValues[i+1]))/chipValues[i]; j++)
				g.drawImage(sprites[80 + i], i * 32, 82-j*4, 30, 18, null);
		}
		g.setColor(Color.black);
		g.drawString("$" + amount + "", 1, 60);
		g.setColor(Color.white);
		g.drawString("$" + amount + "", 0, 60);
		return img;
	}

	public static String getGameID() {
		return gameID;
	}
}