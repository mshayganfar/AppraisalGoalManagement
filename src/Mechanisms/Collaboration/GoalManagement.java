package Mechanisms.Collaboration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Mechanisms.Appraisal.Desirability;
import Mechanisms.Appraisal.Relevance;
import MentalState.Goal;
import MetaInformation.GoalTree;
import MetaInformation.Node;
import edu.wpi.cetask.Plan;

public class GoalManagement {
	
	private Collaboration collaboration;
	private Relevance relevance;
	private Desirability desirability;
	
	public GoalManagement(Collaboration collaboration, Relevance relevance, Desirability desirability) {
		this.collaboration = collaboration;
		this.relevance = relevance;
		this.desirability = desirability;
	}

	public double computeCostValue(Goal eventGoal) {
		
		double costValue = 0.0;
		
		int goalPoximity       = getGoalProximity(eventGoal);
		double goalDifficulty  = getGoalDifficulty(eventGoal);
		double goalSpecificity = getGoalSpecificity(eventGoal);
		
		Map<String, Double> weights = getGoalAttributesWeights(eventGoal);
		
		double base = ((goalPoximity * weights.get("proximity")) + (goalDifficulty * weights.get("difficulty")) + (((double)1/(goalSpecificity + 1)) * weights.get("specificity")));
		
		double exponent = getGammaValue(eventGoal, 3, 2);
		
		costValue = Math.pow(base, exponent);
		
		return costValue;
	}
	
	public int getGoalProximity(Goal eventGoal) {
		
		return Math.max(getGoalsDistance(eventGoal.getPlan(), collaboration.getDisco().getFocus()), 1);
	}
	
	public double getGoalDifficulty(Goal eventGoal) {
		
		double goalDifficulty = 0.0;
		
		int goalHeight           = getGoalHeight(eventGoal);
		double predecessorEffort = getPredecessorEffort(eventGoal);
		double descendentEffort  = getDescendentEffort(eventGoal);
		
		goalDifficulty = (goalHeight + 1) * (predecessorEffort + descendentEffort);
		
		return goalDifficulty;
	}

	public double getGoalSpecificity(Goal eventGoal) {
	
		Integer goalDepth = getGoalDepth(eventGoal);
		
		if (goalDepth == null) System.out.println("Goal Management: Goal was not find in the tree!");
		
		int goalDegree = eventGoal.getPlan().getChildren().size();
		
		return ((double)goalDepth/(goalDegree+1));
	}
	
	private Integer getGoalDepth(Goal eventGoal) {
		
		GoalTree goalTree = new GoalTree(collaboration.getDisco());
		
		ArrayList<Node> treeNodes = goalTree.createTree();
		
		for (Node node : treeNodes) {
			if (node.getNodeGoalPlan().getType().equals(eventGoal.getPlan().getType()))
				return node.getNodeDepthValue();
		}
		
		return null;
	}
	
	private double getGammaValue(Goal eventGoal, double C, double alpha) {
		
		double relevance    = getRelevanceValue(eventGoal);
		double desirability = getDesirabilityValue(eventGoal);
		
		double reverseRelevance    = getReverseRelevanceValue(eventGoal);
		double reverseDesirability = getReverseDesirabilityValue(eventGoal);
		
		return -C*(((relevance + 1) * desirability) + (alpha * (reverseRelevance + 1) * reverseDesirability));
	}
	
	private double getRelevanceValue(Goal eventGoal) {
		
		switch(relevance.isEventRelevant(eventGoal)) {
			case RELEVANT:
				return 1.0;
			case IRRELEVANT:
				return 0.0;
			default:
				return 0.0;
		}
	}
	
	private double getDesirabilityValue(Goal eventGoal) {
		
		switch(desirability.isEventDesirable(eventGoal)) {
			case HIGH_DESIRABLE:
				return 1.0;
			case DESIRABLE:
				return 0.5;
			case NEUTRAL:
				return 0.0;
			case UNDESIRABLE:
				return -0.5;
			case HIGH_UNDESIRABLE:
				return -1.0;
			default:
				return 0.0;
		}
	}
	
	private double getReverseRelevanceValue(Goal eventGoal) {
		return 1.0; // This should get its value using reverse appraisal in ToM.
	}
	
	private double getReverseDesirabilityValue(Goal eventGoal) {
		return 0.0; // This should get its value using reverse appraisal in ToM.
	}
	
	private Map<String, Double> getGoalAttributesWeights(Goal eventGoal) {
		
		Map<String, Double> weights = new HashMap<String, Double>();
		
		weights.put("proximity", 1.0);
		weights.put("difficulty", 1.0);
		weights.put("specificity", 1.0);
		
		return weights;
	}
	
	private int getGoalsDistance (Plan firstGoal, Plan secondGoal) {
		
		if (firstGoal.getType().equals(secondGoal.getType()))
			return 1;
		
		int firstGoalDistance  = getDistanceFromTop(firstGoal);
		int secondGoalDistance = getDistanceFromTop(secondGoal);
		
		Node lcaNode = getLowestCommonAncestor(firstGoal, secondGoal);
		
		int lcaGoalDistance  = getDistanceFromTop(lcaNode.getNodeGoalPlan());
		
		int distance = firstGoalDistance + secondGoalDistance - 2*lcaGoalDistance;
		
		return distance;
	}
	
	private int getDistanceFromTop(Plan goalPlan) {
		
		int count = 0;
		
		while (!goalPlan.equals(collaboration.getDisco().getTop(goalPlan))) { 
			goalPlan = goalPlan.getParent();
			count++;
		}
		
		return count;
	}
	
