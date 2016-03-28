#include "ros/ros.h"
#include <boost/thread/thread.hpp>
#include "control_msgs/FollowJointTrajectoryActionGoal.h" // for sending trajectories to youka
#include "control_msgs/FollowJointTrajectoryActionResult.h" // for waiting trajectory results
#include "control_msgs/JointTolerance.h" // for sending trajectories to youka
#include "trajectory_msgs/JointTrajectory.h" // for sending trajectories to youka
#include "trajectory_msgs/JointTrajectoryPoint.h" // for sending trajectories to youka
#include <actionlib/goal_id_generator.h> // for sending trajectories to youka
#include "brics_actuator/JointPositions.h"
#include <iostream>
#include "sensor_msgs/JointState.h"
#include "std_srvs/Empty.h"
#include <vector>
#include <boost/units/systems/si/length.hpp>
#include <boost/units/systems/si/plane_angle.hpp>
#include <boost/units/io.hpp>
#include <boost/units/systems/angle/degrees.hpp>
#include <boost/units/conversion.hpp>
bool armMoving;
ros::Publisher armTrajectoryPublisher;
ros::Publisher gripperPositionPublisher;
std::vector<double> q;
std::vector<double> qArmHome;
std::vector<double> qArmRight;
std::vector <brics_actuator::JointValue> gripperJointPositions;


void UpdateRobotPose(const sensor_msgs::JointState::ConstPtr& msg) {

 // make sure we're not reading the base wheel joint values
  if( strcmp(msg->name[0].c_str(), "arm_joint_1") == 0 ) {
    // Keep all the joint values
    for(int i=0; i<5; i++)
      q[i] = msg->position[i];
   }
}

void ArmResultListener(const control_msgs::FollowJointTrajectoryActionResult::ConstPtr& msg){
  if( msg->status.status == 3 ){
    std::cout << "Arm stopped moving!" << std::endl;
    armMoving = false;
  }
}

void WaitForArmResult() {
  std::cout << "Arm is moving..." << std::endl;
  while(armMoving) {
    boost::this_thread::sleep(boost::posix_time::milliseconds(1));
  }
  
}

void MoveArmAndWait(const std::vector< std::vector<double> >& traj, double dt) {
    
  // traj's inner vector should have the length of arm-dof
  // traj's outer vector should have the length of goal points in the trajectory

  // Goal ID Generator
  actionlib::GoalIDGenerator idGenerator;
    
  // create joint trajectory message
  trajectory_msgs::JointTrajectory jt;
  // Note: Find a way to fill these in automatically instead of hard-coding.
  jt.header.frame_id = "base_link";
  jt.joint_names.push_back("arm_joint_1");
  jt.joint_names.push_back("arm_joint_2");
  jt.joint_names.push_back("arm_joint_3");
  jt.joint_names.push_back("arm_joint_4");
  jt.joint_names.push_back("arm_joint_5");
    
  for (unsigned i=0; i < traj.size(); i++) {
      
    trajectory_msgs::JointTrajectoryPoint tp;
      
    std::cout << "Configuration for trajectory point " << i << std::endl;
      
    double dq; // delta-q, difference between configuration angles
    // velocity of this joint for this trajectory point
    double v; 
      
    // Loop for however many joints the arm has
    for(unsigned j=0; j < traj[i].size(); j++) {	

      tp.positions.push_back(traj[i][j]);

      // If this is the first or the last trajectory point
      // then all joint velocities are zero.
      if( i == 0 || i == traj.size()-1)
	v = 0.0;
      else {
	  
	// difference between two configurations (i.e. angles) of this particular joint
	// traj[i+1][j] is where this joint wants to go next
	// traj[i][j] is where this joint is currently
	dq = traj[i+1][j] - traj[i][j];
	  
	// dt is the time we want the robot to take between configurations
	v =  dq/dt;
      }
	
      tp.velocities.push_back(v);
      tp.accelerations.push_back(0.0);	
    }
    ros::Duration t(dt*i);
    tp.time_from_start = t;
    jt.points.push_back(tp);
  }

  control_msgs::FollowJointTrajectoryGoal jtg;
  jtg.trajectory = jt;
  ros::Duration gtt(5.0);
  jtg.goal_time_tolerance = gtt;
  //control_msgs::JointTolerance jtol; // joint tolerance
  //control_msgs::JointTolerance gtol; // goal tolerance
    
  control_msgs::FollowJointTrajectoryActionGoal jtag;
  jtag.goal_id = idGenerator.generateID();
  // fill the header
  // jtg.header.seq = seq;
  // jtg.header.stamp.secs = 0; // secs;
  // jtg.header.stamp.nsecs = 0; // nsecs;
  jtag.header.frame_id = "base_link";  
  jtag.goal = jtg;
    
  armMoving = true;
  std::cout << "Moving the arm..." << std::endl;
  armTrajectoryPublisher.publish(jtag);
  std::cout << "Waiting for the arm actionlib result..." << std::endl;
  WaitForArmResult();
}

