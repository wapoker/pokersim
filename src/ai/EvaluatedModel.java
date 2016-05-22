package ai;
import java.io.File;
import java.util.ArrayList;

import javax.naming.CommunicationException;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;

import nn.Network;

public class EvaluatedModel {
	
	private static Network network;
	
	private CombinedModel combinedModel;
	private double[] evaluations;
	private final static String NETWORK_DIR = "C:/Users/Lukas/Desktop/Networks/";
	private MultiLayerPerceptron[] nnets = {
			(MultiLayerPerceptron) NeuralNetwork.createFromFile(new File(NETWORK_DIR + "H0.nnet")),
			(MultiLayerPerceptron) NeuralNetwork.createFromFile(new File(NETWORK_DIR + "H1.nnet")),
			(MultiLayerPerceptron) NeuralNetwork.createFromFile(new File(NETWORK_DIR + "H2.nnet")),
			(MultiLayerPerceptron) NeuralNetwork.createFromFile(new File(NETWORK_DIR + "A1.nnet")),
			(MultiLayerPerceptron) NeuralNetwork.createFromFile(new File(NETWORK_DIR + "A2.nnet")),
			(MultiLayerPerceptron) NeuralNetwork.createFromFile(new File(NETWORK_DIR + "A3.nnet"))
	};
	
	public EvaluatedModel(CombinedModel combinedModel, DataPoint curGameState, boolean actionNotHand, int handStrength) {
		this.combinedModel = combinedModel;
		evaluate(curGameState, actionNotHand, handStrength);
	}
	
	/**
	 * gives every DataPoint from combinedModel a factor which represents similarity to current game state DataPoint
	 * */
	private void evaluate(DataPoint curGameState, boolean actionNotHand, int handStrength) {
		ArrayList<DataPoint> combinedData = combinedModel.getCombinedData();
		evaluations = new double[combinedData.size()];
		for(int i = 0; i < combinedModel.getCombinedData().size(); i++) {
			DataPoint dp = combinedData.get(i);
			evaluations[i] = getSimilarity(curGameState, dp, actionNotHand, handStrength);
		}
	}
	
	/**
	 * ___ OPTIONAL ___
	 * trains the neural network with newly added DataPoints
	 * private void trainNeuralNetwork() {}
	 * */
	
	/**
	 * calculates similarity of all known features of two DataPoints
	 * @param dpCur current DataPoint of which the similarity with previous DataPoints will be calculated
	 * @param dpComp second DataPoint, similarity between them will be determined
	 * @param actionNotHand expresses if the purpose of comparing is to determine the opponent action and not his hand
	 * @param handstrength of opponent in current gameState (dpCur) guessed by AI, necessary only if actionNotHand = false
	 * @return similarity of two DataPoints (not necessary, but maybe value from 0 to 1?)
	 * */
	private double getSimilarity(DataPoint dpCur, DataPoint dpComp, boolean actionNotHand, int handStrengthCur) {
		
		int[] input = new int[16];
		
		for(int i = 0; i < 4; i++) {
			input[i*4] = dpCur.getAction(true, i);
			input[i*4+1] = dpComp.getAction(true, i);
			input[i*4+2] = dpCur.getAction(false, i);
			input[i*4+3] = dpComp.getAction(false, i);
		}
		MultiLayerPerceptron nnet = nnets[(actionNotHand?2:0) + dpCur.getPhase()];
		
		// --> chose inputs
		
		if(actionNotHand) {
			if(dpCur.getPhase() == 1)
				nnet.setInput(dpCur.getAggr(false, 0), dpComp.getAggr(true, 0), dpCur.getChipInput(0), dpComp.getChipInput(0),
						handStrengthCur, dpComp.getHandStrengthOpp(1), dpCur.getAction(true, 1), dpComp.getAction(false, 1));
			if(dpCur.getPhase() == 2)
				nnet.setInput(dpCur.getAggr(false, 0), dpComp.getAggr(true, 0), dpCur.getChipInput(0), dpComp.getChipInput(0),
						dpCur.getAggr(false, 1), dpComp.getAggr(true, 1), dpCur.getChipInput(1), dpComp.getChipInput(1),
						handStrengthCur, dpComp.getHandStrengthOpp(2), dpCur.getAction(true, 2), dpComp.getAction(false, 2));
			if(dpCur.getPhase() == 3)
				nnet.setInput(dpCur.getAggr(false, 0), dpComp.getAggr(true, 0), dpCur.getChipInput(0), dpComp.getChipInput(0),
						dpCur.getAggr(false, 1), dpComp.getAggr(true, 1), dpCur.getChipInput(1), dpComp.getChipInput(1),
						dpCur.getAggr(false, 2), dpComp.getAggr(true, 2), dpCur.getChipInput(2), dpComp.getChipInput(2),
						handStrengthCur, dpComp.getHandStrengthOpp(3), dpCur.getAction(true, 3), dpComp.getAction(false, 3));
		} else {
			if(dpCur.getPhase() == 0)
				nnet.setInput(dpCur.getAggr(false, 0), dpComp.getAggr(true, 0), dpCur.getChipInput(0), dpComp.getChipInput(0));
			if(dpCur.getPhase() == 1) // TODO different inputs for H1
				nnet.setInput(dpCur.getAggr(false, 0), dpComp.getAggr(true, 0), dpCur.getChipInput(0), dpComp.getChipInput(0),
				dpCur.getAggr(false, 1), dpComp.getAggr(true, 1), dpCur.getChipInput(1), dpComp.getChipInput(1));
			if(dpCur.getPhase() == 2)
				nnet.setInput(dpCur.getAggr(false, 0), dpComp.getAggr(true, 0), dpCur.getChipInput(0), dpComp.getChipInput(0),
				dpCur.getAggr(false, 1), dpComp.getAggr(true, 1), dpCur.getChipInput(1), dpComp.getChipInput(1),
				dpCur.getAggr(false, 2), dpComp.getAggr(true, 2), dpCur.getChipInput(2), dpComp.getChipInput(2));
		}
		
		
		nnet.calculate();
		return nnet.getOutput()[0];
	}
	
