package ai;
import java.util.ArrayList;

public class ActionSelector {

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
	Action selectBestAction(GameState gameState) {
 		ArrayList<Action> possibleActions = getPossibleActions(gameState);
 		
		double bestScore = simulate(gameState, possibleActions.get(0));
		Action bestAction = possibleActions.get(0);
		
		for(int i = 1; i < possibleActions.size(); i++) {
			Action action = possibleActions.get(i);
			double score = simulate(gameState, action);
			if(score > bestScore) {
				bestScore = score;
				bestAction = possibleActions.get(i);
			}
		}
		
		return bestAction;
	}
	
	/**
	 * collects all possible actions to select the best one in selectBestAction()
	 * @return ArrayList of all possible actions which AI could do at current gamestate
	 * */
	ArrayList<Action> getPossibleActions(GameState gameState) {
 		ArrayList<Action> possibleActions = new ArrayList<Action>();
 		possibleActions.add(new Action(ActionType.fold, gameState.paidInput()));
 		if(gameState.minInput() - gameState.paidInput() > 0)
 	 		possibleActions.add(new Action(ActionType.call, gameState.minInput()));
 		for(int i = 1; gameState.minInput() <= gameState.maxRaisePossible() - i * gameState.getBB(); i++)
 	 		possibleActions.add(new Action(ActionType.raise, gameState.minInput() + i * gameState.getBB()));
 		return possibleActions;
	}
	
	/**
	 * simulates what would happen if given action was done and opponent would act like described by evaluatedModel
	 * @param gameState current gameState when action will be done in simulation
	 * @param action which AI will do in simulation
	 * @return score representing how successful action was
	 * */
	double simulate(GameState gameState, Action action) {
		double avgwin = 0;
		evaluatedModel = new EvaluatedModel(combinedModel, new DataPoint(gameState.doAction(action).getFeatureValues()));
		double[] probs = evaluatedModel.calculateActionSpeculation();
		for(int i = 0; i < probs.length; i++) {
			ActionType type = ActionType.call;
			int amount = 0;
			avgwin += probs[i] * winForActions(gameState, action, new Action(type, amount));
		}
		return avgwin;
	}
	
	/**
	 * calculates the assumed win of the AI if given actions are done at given gamestate
	 * @param gameState current gameState when action will be done in simulation
	 * @param ownAction which will be done by AI in simulation
	 * @param oppAction which will be done by opponent in simulation
	 * @return score representing how much AI won in sumulation
	 * TODO make algorithm
	 * */
	double winForActions(GameState gameState, Action ownAction, Action oppAction) {
		if(oppAction.getType() == ActionType.fold)
			return 0;
		if(oppAction.getType() == ActionType.call)
			return oppAction.getAmount(); // TODO - or +
		return 0;
	}
}