void MoveArmHome() {
  qArmHome[0] = 0.103;
  qArmHome[1] = 0.078;
  qArmHome[2] = -0.053;
  qArmHome[3] = 0.052;
  qArmHome[4] = 0.178;
  // Notify user
  std::cout << "Starting to move the arm. " << std::endl;
  std::vector< std::vector<double> > here2home;
  std::vector<double> here;
  here.assign(q.begin(),q.begin()+5);
  std::vector<double> home = qArmHome;
  here2home.push_back(here);
  here2home.push_back(home);
  MoveArmAndWait(here2home,5.0);
  // Notify the user
  std::cout << "Finished moving the arm." << std::endl;
}

void MoveArmRight() {
  qArmRight[0] = 0.0;
  qArmRight[1] = 0.11;
  qArmRight[2] = -0.63;
  qArmRight[3] = 0.154;
  qArmRight[4] = 1.991;
  // Notify user
  std::cout << "Starting to move the arm. " << std::endl;
  std::vector< std::vector<double> > here2right;
  std::vector<double> here;
  here.assign(q.begin(),q.begin()+5);
  std::vector<double> home = qArmRight;
  here2right.push_back(here);
  here2right.push_back(home);
  MoveArmAndWait(here2right,5.0);
  // Notify the user
  std::cout << "Finished moving the arm." << std::endl;
}

bool GoHome(std_srvs::Empty::Request  &req,
            std_srvs::Empty::Response &res)
{
  MoveArmHome();
  return true;
}

bool GoRight(std_srvs::Empty::Request  &req,
             std_srvs::Empty::Response &res)
{
  MoveArmRight();
  return true;
}

void WaitForGripper(double target) {
  std::cout << "Waiting for gripper..." << std::endl;
  while( q[5] >= target+0.00001 || q[5] <= target-0.00001) {
    boost::this_thread::sleep(boost::posix_time::milliseconds(1));
  }
  std::cout << "Done." << std::endl;
}

void PublishGripperJointCommand(double& qGripper) {
  brics_actuator::JointPositions command;
    
  gripperJointPositions[0].value = qGripper;
  gripperJointPositions[1].value = qGripper;
    
  command.positions = gripperJointPositions;
  gripperPositionPublisher.publish(command);
}

bool OpenGripper(std_srvs::Empty::Request  &req,
             std_srvs::Empty::Response &res)
{
  double val = 0.0115;
  std::cout << "Opening gripper..." << std::endl;
  PublishGripperJointCommand(val);
  WaitForGripper(val);
  return true;
}

bool CloseGripper(std_srvs::Empty::Request  &req,
             std_srvs::Empty::Response &res) {
  double val = 0.0;
  PublishGripperJointCommand(val);
  WaitForGripper(val);
  return true;
}

int main(int argc, char** argv) {
 
  q.resize(5);
  qArmHome.resize(5);
  qArmRight.resize(5);
  gripperJointPositions.resize(2);

  gripperJointPositions[0].joint_uri = "gripper_finger_joint_l";
  gripperJointPositions[0].unit = boost::units::to_string(boost::units::si::meters);
        
  gripperJointPositions[1].joint_uri = "gripper_finger_joint_r";
  gripperJointPositions[1].unit = boost::units::to_string(boost::units::si::meters);

  ros::init(argc, argv, "mahni");
  ros::NodeHandle n;
  ros::Subscriber subArmActionResult = n.subscribe("arm_1/arm_controller/follow_joint_trajectory/result", 1000, &ArmResultListener);
  ros::Subscriber subJointStates = n.subscribe("joint_states", 1000, &UpdateRobotPose);
  armTrajectoryPublisher = n.advertise<control_msgs::FollowJointTrajectoryActionGoal> ("arm_1/arm_controller/follow_joint_trajectory/goal",1);

  gripperPositionPublisher = n.advertise<brics_actuator::JointPositions > ("arm_1/gripper_controller/position_command", 1);

  ros::ServiceServer serviceGoHome = n.advertiseService("go_home", GoHome);
  ros::ServiceServer serviceGoRight = n.advertiseService("go_right", GoRight);
  ros::ServiceServer serviceOpenGripper = n.advertiseService("open_gripper", OpenGripper);
  ros::ServiceServer serviceCloseGripper = n.advertiseService("close_gripper", CloseGripper);

  ros::MultiThreadedSpinner spinner(4); // Use 4 threads
  spinner.spin(); // spin() will not return until the node has been shutdown

  return 0;
}