	/**
	 * calculates how likely opponent will do any action
	 * @return calculated probabilities of opponent's possible actions
	 * */
	public double[] calculateSpeculation(boolean actionNotHand, int phase, int[] curCommunity, int[] oppHand) {
		ArrayList<DataPoint> combinedData = combinedModel.getCombinedData();
		
		double[] probs = new double[actionNotHand?3:13*13]; // <--- fold, call/check, bet/raise
		for(int i = 0; i < probs.length; i++)
			probs[i] = 0;
		
		// init probs by sum of similarity
		for(int i = 0; i < combinedData.size(); i++) {
			if(actionNotHand)
				probs[(int)(combinedData.get(i).getAction(false, phase))] += evaluations[i];
			else {
				DataPoint dp = combinedData.get(i);
				
				int[] hand = {
					combinedData.get(i).getHand(true),
					combinedData.get(i).getHand(false)
				};
				
				int[] community = {
					phase > 0 ? dp.getCommCard(0) : -1,
					phase > 0 ? dp.getCommCard(1) : -1,
					phase > 0 ? dp.getCommCard(2) : -1,
					phase > 1 ? dp.getCommCard(3) : -1,
					phase > 2 ? dp.getCommCard(4) : -1
				};
				
				double score1 = gui.Main.calcScore(hand, community);
				
				for(int c1 = 0; c1 < 13; c1++)
					for(int c2 = 0; c2 <= c1; c2++)
						for(int color1 = 0; color1 < 4; color1++)
							for(int color2 = 0; color2 < 4; color2++)
								if(c1 != c2 || color1 != color2) {
									int[] testedHand = {c1*4+color1, c2*4+color2};
									double score2 = gui.Main.calcScore(testedHand, community);
									double scoreSim = calcScoreSim(score1, score2, phase);
									double add =  evaluations[i] * scoreSim;
									if(color1 == color2)
										probs[(12-c1)*13 + (12-c2)] += add;
									else
										probs[(12-c2)*13 + (12-c1)] += add;
								}
			}
		}
			
		// make sum of probs to 1
		double sum = 0;
		for(int i = 0; i < probs.length; i++)
			sum += probs[i];
		
		for(int i = 0; i < probs.length; i++) {
			//System.out.println(probs[i] + " > " + probs[i] / sum);
			probs[i] = probs[i] / sum;
		}
		
		return probs;
	}
	
	public double calcScoreSim(double score1, double score2, int phase) {
		double dif = score2 - score1;
		double factor = phase == 0 ? 25 : 4;
		if(factor * dif == 0)
			return 1;
		return Math.pow(Math.sin(factor * dif)/(factor * dif), 5);
	}

	public static Network getNetwork() {
		return network;
	}

	public static void setNetwork(Network network) {
		EvaluatedModel.network = network;
	}
}