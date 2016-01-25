package ai;
import java.util.ArrayList;


public class CombinedModel {
	public static final int FEATURE_AMOUNT = 1;

	private ArrayList<DataPoint> ingameData = new ArrayList<DataPoint>();
	private ArrayList<DataPoint> pregameData = new ArrayList<DataPoint>();
	private ArrayList<DataPoint> combinedData = new ArrayList<DataPoint>();

	/**
	 * extends ingameData by current game state
	 * @param list of all features of current game state (also unknown features marked by special value as such)
	 * */
	public void addIngameData(String[] features) {
		double[] feature_values = new double[FEATURE_AMOUNT];
		feature_values[0] = (Double.parseDouble(features[7])-Double.parseDouble(features[5])) / Double.parseDouble(features[0]);
		if(feature_values[0] < 0)
			feature_values[0] = -1;
		feature_values[0]++;
		ingameData.add(new DataPoint(feature_values));
	}
	
	/**
	 * combines ingameData and best pregameData to combinedData
	 */
	public void substitute() {
		combinedData = new ArrayList<DataPoint>();
		combinedData.addAll(ingameData);
		combinedData.addAll(findBestPregameData());
	}
	
	/**
	 * @return the n-best DataPoints from pregameData, n decreases with increasement of ingameData
	 */
	public ArrayList<DataPoint> findBestPregameData() {
		ArrayList<DataPoint> bestPregameData = new ArrayList<DataPoint>();

		for(int i = 0; i < pregameData.size() - ingameData.size(); i++) {
			bestPregameData.add(findNextBestDataPoint(bestPregameData));
		}
		
		return bestPregameData;
	}
	
	/**
	 * @return DataPoint with highest score which isn't already in bestPregameData (parameter)
	 * @param bestPregameData already selected high-score DataPoints
	 */
	public DataPoint findNextBestDataPoint(ArrayList<DataPoint> bestPregameData) {
		DataPoint bestPregameDataPoint = null;
		for(int j = 0; j < pregameData.size(); j++) {
			DataPoint curPregameDataPoint = pregameData.get(j);
			if((bestPregameDataPoint == null || getPregameDataPointScore(curPregameDataPoint) > getPregameDataPointScore(bestPregameDataPoint)) && !bestPregameData.contains(curPregameDataPoint))
				bestPregameDataPoint = curPregameDataPoint;
		}
		return bestPregameDataPoint;
	}
	
	/**
	 * @return score representing how good pregame_datapoint describes current opponent
	 * @param pregame_datapoint datapoint which score will be determined
	 * TODO find formula
	 * */
	public double getPregameDataPointScore(DataPoint pregame_datapoint) {
		return 0;
	}
	
	// GETTERS AND SETTERS
	
	public ArrayList<DataPoint> getCombinedData() {
		return combinedData;
	}
}