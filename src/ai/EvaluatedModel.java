package ai;
import java.util.ArrayList;

public class EvaluatedModel {
	
	private CombinedModel combinedModel;
	private double[] evaluations;
	
	public EvaluatedModel(CombinedModel combinedModel, DataPoint curGameState) {
		this.combinedModel = combinedModel;
		evaluate(curGameState);
	}
	
	/**
	 * gives every DataPoint from combinedModel a factor which represents similarity to current game state DataPoint
	 * */
	private void evaluate(DataPoint curGameState) {
		ArrayList<DataPoint> combinedData = combinedModel.getCombinedData();
		evaluations = new double[combinedData.size()];
		for(int i = 0; i < combinedModel.getCombinedData().size(); i++) {
			DataPoint dp = combinedData.get(i);
			evaluations[i] = getSimilarity(dp, curGameState);
		}
	}
	
	/**
	 * calculates similarity of all known features of two DataPoints
	 * @return similarity of two DataPoints (not necessary, but maybe value from 0 to 1?)
	 * TODO implement artificial Neural Network or other algorithm for calculation
	 * */
	private double getSimilarity(DataPoint dp1, DataPoint dp2) {
		double similarity = 1; //Math.random();
		return similarity;
	}
	
	/**
	 * calculates how likely opponent will do any action
	 * @return calculated probabilities of opponent's possible actions
	 * TODO differ raise actions, give 'searchedFeature' constant number
	 * */
	public double[] calculateActionSpeculation() {
		ArrayList<DataPoint> combinedData = combinedModel.getCombinedData();
		
		double[] probs = new double[3]; // <--- fold, call raise 10, raise 20, ...
		
		int searchedFeature = 0; // <--- give 'searchedFeature' constant number
		
		// init probs by sum of similarity
		for(int i = 0; i < combinedData.size(); i++)
			probs[(int)(combinedData.get(i).getFeatureValue(searchedFeature))+1] += evaluations[i]; // assumes that searched feature is (int) instead of double
			
		// make sum of probs to 1
		double sum = 0;
		for(int i = 0; i < probs.length; i++)
			sum += probs[i];
		for(int i = 0; i < probs.length; i++)
			probs[i] = probs[i] / sum;
		
		return probs;
	}
}