	private Node getLowestCommonAncestor(Plan firstGoalPlan, Plan secondGoalPlan) {
		
		Node lcaGoalNode = null;
		
		GoalTree goalTree = new GoalTree(collaboration.getDisco());
		
		ArrayList<Node> treeNodes = goalTree.createTree();
		
		ArrayList<Node> leveledUpNodes = levelUpNodes(treeNodes, firstGoalPlan, secondGoalPlan);
		
		if (leveledUpNodes != null) {
			lcaGoalNode = goUpToCommonAncestor(treeNodes, leveledUpNodes);
		}
		
		return lcaGoalNode;
	}
	
	private Node goUpToCommonAncestor(ArrayList<Node> treeNodes, ArrayList<Node> leveledUpNodes) {
		
		Node firstNodeAncestor, secondNodeAncestor;
		
		firstNodeAncestor  = leveledUpNodes.get(0);
		secondNodeAncestor = leveledUpNodes.get(1);
		
		while (!firstNodeAncestor.getNodeTaskClass().equals(secondNodeAncestor.getNodeTaskClass())) {
			firstNodeAncestor  = getParentNode(treeNodes, leveledUpNodes.get(0));
			secondNodeAncestor = getParentNode(treeNodes, leveledUpNodes.get(1));
			if ((firstNodeAncestor == null) || (secondNodeAncestor == null)) {
				return null;
			}
		}
		
		return firstNodeAncestor;
	}
	
	private Node getParentNode(ArrayList<Node> treeNodes, Node targetNode) {
		
		for(int i = treeNodes.size()-1 ; i >= 0 ; i--) {
			if (treeNodes.get(i).equals(targetNode)) {
				for(int j = i-1 ; j >= 0 ; j--) {
					if (treeNodes.get(j).getNodeDepthValue() == targetNode.getNodeDepthValue()-1) {
						return treeNodes.get(j);
					}
				}
			}
		}
		
		return null;
	}
	
	private ArrayList<Node> levelUpNodes(ArrayList<Node> treeNodes, Plan firstGoalPlan, Plan secondGoalPlan) {
		
		Node firstNode = null, secondNode = null;
		
		ArrayList<Node> twoLeveledNodes = new ArrayList<Node>();
		
		for (Node node : treeNodes) {
			if(node.getNodeGoalPlan().getType().equals(firstGoalPlan.getType()))
				firstNode = node;
			if(node.getNodeGoalPlan().getType().equals(secondGoalPlan.getType()))
				secondNode = node;
		}
		
		while (firstNode.getNodeDepthValue() > secondNode.getNodeDepthValue())
			firstNode = getParentNode(treeNodes, firstNode);
		
		while (firstNode.getNodeDepthValue() < secondNode.getNodeDepthValue())
			secondNode = getParentNode(treeNodes, secondNode);
		
		twoLeveledNodes.add(firstNode);
		twoLeveledNodes.add(secondNode);
		
		if (twoLeveledNodes.size() == 2)
			return twoLeveledNodes;
		else
			return null;
	}
	
	private int getGoalHeight(Goal eventGoal) {
		
		int maxDepth = -1, goalDepth = 0;
		
		GoalTree goalTree = new GoalTree(collaboration.getDisco());
		
		ArrayList<Node> treeNodes = goalTree.createTree();
		
		for (Node node : treeNodes) {
			if (node.getNodeDepthValue() > maxDepth)
				maxDepth = node.getNodeDepthValue();
			if (node.getNodeGoalPlan().getType().equals(eventGoal.getPlan().getType()))
				goalDepth = node.getNodeDepthValue();
		}
		
		return (maxDepth - goalDepth);
	}
	
	private double getPredecessorEffort(Goal eventGoal) {
		
		double effortSum = 0.0;
		
		ArrayList<Goal> predecessors = findPredecessors(new GoalTree(collaboration.getDisco()).createTree(), eventGoal.getPlan().getPredecessors());
		
		for (Goal node : predecessors) {
			effortSum += node.getGoalEffort();
		}
		
		return effortSum;
	}
	
	private ArrayList<Goal> findPredecessors(ArrayList<Node> treeNodes, List<Plan> planPredecessors) {
		
		ArrayList<Goal> predecessors = new ArrayList<Goal>(); 
		
		for(Plan plan : planPredecessors) {
			for (Node node : treeNodes) {
				if (node.getNodeGoalPlan().getType().equals(plan.getType()))
					predecessors.add(node.getNodeGoal());
			}
		}
		
		return predecessors;
	}
	
	private double getDescendentEffort(Goal eventGoal) {
		
		double effortSum = 0.0;
		
		GoalTree goalTree = new GoalTree(collaboration.getDisco());
		
		ArrayList<Node> treeNodes = goalTree.createTree();
		
		ArrayList<Goal> descendants = findDescendants(treeNodes, eventGoal);
		
		for (Goal node : descendants) {
			effortSum += node.getGoalEffort();
		}
		
		return effortSum;
	}
	
	private ArrayList<Goal> findDescendants(ArrayList<Node> treeNodes, Goal eventGoal) {
		
		int index = 0;
		
		ArrayList<Goal> descendants = new ArrayList<Goal>();
		
		for (int i = 0 ; i < treeNodes.size() ; i++) {
			if (treeNodes.get(i).getNodeGoalPlan().getType().equals(eventGoal.getPlan().getType())) {
				index = i+1;
				while ((treeNodes.get(index).getNodeDepthValue() > treeNodes.get(i).getNodeDepthValue()) && 
						index < treeNodes.size()) {
					descendants.add(treeNodes.get(index).getNodeGoal());
					index++;
				}
				return descendants;
			}
		}
		
		return descendants;
	}
}
