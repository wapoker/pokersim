package ai;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.imageio.ImageIO;

import gui.Player;

public class AI extends Player {

	private double[][] handProbabilities = new double[3][13*13];
	private double[][] lastProbs = new double[3][13*13];
	public static boolean READY = false;
	private double sum;
	
	private String log = "";
	
	public AI() {
		init();
	}
	
	public void init() {
		// TODO add pregame data
	}
	
	public int getAction(int[] features) {
		DataPoint dp = new DataPoint(features);
		
		if(!READY)
			return dp.getInput(false);
		
		CombinedModel c = new CombinedModel();
		c.substitute();
		ActionSelector a = new ActionSelector(c);
		if(dp.getPhase() > 0) {
			lastProbs[dp.getPhase()-1] = handProbabilities[dp.getPhase()-1];
			handProbabilities[dp.getPhase()-1] = a.calculateHandProbabilities(dp);
			
			if(dp.getPhase() == 2) {
				messureProbsAccuracy(handProbabilities[dp.getPhase()-1]);
				exportHandProbs(handProbabilities[dp.getPhase()-1], dp.getPhase()-1, features);
			}
			return dp.getInput(false); // TODO replace \|/
			//return dp.getInput(false) + a.selectBestAction(dp, calcHandProbsCombo(dp.getPhase()-1)).getAmount();		
		}
		return dp.getInput(false);
	}
	
	public void addRoundEndState(int[] features) {
		DataPoint dp = new DataPoint(features);
		if(dp.getHand(true) != -1)
			CombinedModel.addIngameData(dp);
	}
	
	private double[] calcHandProbsCombo(int phase) {
		return handProbabilities[phase]; // TODO combine handProbs
	}

	public String getName() {
		return "AI";
	}
	
	private void exportHandProbs(double[] probs, int phase, int[] features) {
		
		int handId = sBot.NI.getHandId();
		
		BufferedImage bfR = new BufferedImage(260, 260, BufferedImage.TYPE_3BYTE_BGR);
		BufferedImage bfB = new BufferedImage(260, 260, BufferedImage.TYPE_3BYTE_BGR);
		Graphics gR = bfR.getGraphics();
		Graphics gB = bfB.getGraphics();

		double maxProb = 0;
		for(int i = 0; i < probs.length; i++)
			maxProb = Math.max(maxProb, probs[i]);
			
		for(int i = 0; i < probs.length; i++) {
			int c1 = i/13;
			int c2 = i%13;
			String[] cardValueStrings = {"A", "K", "Q", "J", "T", "9", "8", "7", "6", "5", "4", "3", "2"};
			gR.setColor(new Color((int)(probs[i] / maxProb * 255), 0, 0));
			gR.fillRect(c2*20, c1*20, 20, 20);
			int blue = (int)(((probs[i]-lastProbs[phase][i])*10000)*127+127);
			if(blue < 0 || blue > 255) {blue = 0;}
			gB.setColor(new Color(0, blue > 127 ? blue : 0, blue > 127 ? 0 : blue));
			gB.fillRect(c2*20, c1*20, 20, 20);
			if(i == handId) {
				gR.setColor(new Color(255, 255, 255));
				gR.drawRect(c2*20, c1*20, 20, 20);
				gB.setColor(new Color(255, 255, 255));
				gB.drawRect(c2*20, c1*20, 20, 20);
			}
			gR.setColor(new Color(255, 255, 255, 127));
			gR.drawString(cardValueStrings[c2] + cardValueStrings[c1], c2*20+2, c1*20+16);
			gB.setColor(new Color(255, 255, 255, 127));
			gB.drawString(cardValueStrings[c2] + cardValueStrings[c1], c2*20+2, c1*20+16);
		}

		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter("src/export/history.txt", "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		log += gui.Main.getGameID() + ": " + gui.Main.cardsToStr(new DataPoint(features).getCommCards()) + "\n";
		writer.println(log);
		writer.close();
		
		try {
			ImageIO.write(bfR, "png", new File("src/export/R" + gui.Main.getGameID() + ".png"));
			ImageIO.write(bfB, "png", new File("src/export/B" + gui.Main.getGameID() + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void messureProbsAccuracy(double[] probs) {
		int handId = sBot.NI.getHandId();
		
		double probSumDifColors = 0;
		
		if(handId != -1) {

			for(int x = 0; x < 13; x++)
				for(int y = 0; y < x+1; y++)
					probSumDifColors += probs[x * 13 + y];
			
			sum += probs[handId]/(probSumDifColors) *91.0-1;
			System.out.println(probs[handId]);
			System.out.println(sum); // TODO remove
		}
	}
}
