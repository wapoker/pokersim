package ai;

public class Action {
	private ActionType type; // is it raise, call or fold?
	private int amount; // how much chips are raised/called
	
	public Action(ActionType type, int amount) {
		this.type = type;
		this.amount = amount;
	}
	
	public ActionType getType() {
		return type;
	}
	
	public int getAmount() {
		return amount;
	}
}

enum ActionType {
	raise, call, fold;
	
	public int toNumber() {
		if(this == raise)
			return 1;
		if(this == call)
			return 0;
		return -1;
	}
}