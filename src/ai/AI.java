package ai;

import gui.Player;

public class AI extends Player {
	
	public int getAction(double[] gameState) {
		CombinedModel c = new CombinedModel();
		c.substitute();
		ActionSelector a = new ActionSelector(c);
		return a.selectBestAction(new GameState(gameState)).getAmount();
	}
}
