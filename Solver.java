import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;

// solves puzzles from Wordle
public class Solver{
	public static void main(String[] args){
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> expandedWords = new ArrayList<String>();

		// 'shorter list.txt' is the list of all possible correct answers for Wordle
		// it is the 'shorter' of the two text files since Wordle accepts certain words that it will never set as the correct word
		// those extra words, along with the normal ones, are stored in a separate text file
		File smallerFile = new File("shorter list.txt");
		File largerFile = new File("five letter words.txt");

		// read the contents from the file and store them in an array list
		try (FileInputStream fis = new FileInputStream(smallerFile); BufferedInputStream bis = new BufferedInputStream(fis)){
			String s = new String(bis.readAllBytes());
			String[] arr = s.split("\n");
			for (String each : arr)
				words.add(each);
		} catch (FileNotFoundException ex){
			System.err.println("file not found");
		} catch (IOException ex){
			System.err.println("IO exception");
		}

		try (FileInputStream fis = new FileInputStream(largerFile); BufferedInputStream bis = new BufferedInputStream(fis)){
			String s = new String(bis.readAllBytes());
			String[] arr = s.split("\n");
			for (String each : arr)
				expandedWords.add(each);
		} catch (FileNotFoundException ex){
			System.err.println("file not found");
		} catch (IOException ex){
			System.err.println("IO exception");
		}
		
		// begin taking in user input
		Scanner kb = new Scanner(System.in);
		
		// the manual solver allows the user to manually enter in guesses and results
		// the program will then recommend the best guess
		// the auto solver guesses each possible Wordle answer and prints the number of guesses it took
		System.out.print("Manual or auto solver (y/n)? ");
		boolean autoSolver = kb.next().charAt(0) == 'n';

		// manual solver
		if (!autoSolver){
			System.out.print("Allow obscure words as guesses (y/n)? ");
			boolean allowObscureWords = kb.next().charAt(0) == 'y';
			int numberOfGuesses = 0;
			while (true){
				System.out.print("Enter your guess: ");
				String guess = kb.next();

				// alows the user to prematurely exit the program
				// 'exit' is chosen because it is only 4 letters, and every Wordle word is 5, so it can never be confused for a valid guess
				if (guess.equals("exit"))
					break;

				// the user puts their word into Wordle, and translates the feedback to the program
				System.out.print("Enter your result as a sequence of numbers,\n" + 
					"where 0 = gray, 1 = yellow, 2 = green (eg 01021): ");
				String result = kb.next();
				numberOfGuesses++;

				if (result.equals("22222")){
					System.out.println("The word was correctly guessed in " + numberOfGuesses + (numberOfGuesses == 1 ? " try" : " tries"));
					return;
				}


				// iterate through every word it could possibly be
				for (int i = 0; i < words.size(); i++){
					boolean willRemove = false;

					// iterate through every digit of the feedback
					for (int j = 0; j < 5; j++){
						String word = words.get(i);
						switch (result.charAt(j)){
							case '0':
								// if the word contains a letter we know the answer does not, that word cannot be correct
								if (word.indexOf(guess.charAt(j)) >= 0 && result.charAt(word.indexOf(guess.charAt(j))) != '2')
									willRemove = true;
								break;
							
							case '1':
								// if the word does not have a letter we know is in the answer, that word cannot be correct
								// also, if the word has the letter in the guessed space, and the answer does not, that word cannot be correct
								if (word.indexOf(guess.charAt(j)) < 0 || word.charAt(j) == guess.charAt(j))
									willRemove = true;
								break;
							
							case '2':
								// if the word does not have a letter in the exact space we know the answer does, that word cannot be correct
								if (word.charAt(j) != guess.charAt(j))
									willRemove = true;
									break;
									
							default:
								break;
						}
						
						if (willRemove)
							break;
					}
					
					// remove the word if it determined it could not be correct
					if (willRemove){
						words.remove(i);
						i--;
					}
				}
				
				// the next part of the solver assesses what the best next guess would be
				// it does so by adding up all occurences of every letter in the corresponding place of every possible correct word
				int[][] charFreq = new int[5][26];
				
				for (int i = 0; i < words.size(); i++){
					String word = words.get(i);
					// 97 is subtracted because 97 is 'a' in ASCII, and we want our indices from 0-25
					charFreq[0][(int)word.charAt(0)- 97]++;
					charFreq[1][(int)word.charAt(1)- 97]++;
					charFreq[2][(int)word.charAt(2)- 97]++;
					charFreq[3][(int)word.charAt(3)- 97]++;
					charFreq[4][(int)word.charAt(4)- 97]++;
				}
				
				// select the highest-scoring word to recommend as the best next guess
				int high = 0;
				String bestWord = "";
				ArrayList<String> possibleWords = new ArrayList<String>();
				if (allowObscureWords)
					possibleWords = expandedWords;
				else
					possibleWords = words;
				for (String word : possibleWords){
					ArrayList<Character> usedLetters = new ArrayList<Character>();
					int score = 0;
					
					// words with more than one of the same letter get unreasonably large scores when a letter is known to be somewhere in the word
					// those words actually give less information that ones with five unique letters, so their scores are penalized to discourage them
					// the penalty takes only the highest-scoring slot for that letter and has the rest of that same letter give 0 points
					for (int i = 0; i < 5; i++){
						char c = word.charAt(i);
						if (usedLetters.contains(c)){
							if (charFreq[usedLetters.indexOf(c)][(int)c - 97] < charFreq[i][(int)c - 97]){
								usedLetters.set(usedLetters.indexOf(c), '-');
								usedLetters.add(c);
								score -= charFreq[usedLetters.indexOf(c)][(int)c - 97];
								score += charFreq[i][(int)c - 97];
							} else {
								usedLetters.add('-');
							}
						} else {
							usedLetters.add(c);
							score += charFreq[i][(int)c - 97];
						}
					}
					
					// the list of remaining possible words is only printed when there is a reasonable number of them left for the user to look through manually
					if (words.size() > 1 && words.size() <= 20 && words.contains(word))
						System.out.println(word);

					// determine the best next guess
					if (score > high){
						high = score;
						bestWord = word;
					}
				}
				
				if (words.size() == 0){
					System.out.println("fatal: possible word list was completely exhausted. Unable to determine the correct word");
					return;
				} else if (words.size() == 1){
					System.out.println("the only possible remaining word is " + words.get(0));
				} else {
					System.out.println(words.size() + " possible words left");
					System.out.println("best next guess is " + bestWord + ", scoring " + high + " points");
				}
			}
		} else if (autoSolver){
			// the auto-solver solves every possible Wordle word and prints out the results
			// this is only used to gather statistics
			final ArrayList<String> fullwords = new ArrayList<String>();

			// fullwords is an unchanging list of possible Wordle words
			// this is needed so words can be removed from the dynamic list to knock off impossible words, but still be reset when a new word is to be guessed
			for (String w : words){
				fullwords.add(w);
			}
			
			for (String randomWord : fullwords){
				// restore the dynamic word list
				for (String w : fullwords){
					words.add(w);
				}

				// the following is essentially the same algorithm as the manual solver, except it keeps track of the number of guesses
				String guess = "*****";
				String result = "00000";
				int tries = 0;
				while (true){
					tries++;
					for (int i = 0; i < words.size(); i++){
						boolean willRemove = false;
						for (int j = 0; j < 5; j++){
							String word = words.get(i);
							switch (result.charAt(j)){
								case '0':
									if (word.indexOf(guess.charAt(j)) >= 0 && result.charAt(word.indexOf(guess.charAt(j))) != '2')
										willRemove = true;
									break;
								
								case '1':
									if (word.indexOf(guess.charAt(j)) < 0 || word.charAt(j) == guess.charAt(j))
										willRemove = true;
									break;
								
								case '2':
									if (word.charAt(j) != guess.charAt(j))
										willRemove = true;
										break;
										
								default:
									break;
							}
							
							if (willRemove)
								break;
						}
						
						if (willRemove){
							words.remove(i);
							i--;
						}
					}
					
					// if the list has run out of words, then the program was somehow unable to determine the word
					// this should never run
					if (words.size() == 0){
						System.out.println("Unable to guess word");
						break;
					}
					
					int[][] charFreq = new int[5][26];
					
					for (int i = 0; i < words.size(); i++){
						String word = words.get(i);
						if (word.length() != 5)
							System.out.println(word);
						// System.out.println(word);
						charFreq[0][(int)word.charAt(0)- 97]++;
						charFreq[1][(int)word.charAt(1)- 97]++;
						charFreq[2][(int)word.charAt(2)- 97]++;
						charFreq[3][(int)word.charAt(3)- 97]++;
						charFreq[4][(int)word.charAt(4)- 97]++;
					}
					
					int high = 0;
					String bestWord = "";
					for (String word : words){
						ArrayList<Character> usedLetters = new ArrayList<Character>();
						int score = 0;
						
						for (int i = 0; i < 5; i++){
							char c = word.charAt(i);
							// System.out.println(usedLetters);
							if (usedLetters.contains(c)){
								if (charFreq[usedLetters.indexOf(c)][(int)c - 97] < charFreq[i][(int)c - 97]){
									usedLetters.set(usedLetters.indexOf(c), '-');
									usedLetters.add(c);
									score -= charFreq[usedLetters.indexOf(c)][(int)c - 97];
									score += charFreq[i][(int)c - 97];
								} else {
									usedLetters.add('-');
								}
							} else {
								usedLetters.add(c);
								score += charFreq[i][(int)c - 97];
							}
						}
						
						
						// System.out.println(word + " " + score);
						if (score > high){
							high = score;
							bestWord = word;
						}
					}
					
					// System.out.println("best guess is " + bestWord);
					guess = bestWord;
					int[] arr = new int[5];
					for (int i = 0; i < 5; i++){
						if (guess.charAt(i) == randomWord.charAt(i))
							arr[i] = 2;
						else if (randomWord.indexOf(guess.charAt(i)) >= 0 && randomWord.charAt(i) != guess.charAt(i))
							arr[i] = 1;
						else
							arr[i] = 0;
					}
					result = new String(arr[0] + "" + arr[1] + "" + arr[2] + "" + arr[3] + "" + arr[4]);
					
					// if every letter is in the correct spot, the guess must be correct
					if (result.equals("22222")){
						// System.out.println("Solved in " + tries + (tries == 1 ? " try" : " tries") + ": the word was " + guess);
						System.out.println(guess + " " + tries);
						break;
					}
				}
			}
		}
	}
}
