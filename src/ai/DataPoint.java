package ai;

public class DataPoint {
	private double[] featureValues = new double[CombinedModel.FEATURE_AMOUNT];
	
	public DataPoint(double[] featureValues) {
		this.featureValues = featureValues;
	}
	
	public double getFeatureValue(int i) {
		return featureValues[i];
	}
}