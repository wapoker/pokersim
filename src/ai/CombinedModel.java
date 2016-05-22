package ai;
import java.util.ArrayList;


public class CombinedModel {
	public static final int FEATURE_AMOUNT = 13;

	private static ArrayList<DataPoint> ingameData = new ArrayList<DataPoint>();
	private static ArrayList<DataPoint> pregameData = new ArrayList<DataPoint>();
	private ArrayList<DataPoint> combinedData = new ArrayList<DataPoint>();

	/**
	 * extends ingameData by one element
	 * @param DataPoint containing all information known at round end
	 * */
	public static void addIngameData(DataPoint dp) {
		ingameData.add(dp);
	}
	
	public static void addPregameData(DataPoint dp) {
		pregameData.add(dp);
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
	public double getPregameDataPointScore(DataPoint dp) {
		return Math.random();
	}
	
	// GETTERS AND SETTERS
	
	public ArrayList<DataPoint> getCombinedData() {
		return combinedData;
	}
}