package gradientDescentOnManualAI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import log.Log;
import log.LogLevel;
import mastermind.Combination;
import mastermind.Pin;
import mastermind.Response;

public class GradientDescentAlgorithm {
	
	private static Log log = new Log("GDA", LogLevel.INFO);
	
	private static final Combination[] POSSIBLE_COMBINATIONS = determinePossibleCombinations();
	
	private static final Response[] POSSIBLE_RESPONSES = new Response[] {
			new Response(0, 1),//1
			new Response(0, 2),//2
			new Response(1, 0),//4
			new Response(1, 1),//5
			new Response(0, 0),//0
			new Response(2, 0),//8
			new Response(1, 2),//6
			new Response(0, 3),//3
			new Response(2, 1),//9
			new Response(3, 0),//12
	};
	
	/*
	 * The following manually derived weight and bias values have solved with a 
	 * min of 3, max of 10, averaging 6.088 rounds per game, out of 500 games
	 */
	private static float[] weights = new float[] {
			0.9f, //W
			0.63f, //WW
			0.597f, //B
			0.549f, //BW
			0.511f, //
			0.213f, //BB
			0.142f, //BWW
			0.119f, //WWW
			0.0594f, //BBW
			0.0264f //BBB
	};
	
	private static float bias = 2.2f;
	
	private static int sampleSize = 20;
	
	private static float percentageChangeForWeights = 0.01f;
	
	private static float friction = 0.5f;
	
	public static void learn(int numberOfIterations) {
		float originalSucess = -1;
		float sucess = 0;
		for(int i = 0; i < numberOfIterations; i++) {
			//evaluate current Pool
			List<Combination> startingPool = createSamplePool();
			sucess = evaluateCombinationPool(startingPool);
			if(originalSucess == -1) {
				originalSucess = sucess;
			}
			//weights
			for(boolean up = false; up == true; up = !up) {
				for(int weightID = 0; weightID < POSSIBLE_RESPONSES.length; weightID++) {
					sucess = doGradientDescentOnWeight(weightID, up, sucess);
					System.out.println("Finished checking " + (up ? "up":"down") + " for weight number " + weightID + "! Sucess = " + sucess);
				}
			}
			//bias value
			for(boolean up = false; up == true; up = !up) {
				sucess = doGradientDescentOnBiasValue(up, sucess);
				System.out.println("Finished checking " + (up ? "up":"down") + " for bias value! Sucess = " + sucess);
			}
			percentageChangeForWeights *= friction;
		}
		System.out.println("Sucess: " + sucess);
		System.out.println("Original sucess: " + originalSucess);
		for(float weight: weights) {
			System.out.println("Weight: " + weight);
		}
		System.out.println("Bias: " + bias);
	}
	
	private static final int MAX_NUMBER_OF_ROUNDS_TO_TAKE = 16;
	
	public static float evaluateCombinationPool(List<Combination> combinationPool) {
		log.info("Evaluating new combination pool!");
		int totalRoundsTaken = 0;
		for(int i = 0; i < combinationPool.size(); i++) {
			log.info("Evaluating combination: " + combinationPool.get(i).getCombinationAsText());
			Combination solution = combinationPool.get(i);
			List<Combination> combinationsLeft = new ArrayList<Combination> (Arrays.asList(POSSIBLE_COMBINATIONS.clone()));
			Combination lastGuess = null;
			Combination secondLastGuess = null;
			int roundNum;
			for(roundNum = 1; roundNum < MAX_NUMBER_OF_ROUNDS_TO_TAKE; roundNum++) {
				Combination guess = makeGuess(roundNum, combinationsLeft, lastGuess, secondLastGuess);
				if(guess == solution) {
					totalRoundsTaken += roundNum;
					log.info("Correct combination guessed in " + roundNum + " rounds.");
					break;
				}
				secondLastGuess = lastGuess;
				lastGuess = guess;
				combinationsLeft = reduceCombinationsAccordingToGuess(combinationsLeft, guess, solution);
			}
			if(roundNum == MAX_NUMBER_OF_ROUNDS_TO_TAKE) {
				log.fatal("Exceeded max number of rounds!");
				System.exit(-1);
			}
		}
		return (float) (totalRoundsTaken) / (float) (combinationPool.size());
	}
	
	private static List<Combination> reduceCombinationsAccordingToGuess(List<Combination> possibleCombinations, Combination guess, Combination solution) {
		Response response = guess.compareToCombination(solution);
		for(int i = 0; i < possibleCombinations.size(); i++) {
			Combination combination = possibleCombinations.get(i);
			if(!combination.compareToCombination(guess).isEquivalentToResponse(response)) {
				possibleCombinations.remove(combination);
				i--;
			}
		}
		return possibleCombinations;
	}
	
