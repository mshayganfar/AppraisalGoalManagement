import Mechanisms.Appraisal.AppraisalProcesses;
import Mechanisms.Appraisal.Relevance;
import MentalState.Belief;
import MentalState.Goal;
import edu.wpi.cetask.Plan;
import edu.wpi.cetask.TaskClass;
import edu.wpi.cetask.TaskModel;
import edu.wpi.disco.Agent;
import edu.wpi.disco.Disco;
import edu.wpi.disco.Interaction;
import edu.wpi.disco.User;

public class AppraisalGoalManagement {
	
	private static Disco disco;
	private static TaskModel taskModel;
	
	public static TaskClass getGoal(Plan plan) {
		
		while (!plan.getType().getNamespace().toString().equals(taskModel.toString()))
			plan = plan.getParent();
		
		return plan.getGoal().getType();
	}
	
	public static void doAppraisal() {
		
		if (disco.getFocus() != null) {
			System.out.println(getGoal(disco.getFocus()));
		} else {
			System.out.println("No goal in the stack!");
		}
	}
	
	public static void main(String[] args) {
		
		Interaction interaction = new Interaction(new Agent("agent"), new User("user"),
				  args.length > 0 && args[0].length() > 0 ? args[0] : null);

		interaction.getExternal().setEval(true);
		interaction.start(true);

		disco = interaction.getDisco();

		System.out.println(disco);

		taskModel = disco.load("/TaskModel/Sandwich.xml");
		disco.load("/TaskModel/Events.xml");
		
//		interaction.getConsole().source("test/events.txt");
//		interaction.getConsole().test("test/Console.test");
//		interaction.getConsole().step("test/Console.test");
		
//		Goal eventGoal1 = new Goal(null);
//		Belief belief1 = new Belief(eventGoal1);
//		Belief belief2 = new Belief(eventGoal1);
//		Belief belief3 = new Belief(eventGoal1);
//		
//		Goal eventGoal2 = new Goal(null);
//		Belief belief4 = new Belief(eventGoal2);
//		Belief belief5 = new Belief(eventGoal2);
		
//		Relevance relevance = new Relevance(args);
//		relevance.isEventRelevant(eventGoal1);
//		
//		relevance.isEventRelevant(eventGoal2);
		
		
//		Task task = taskModel.getTaskClass("d").newInstance();
//
//		System.out.println(task.toString());
//		System.out.println(taskModel.toString());
//
//		//interaciton.done(false, Propose.Should.newInstance(disco, false, task), null);
//		interaciton.occurred(false, task, null);
//
//		//interaciton.getSystem().respond(interaciton, true, false, false);

	}
}
