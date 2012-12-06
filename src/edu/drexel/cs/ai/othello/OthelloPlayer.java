package edu.drexel.cs.ai.othello;

/**
 * This class provides the API for othello-playing agents.  Here is an
 * example of a simple random othello-playing agent (see {@link
 * RandomOthelloPlayer}):
 * <p><pre>
public class RandomOthelloPlayer extends OthelloPlayer {
    public RandomOthelloPlayer(String name) {
		super(name);
    }

    public void play(GameState currentState) {
		Square moves[] = currentState.getValidMoves().toArray(new Square[0]);
		int next = currentState.getRandom().nextInt(moves.length);
		log("Randomly moving to " + moves[next].toString() + "...");
		registerCurrentBestMove(moves[next]);
    }
}
</pre></p>
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public abstract class OthelloPlayer {
	private String name;
	private Logger logger;
	volatile private Square tempMove;
	volatile private Thread currentThread;

	/**
	 * Creates a new Othello Player
	 */
	public OthelloPlayer(String name) {
		this.name = name;
		logger = null;
		currentThread = null;
	}

	/**
	 * A function in which the agent should make its choice for the next move from the given state.
	 * 
	 * @see {@link #registerCurrentBestMove(Square)}
	 */
	public abstract void play(GameState currentState);

	/**
	 * Returns the name of this player.
	 */
	public String getName() {
		return name;
	}

	synchronized void playInternal(GameState currentState) {
		if(currentThread != null)
			throw new IllegalStateException("getMoveInternal(...) is already being called by another thread (" + currentThread + ")");
		currentThread = Thread.currentThread();
		tempMove = null;
		try {
			play(currentState);
		} finally {
			getCurrentThreadCpuTime(); /* a side-effect of this function is to record the last CPU time usage, so run it before we wipe the currentThread and threadBean objects! */
			currentThread = null;
		}
	}
	
	/**
	 * Register's the best move the agent has found so far in its search.  If the agent runs out of time, this is the move that will be used for the agent.  If no move is registered and the agent misses its deadline, then a move will be chosen at random.
	 * 
	 * @param bestMove The best move that the agent has found so far.
	 * @throws IllegalStateException if {@link #play(GameState)} is not currently being run, or if it is being run from a different thread.
	 */
	protected final void registerCurrentBestMove(Square bestMove) throws IllegalStateException {
		if(currentThread == null)
			throw new IllegalStateException("This OthelloPlayer is not currently running play(...)!");
		else if(currentThread != Thread.currentThread())
			throw new IllegalStateException("registerCurrentBestMove(...) can only be called from the thread that is currently running play(...): " + currentThread);
		else if(this.getTimeRemaining() > 0)
			tempMove = bestMove; /* only set the move if the deadline hasn't yet expired */
	}
	
	/**
	 * Returns The best move that the agent has found so far, as registered using {@link #registerCurrentBestMove(Square)}.  If no move has been registered, or if {@link #play(GameState)} is not currently running, then <code>null</code> is returned.
	 */
	protected final Square getCurrentBestMove() {
		return tempMove;
	}

	void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Returns the number of milliseconds remaining until the deadline.
	 * {@link java.lang.Long.MAX_VALUE} is returned if there is no deadline.
	 */
	protected final long getTimeRemaining() {
		if(currentThread == null || !hasDeadline() || !(currentThread instanceof Othello.PlayerTimerThread))
			return Long.MAX_VALUE; /* the player isn't running yet, or there is no deadline */
		else
			return ((Othello.PlayerTimerThread)currentThread).getTimeRemaining();
	}
	
	protected final long getCurrentThreadCpuTime() {
		if(currentThread == null || !(currentThread instanceof Othello.PlayerTimerThread))
			return 0;
		else
			return ((Othello.PlayerTimerThread)currentThread).getThreadCpuTime();
	}
	
	/**
	 * Returns whether this agent has a deadline.
	 */
	protected final boolean hasDeadline() {
		return currentThread != null && (currentThread instanceof Othello.PlayerTimerThread) && ((Othello.PlayerTimerThread)currentThread).hasDeadline();
	}
	
	/**
	 * Returns the number of milliseconds of CPU time that have been consumed by this agent since {@link #play(GameState)} was called.
	 */
	protected final long getTimeUsed() {
		if(currentThread == null || !(currentThread instanceof Othello.PlayerTimerThread))
			return 0;
		else
			return ((Othello.PlayerTimerThread)currentThread).getTimeUsed();
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
