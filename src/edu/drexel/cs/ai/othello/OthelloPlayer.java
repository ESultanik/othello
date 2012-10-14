package edu.drexel.cs.ai.othello;

import java.util.Date;

/**
 * This class provides the API for othello-playing agents.  Here is an
 * example of a simple random othello-playing agent (see {@link
 * RandomOthelloPlayer}):
 * <p><pre>
public class RandomOthelloPlayer extends OthelloPlayer {
    public RandomOthelloPlayer(String name) {
	super(name);
    }

    public Square getMove(GameState currentState, Date deadline) {
	Square moves[] = currentState.getValidMoves().toArray(new Square[0]);
	int next = currentState.getRandom().nextInt(moves.length);
	log("Randomly moving to " + moves[next].toString() + "...");
	return moves[next];
    }
}
</pre></p>
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public abstract class OthelloPlayer {
    private String name;
    private Logger logger;
    private Date currentDeadline;

    /**
     * Creates a new Othello Player
     */
    public OthelloPlayer(String name) {
	this.name = name;
	logger = null;
	currentDeadline = null;
    }

    /**
     * Returns the move chosen by this player given the current game
     * state.  <code>deadline</code> is the time by which this
     * function must return.  If <code>deadline</code> is
     * <code>null</code>, there is no deadline.
     */
    public abstract Square getMove(GameState currentState, Date deadline);

    /**
     * Returns the name of this player.
     */
    public String getName() {
	return name;
    }

    Square getMoveInternal(GameState currentState, Date deadline) {
	currentDeadline = deadline;
	Square move = getMove(currentState, deadline);
	currentDeadline = null;
	return move;
    }

    void setLogger(Logger logger) {
	this.logger = logger;
    }

    /**
     * Utility function for returning the number of milliseconds remaining until the deadline.
     */
    protected long getMillisUntilDeadline() {
	if(currentDeadline == null)
	    return 0;
	else
	    return currentDeadline.getTime() - (new Date()).getTime();
    }

    /**
     * Sends a log message to the user interface.
     */
    protected void log(String message) {
	if(logger == null)
	    System.out.println(name + ": " + message);
	else
	    logger.log(message, this);
    }

    /**
     * Returns a string representation of this player.
     */
    public String toString() {
	return name;
    }
}
