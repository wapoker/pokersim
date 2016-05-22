package gui;

public abstract class Player {
	public abstract int getAction(int[] gameState);
	public abstract void addRoundEndState(int[] features);
	public abstract String getName();
}