	public static Combination makeGuess(int roundNumber, List<Combination> combinationsLeft, Combination lastGuess, Combination secondLastGuess) {
		log.info("Computer is determining the best combination to guess out of " + combinationsLeft.size() + " combinations.");
		if(combinationsLeft.size() <= 2) {
			if(combinationsLeft.size() == 0) {
				log.fatal("There are no combinations left!");
				System.exit(-1);
			}
			log.info("Chose combination: " + combinationsLeft.get(0).getCombinationAsText());
			return combinationsLeft.get(0);
		}
		if(roundNumber == 1) {
			Combination defaultCombination = new Combination(Pin.A, Pin.A, Pin.B, Pin.D);
			log.info("Chose combination: " + defaultCombination.getCombinationAsText());
			return defaultCombination;
		}
		
		Combination bestCombination = null;
		float numberOfEliminations = 0;
		for(Combination possibleGuess: POSSIBLE_COMBINATIONS) {
			float cNumberOfEliminations = 0;
			for(Combination d: combinationsLeft) {
				Response r = d.compareToCombination(possibleGuess);
				for(int i = 0; i < POSSIBLE_RESPONSES.length; i++) {
					if(!POSSIBLE_RESPONSES[i].isEquivalentToResponse(r)) {
						cNumberOfEliminations += bias + weights[i];
					}
				}
			}
			if(cNumberOfEliminations >= numberOfEliminations) {
				if(possibleGuess.isEquivalentToCombination(lastGuess)) {
					continue;
				}
				if(secondLastGuess != null && possibleGuess.isEquivalentToCombination(secondLastGuess)) {
					continue;
				}
				bestCombination = possibleGuess;
				numberOfEliminations = cNumberOfEliminations;
			}
		}
		if(bestCombination == null) {
			log.fatal("Function did not make a guess!");
			System.exit(-1);
		}
		log.info("Chose combination: " + bestCombination.getCombinationAsText());
		return bestCombination;
	}
	
	private static float doGradientDescentOnWeight(int weightID, boolean up, float previousEvaluation) {
		List<Combination> samplePool = createSamplePool();
		float originalWeight = weights[weightID];
		float change = originalWeight*percentageChangeForWeights;
		weights[weightID] += (up ? change : -change);
		float newEvaluation = evaluateCombinationPool(samplePool);
		if(newEvaluation > previousEvaluation) {
			return newEvaluation;
		} else {
			weights[weightID] -= (up ? change : -change);
			return previousEvaluation;
		}
	}
	
	private static float doGradientDescentOnBiasValue(boolean up, float previousEvaluation) {
		List<Combination> samplePool = createSamplePool();
		float originalBias = bias;
		float change = originalBias*percentageChangeForWeights*0.05f;
		bias += (up ? change : -change);
		float newEvaluation = evaluateCombinationPool(samplePool);
		if(newEvaluation > previousEvaluation) {
			return newEvaluation;
		} else {
			bias -= (up ? change : -change);
			return previousEvaluation;
		}
	}
	
	private static List<Combination> createSamplePool() {
		List<Combination> allCombinations = Arrays.asList(POSSIBLE_COMBINATIONS.clone());
		Collections.shuffle(allCombinations);
		List<Combination> samplePool = allCombinations.subList(0, sampleSize);
		if(samplePool.size() != sampleSize) {
			log.fatal("Sample pool is not the right size!");
			System.exit(-1);
		}
		return samplePool;
	}
	
	public static void saveInMemory(String fileName) { }
	
	public static Combination[] determinePossibleCombinations() {
		Combination[] combinations = new Combination[4096];
		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				for(int k = 0; k < 8; k++) {
					for(int l = 0; l < 8; l++) {
						combinations[(((i * 8) + j) * 8 + k) * 8 + l] = new Combination(Pin.values()[i], Pin.values()[j], Pin.values()[k], Pin.values()[l]);
					}
				}
			}
		}
		if(!combinations[4095].compareToCombination(new Combination(Pin.H, Pin.H, Pin.H, Pin.H)).isSolved()) {
			System.out.println("method determinePossibleCombinations in GradientDescentAlgoritm failed!");
			System.exit(-1);
		}
		return combinations;
	}
	
//	private static Random random = new Random();
//	
//	private static Combination getRandomCombination() {
//		return new Combination(Pin.values()[random.nextInt(8)], Pin.values()[random.nextInt(8)], Pin.values()[random.nextInt(8)], Pin.values()[random.nextInt(8)]);
//	}

}
