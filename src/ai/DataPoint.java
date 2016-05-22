package ai;

public class DataPoint {
	
	/*
	 * 00    ... SB-Pos
	 * 01/02 ... Hand Spieler
	 * 03/05 ... Chips Spieler/Gegner
	 * 04/06 ... Einsatz Spieler/Gegner
	 * 07-11 ... Communitykarten
	 * 12-15 ... Aktionen Spieler
	 * 16-19 ... Aktionen Gegner
	 * 20-21 ... Hand Gegner (f. Ref.-Zustände -> HS-Berechnung)
	 * 22-25 ... Chip-Einsätze gesamt pro Runde
	 * */
	
	private int[] features;

	public DataPoint(int[] features) {
		this.features = features;
	}
	
	public DataPoint doAction(Action action) {
		int[] featuresCopy = new int[features.length];
		System.arraycopy(features, 0, featuresCopy, 0, features.length);
		DataPoint newDP = new DataPoint(featuresCopy);
		
		newDP.features[12+getPhase()] = action.getType().toNumber();
		
		return newDP;
	}
	
	public int[] getFeatureValues() {
		return features;
	}
	
	public int getSBPos() {
		return (int)features[0];
	}
	
	public int getHand(boolean first) {
		return (int)features[first?1:2];
	}
	
	public int getChips(boolean playerNotOpp) {
		return (int)features[playerNotOpp?3:5];
	}
	
	public int getInput(boolean playerNotOpp) {
		return (int)features[playerNotOpp?4:6];
	}
	
	public int getCommCard(int index) {
		return (int)features[7+index];
	}
	
	public int paidInput() {
		return (int)features[4];
	}
	
	public int maxRaisePossible() {
		return Math.min((int)(features[4]+features[5]), (int)(features[6]+features[7]));
	}
	
	public int getAction(boolean playerNotOpp, int phase) {
		return (int)features[12 + phase + (playerNotOpp?0:4)];
	}
	
	public int getPhase() {
		if(features[11] > -1)
			return 3;
		if(features[10] > -1)
			return 2;
		if(features[9] > -1)
			return 1;
		return 0;
	}
	
	public static int getBB() {
		return 20;
	}
	
	public static int getMinInput() {
		return getBB();
	}

	public double getHandStrengthOpp(int phase) {
		int c1 = features[20];
		int c2 = features[21];
		
		int[] community = {phase>0?getCommCard(0):-1,phase>0?getCommCard(1):-1,phase>0?getCommCard(2):-1,phase>1?getCommCard(3):-1,phase>2?getCommCard(4):-1};
		int[] handOpp = {features[20], features[21]};

		if(getPhase() == 3)
			return calcWinProb(community, handOpp);
		if(getPhase() == 0) {
			int v1 = 12-handOpp[0]/4;
			int v2 = 12-handOpp[1]/4;
			if(handOpp[0]%4 == handOpp[1]%4)
				return ActionSelector.WINNING_PERCENTAGES[Math.min(v1, v2)][Math.max(v1, v2)];
			else
				return ActionSelector.WINNING_PERCENTAGES[Math.max(v1, v2)][Math.min(v1, v2)];
		}
		if(getPhase() == 2 || getPhase() == 1)
			return gui.Main.calcScore(handOpp, community); // TODO Odds einkalkulieren (z. B. hohe Flash-Wkt.)
		
		int[] handOpponent = {c1, c2};
		return ActionSelector.calculateWinProbability(this, handOpponent);
	}
	
	private static double calcWinProb(int[] community, int[] hand) {
		double refScore = gui.Main.calcScore(community, hand);
		double better = 0, worse = 0, equal = 0;
		for(int i = 0; i < 52; i++)
			for(int j = 0; j < 52; j++)
				if(i != j && (hand[0] != i || hand[1] != j) && (hand[0] != j || hand[1] != i)) {
					int hand2[] = {i, j};
					double score = gui.Main.calcScore(community, hand2);
					better += score > refScore ? 1 : 0;
					worse += score < refScore ? 1 : 0;
					equal += score == refScore ? 1 : 0;
				}
		return (worse + 0.5 * equal)/(better+equal+worse);
	}

	public int getAggr(boolean playerNotOpp, int phase) {
		int a1 = getAction(playerNotOpp, phase);
		int a2 = getAction(!playerNotOpp, phase);
		return a1 > a2 ? 1 : (a1 == a2 ? 0 : -1);
	}

	public double getChipInput(int phase) {
		return features[22+phase];
	}

	public int[] getCommCards() {
		int[] commCards = {getCommCard(0), getCommCard(1), getCommCard(2), getCommCard(3), getCommCard(4)};
		return commCards;
	}
}