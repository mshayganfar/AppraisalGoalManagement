package MetaInformation;

import java.util.ArrayList;

import MentalState.Goal;
import MentalState.MentalState;
import edu.wpi.cetask.Plan;
import edu.wpi.disco.Disco;

public class GoalTree {

	private int nodeCounter = 1;
	private Node topLevelNode;
	private Disco disco;
	
	private ArrayList<Node> preorderTree = new ArrayList<Node>();
	
	public GoalTree(Disco disco) {
		
		this.disco = disco;
		topLevelNode = new Node(new Goal(disco.getTop(disco.getFocus())), 0);
	}
	
	public ArrayList<Node> createTree() {
		
		preorderTraverse(topLevelNode);
		
		return preorderTree;
	}
	
	private void preorderTraverse(Node node) {
		
		if (node == null) {
			nodeCounter--;
			return;
		}
		
		preorderTree.add(node);
		
		for (int i = 0 ; i < node.getNodeGoalPlan().getChildren().size() ; i++) {
			
			preorderTraverse(createNode(node.getNodeGoalPlan().getChildren().get(i), getDistanceFromTop(node.getNodeGoalPlan())));
		}
	}
	
	private int getDistanceFromTop(Plan goalPlan) {
		
		int count = 1;
		
		while (!goalPlan.equals(disco.getTop(goalPlan))) { 
			goalPlan = goalPlan.getParent();
			count++;
		}
		
		return count;
	}
	
	private Node createNode(Plan goalPlan, int planDepthValue) {
		
		if (goalPlan != null)
			return new Node(new Goal(goalPlan), planDepthValue);
		else
			return null;
	}
	
	public int getNodeNumbers() {
		return this.nodeCounter;
	}
}