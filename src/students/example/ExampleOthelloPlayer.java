package students.example;

import java.util.Date;

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
	public Square getMove(GameState currentState, Date deadline) {
		Square square = currentState.getValidMoves().toArray(new Square[0])[0];
		log("Example player is moving to " + square + "...");
		return square;
	}
}
