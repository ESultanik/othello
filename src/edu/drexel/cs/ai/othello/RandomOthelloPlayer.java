package edu.drexel.cs.ai.othello;

/**
 * An othello-playing agent that plays at random.
 *
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public class RandomOthelloPlayer extends OthelloPlayer {
	/**
	 * Creates a new othello-playing agent that plays at random.
	 */
	public RandomOthelloPlayer(String name) {
		super(name);
	}

	/**
	 * Returns a random, valid move from <code>currentState</code>.
	 */
	@Override
	public void play(GameState currentState) {
		Square moves[] = currentState.getValidMoves().toArray(new Square[0]);
		int next = currentState.getRandom().nextInt(moves.length);
		log("Randomly moving to " + moves[next].toString() + "...");
		registerCurrentBestMove(moves[next]);
	}
}
