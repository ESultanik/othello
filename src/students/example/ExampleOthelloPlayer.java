package students.example;

import edu.drexel.cs.ai.othello.GameState;
import edu.drexel.cs.ai.othello.OthelloPlayer;
import edu.drexel.cs.ai.othello.Square;

/**
 * An example of how students should implement their own agents.
 *
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public class ExampleOthelloPlayer extends OthelloPlayer {
	/**
	 * Creates a new <code>ExampleOthelloPlayer</code>.
	 */
	public ExampleOthelloPlayer(String name) {
		super(name);
	}

	/**
	 * Returns the first move that the agent discovers is valid.
	 */
	@Override
	public void play(GameState currentState) {
		if(hasDeadline())
			log("I have " + this.getTimeRemaining() + "ms remaining until the deadline.");
		
		/* register this as our current best move; if there is a deadline and we don't reach it,
		 * then registering the move will make sure that that is the move we take.
		 * If we reach the deadline and we neither registered a move nor returned from this
		 * function, then a move will be chosen for us at random. */
		Square square = currentState.getValidMoves().toArray(new Square[0])[0];
		this.registerCurrentBestMove(square);
		
		/* registerCurrentBestMove(...) can be called multiple times to reset the current best
		 * move before returning from this function. */
	}
}
