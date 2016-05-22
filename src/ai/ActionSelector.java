package ai;
import java.util.ArrayList;

public class ActionSelector {

	public static final double[][] WINNING_PERCENTAGES = {
			{0.852, 0.610, 0.572, 0.537, 0.501, 0.457, 0.427, 0.401, 0.385, 0.389, 0.380, 0.371, 0.363},
			{0.590, 0.726, 0.435, 0.424, 0.413, 0.390, 0.375, 0.370, 0.372, 0.366, 0.359, 0.351, 0.344},
			{0.551, 0.405, 0.690, 0.414, 0.410, 0.391, 0.376, 0.359, 0.364, 0.357, 0.350, 0.343, 0.335},
			{0.512, 0.392, 0.386, 0.657, 0.414, 0.395, 0.380, 0.363, 0.355, 0.351, 0.344, 0.337, 0.329},
			{0.474, 0.380, 0.378, 0.382, 0.624, 0.400, 0.382, 0.369, 0.360, 0.344, 0.339, 0.332, 0.324},
			{0.427, 0.356, 0.358, 0.362, 0.368, 0.587, 0.388, 0.373, 0.364, 0.348, 0.330, 0.326, 0.318},
			{0.395, 0.340, 0.341, 0.346, 0.352, 0.355, 0.558, 0.377, 0.369, 0.353, 0.335, 0.317, 0.312},
			{0.367, 0.336, 0.324, 0.329, 0.335, 0.339, 0.344, 0.527, 0.372, 0.356, 0.338, 0.321, 0.303},
			{0.350, 0.337, 0.328, 0.319, 0.325, 0.330, 0.335, 0.339, 0.503, 0.367, 0.349, 0.332, 0.314},
			{0.354, 0.330, 0.322, 0.316, 0.308, 0.312, 0.318, 0.322, 0.333, 0.481, 0.356, 0.339, 0.321},
			{0.344, 0.323, 0.314, 0.308, 0.303, 0.294, 0.299, 0.303, 0.314, 0.322, 0.455, 0.331, 0.313},
			{0.335, 0.315, 0.306, 0.300, 0.295, 0.289, 0.280, 0.284, 0.296, 0.303, 0.295, 0.430, 0.304},
			{0.326, 0.307, 0.298, 0.292, 0.287, 0.281, 0.275, 0.265, 0.277, 0.284, 0.276, 0.267, 0.405},
	};
	
	private CombinedModel combinedModel;
	private EvaluatedModel evaluatedModel;
	
	public ActionSelector(CombinedModel combinedModel) {
		this.combinedModel = combinedModel;
	}
	
	/**
	 * simulates game for selecting every possible action and searches for best one
	 * @param gameState current state in the game, needed to find all possible actions and to simulate future
	 * @return action which is most successful in simulation
	 * */
	Action selectBestAction(DataPoint dataPoint, double[] handProbabilities) {
 		ArrayList<Action> possibleActions = getPossibleActions(dataPoint);
 		
		double bestScore = simulate(dataPoint, possibleActions.get(0), handProbabilities);
		Action bestAction = possibleActions.get(0);
		
		for(int i = 1; i < possibleActions.size(); i++) {
			Action action = possibleActions.get(i);
			double score = simulate(dataPoint, action, handProbabilities);
			if(score > bestScore) {
				bestScore = score;
				bestAction = possibleActions.get(i);
			}
		}
		
		System.out.println("[NET] ===  CALCULATED BEST ACTION  ===");
		return bestAction;
	}
	
	/**
	 * collects all possible actions to select the best one in selectBestAction()
	 * @return ArrayList of all possible actions which AI could do at current gamestate
	 * */
	ArrayList<Action> getPossibleActions(DataPoint dp) {
 		ArrayList<Action> possibleActions = new ArrayList<Action>();
 		possibleActions.add(new Action(ActionType.fold, -1));
 		if(DataPoint.getMinInput() - dp.paidInput() > 0) 
 	 		possibleActions.add(new Action(ActionType.call, dp.getInput(false)-dp.getInput(true)));
 		for(int i = 1; DataPoint.getMinInput() <= dp.maxRaisePossible() - i * DataPoint.getBB(); i++)
 	 		possibleActions.add(new Action(ActionType.raise, i * DataPoint.getBB()));
 		return possibleActions;
	}
	
