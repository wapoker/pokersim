package ai;
public class GameState {
	
	private double[] features;
	
	public GameState doAction(Action action) {
		GameState newGS = new GameState(features);
		newGS.features[5] = action.getAmount(); // TODO ...
		return newGS;
	}
	
	public double[] getFeatureValues() {
		double[] featureValues = {(int)((features[7] - features[5]) / features[0])};
		return featureValues;
	}
	
	public GameState(double[] features) {
		this.features = features;
	}
	
	public int minInput() {
		return (int)features[7];
	}
	
	public int paidInput() {
		return (int)features[5];
	}
	
	public int maxRaisePossible() {
		return Math.min((int)(features[4]+features[5]), (int)(features[6]+features[7]));
	}
	
	public int getBB() {
		return (int)features[0];
	}
}