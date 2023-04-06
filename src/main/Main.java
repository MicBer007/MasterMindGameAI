package main;

import gradientDescentOnManualAI.GradientDescentAlgorithm;
import mastermind.MasterMind;

public class Main {

	public static void main(String[] args) {
//		NetworkController.startLearningProcedure(true);
		GradientDescentAlgorithm.learn(1);
	}
	
	public static void doMasterMind() {
		new MasterMind();
	}

}
