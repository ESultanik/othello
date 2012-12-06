package edu.drexel.cs.ai.othello;

/**
 * An interface for having a human play othello through the {@link
 * UserInterface user interface}.
 *
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public final class HumanOthelloPlayer extends OthelloPlayer {
	Square nextMove;
	Object mutex;

	/**
	 * Creates a new agent that plays according to human input.
	 */
	public HumanOthelloPlayer(String name) {
		super(name);
		nextMove = null;
		mutex = new Object();
	}

	/**
	 * Callback function for receiving the next move from the UI.
	 */
	public void handleUIInput(Square square) {
		synchronized(mutex) {
			nextMove = square;
			mutex.notifyAll();
		}
	}

	/**
	 * Returns the next move as input by the human from the UI.  Note
	 * that this function will block until the UI makes a call to
	 * {@link #handleUIInput(Square)} with the next move.  Also, the
	 * HumanOthelloPlayer agent will always have an infinite deadline.
	 */
	@Override
	public void play(GameState currentState) {
		synchronized(mutex) {
			while(nextMove == null) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {}
			}
			registerCurrentBestMove(nextMove);
			nextMove = null;
		}
	}
}
