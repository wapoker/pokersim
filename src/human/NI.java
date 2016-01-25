package human;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gui.Main;
import gui.Player;

public class NI extends Player {

	private JFrame frame;
	private JPanel panel;
	private JButton fold, call, bet;
	private JSlider betAmount;
	private JLabel status, betAmountLabel;
	
	private boolean pressedFold = false, pressedCall = false, pressedBet = false;
	
	public NI() {
		frame = new JFrame();
		frame.setLocationByPlatform(true);
		frame.setSize(360, 230);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		panel = new JPanel(null);
		frame.add(panel);
		
		status = new JLabel();
		status.setText("Warte auf Gegner ...");
		status.setBounds(10, 30, 340, 90);
		status.setFont(new Font("Consolas", 0, 12));
		panel.add(status);
		
		betAmount = new JSlider();
		betAmount.setBounds(120, 130, 100, 20);
		betAmount.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				betAmountLabel.setText(betAmount.getValue() + "$");
			}
		});
		panel.add(betAmount);
		
		betAmountLabel = new JLabel();
		betAmountLabel.setBounds(270, 130, 60, 20);
		panel.add(betAmountLabel);
		
		fold = new JButton("fold/check");
		fold.setBounds(10, 160, 100, 20);
		fold.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pressedFold = true;
			}
		});
		panel.add(fold);
		
		call = new JButton("call/check");
		call.setBounds(120, 160, 100, 20);
		call.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pressedCall = true;
			}
		});
		panel.add(call);
		
		bet = new JButton("bet");
		bet.setBounds(230, 160, 100, 20);
		bet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pressedBet = true;
			}
		});
		panel.add(bet);
		
		setButtonsEnabled(false);
		
		frame.repaint();
	}
	
	public int getAction(double[] gameState) {
		pressedFold = false;
		pressedBet = false;
		pressedCall = false;
		updateStatus(true, gameState);
		betAmount.setMinimum((int)gameState[7]+(int)gameState[0]);
		betAmount.setValue((int)gameState[7]+(int)gameState[0]);
		betAmount.setMaximum((int)gameState[4]+(int)gameState[5]);
		setButtonsEnabled(true);
		while(!pressedFold && !pressedCall && !pressedBet) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		setButtonsEnabled(false);
		updateStatus(false, gameState);
		if(pressedCall)
			return (int)(gameState[7]);
		if(pressedFold)
			return (int)(gameState[5]);
		return betAmount.getValue();
	}
	
	private void updateStatus(boolean waitingForAction, double[] gameState) {
		int[] hand = {(int)gameState[2], (int)gameState[3]};
		int[] community = {(int)gameState[8], (int)gameState[9], (int)gameState[10], (int)gameState[11], (int)gameState[12]};
		String statusString = "<html>"
				+ (!waitingForAction ? "Warte auf Gegner ...<br/>" : "Wähle eine Aktion!<br/>"
				+ "BB: " + (int)gameState[0] + "$<br/>"
				+ "IN: " + (int)gameState[5] + "$/" + (int)gameState[4] + "$ [" + (int)gameState[7] + "$/" + (int)gameState[6] + "$]<br/>"
				+ "HD: " + Main.cardsToStr(hand) + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + Main.cardsToStr(community).replace(" ", "&nbsp;") + "<br/>")
				+ "</html>";
		status.setText(statusString);
	}
	
	private void setButtonsEnabled(boolean enabled) {
		fold.setEnabled(enabled);
		call.setEnabled(enabled);
		bet.setEnabled(enabled);
	}
}