	/**
	 * simulates what would happen if given action was done and opponent would act like described by evaluatedModel
	 * @param dp current gameState when action will be done in simulation
	 * @param action which AI will do in simulation
	 * @return score representing how successful action was
	 * */
	double simulate(DataPoint dp, Action action, double[] handProbs) {
		double avgwin = 0;
		
		for(int i = 0; i < handProbs.length; i++) {
			
			int[] community = {dp.getCommCard(0), dp.getCommCard(1), dp.getCommCard(2), dp.getCommCard(3), dp.getCommCard(4)};
			
			int c1 = 12-i/13;
			int c2 = 12-i%13;
			int[] oppHand = {c1*4, c2*4 + (c1 > c2 ? 0 : 1)}; // TODO check if > not <
			
			evaluatedModel = new EvaluatedModel(combinedModel, dp.doAction(action), true, -1);
			double[] actionProbs = evaluatedModel.calculateSpeculation(true, dp.getPhase()-1, community, oppHand);
			
			for(int j = 0; j < 3; j++) {
				ActionType type = j == 0 ? ActionType.fold : (j == 1 ? ActionType.call : ActionType.raise);
				int amount = j == 0 ? -1 : (j == 1 ? dp.getInput(false) : dp.getInput(false) + 100); // TODO allow other values
				avgwin += actionProbs[j] * handProbs[i] * winForActions(dp, action, new Action(type, amount), oppHand);
			}
		}
		return avgwin;
	}
	
	/**
	 * calculates the assumed win of the AI if given actions are done at given datapoint
	 * @param gameState current gameState when action will be done in simulation
	 * @param ownAction which will be done by AI in simulation
	 * @param oppAction which will be done by opponent in simulation
	 * @param handProbs probabilities that opponent holds any specific hand guessed by AI
	 * @return score representing how much AI wins in simulation
	 * */
	double winForActions(DataPoint dataPoint, Action ownAction, Action oppAction, int[] hand) {
		if(oppAction.getType() == ActionType.fold)
			return dataPoint.getInput(false);
		else
			return oppAction.getAmount() * (2*calculateWinProbability(dataPoint, hand)-1);
		
		/* for(int i = 0; i < 13; i++) {
			for(int j = 0; j < 13; j++) {
				int[] hand = {i*4, j*4 + (i > j ? 0 : 1)};
				if(i != j)
					simWin += handProbs[i * 13 + j] * oppAction.getAmount() * (2*calculateWinProbability(dataPoint, hand)-1);
			} }*/
	}

	
	/**
	 * calculates the probability that the AI wins this round.
	 * @param gameState current gameState for which calculation will be done
	 * @param handOpp hand cards of opponent guessed by AI, complements gameState
	 * @return probability (0-1) representing how likely AI wins in simulation
	 * */
	public static double calculateWinProbability(DataPoint dataPoint, int[] handOpp) {
		int[] handPlayer = {dataPoint.getHand(true), dataPoint.getHand(false)};
		int[] community = {dataPoint.getCommCard(0), dataPoint.getCommCard(1), dataPoint.getCommCard(2), dataPoint.getCommCard(3), dataPoint.getCommCard(4)};
		
		// river
		if(dataPoint.getPhase() == 3) {
			double scorePlayer = gui.Main.calcScore(handPlayer, community);
			double scoreOpp = gui.Main.calcScore(handOpp, community);
			return scoreOpp > scorePlayer ? 0 : (scorePlayer > scoreOpp ? 1 : 0.5);
		}
		
		// preflop
		if(dataPoint.getPhase() == 0) {
			double winPlayer = getPreflopScore(handPlayer);
			double winOpp = getPreflopScore(handOpp);
			return winPlayer / (winPlayer+winOpp); // Annäherung, nicht optimal bei stark abweichenden Werten (z. B. 0.2 gegen 0.8)
		}
		
		// flop & turn
		double scoreP = gui.Main.calcScore(handPlayer, community);
		double scoreO = gui.Main.calcScore(handOpp, community);
		double quot = scoreP/(scoreO+scoreP);
		return ((5*quot-2.5)/(1+Math.abs(5*quot-2.5))+1)/2;
	}
	
	private static double getPreflopScore(int[] hand) {

		int v1 = 12-hand[0]/4;
		int v2 = 12-hand[1]/4;
		if(hand[0]%4 == hand[1]%4)
			return WINNING_PERCENTAGES[Math.min(v1, v2)][Math.max(v1, v2)];
		else
			return WINNING_PERCENTAGES[Math.max(v1, v2)][Math.min(v1, v2)];
	}

	public double[] calculateHandProbabilities(DataPoint dp) {
		EvaluatedModel e = new EvaluatedModel(combinedModel, dp, false, -1);
		int[] curCommunity = {dp.getCommCard(0), dp.getCommCard(1), dp.getCommCard(2), dp.getCommCard(3), dp.getCommCard(4)};
		double[] speculation = e.calculateSpeculation(false, dp.getPhase()-1, curCommunity, null);
		return speculation;
	}
}