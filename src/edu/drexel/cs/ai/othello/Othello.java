package edu.drexel.cs.ai.othello;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

/**
 * A class for playing the game Othello.
 *
 * @author <a href="http://www.sultanik.com" target="_blank">Evan A. Sultanik</a>
 */
public class Othello {
	private OthelloPlayer player1;
	private OthelloPlayer player2;
	private long p1timeUsed;
	private long p2timeUsed;
	private GameState state;
	private UserInterface ui;
	private long turnDuration;
	private static final JailSecurityManager jsm = new JailSecurityManager();

	/**
	 * The release version of this code.
	 */
	public static final String VERSION  = "3.0";
	/**
	 * The release date of this code.
	 */
	public static final String REV_DATE = "2012-12-06";

	/**
	 * Constructs a new othello game with a specific seed to the random number generator.
	 */
	public Othello(OthelloPlayer player1, OthelloPlayer player2, UserInterface ui, long seed) {
		init(player1, player2, ui, seed, true);
	}

	/**
	 * Constructs a new othello game with the random number generator seeded randomly.
	 */
	public Othello(OthelloPlayer player1, OthelloPlayer player2, UserInterface ui) {
		init(player1, player2, ui, 0, false);
	}

	private void init(OthelloPlayer player1, OthelloPlayer player2, UserInterface ui, long seed, boolean useSeed) {
		this.player1 = player1;
		this.player2 = player2;
		p1timeUsed = 0;
		p2timeUsed = 0;
		turnDuration = -1;
		this.ui = ui;
		if(useSeed)
			this.state = new GameState(seed);
		else
			this.state = new GameState();
		System.setSecurityManager(jsm);
	}

	private final static class JailSecurityManager extends SecurityManager {
		public JailSecurityManager() {}
		private void validate(String error) {
			if(Thread.currentThread() instanceof PlayerTimerThread)
				throw new SecurityException(error);
		}
		public void checkWrite(String filename) {
			validate("You cannot write to any files!");
		}
		public void checkDelete(String filename) {
			validate("No files may be deleted!");
		}
		public void checkExec(String cmd) {
			validate("This thread may not execute a subprocess!");
		}
		public void checkExit(int status) {
			validate("This thread may not call System.exit()!");
		}
		public void checkLink(String lib) {
			if(!lib.equals("management") || (Thread.currentThread() instanceof PlayerTimerThread && !((PlayerTimerThread)Thread.currentThread()).gettingCpuTime))
				validate("This thread may not call native libraries!");
		}
		public void checkConnect(String host, int port) {
			validate("This thread may not open sockets!");
		}
		public void checkAccess(Thread t) {
			validate("This thread may not create or modify any threads, including itself.");
		}
	}

	/**
	 * Attempts to instantiate a new {@link OthelloPlayer} with the
	 * given <code>playerName</code> from the given class.
	 *
	 * @throws ClassNotFoundException if the class named <code>className</code> was not found in the current classpath.
	 * @throws NoSuchMethodException if the given class does not have a constructor that takes a single {@link java.lang.String} argument.
	 * @throws InstantiationException if <code>className</code> represents an abstract class.
	 * @throws IllegalAccessException if the constructor of <code>className</code> enforces Java language access control and the underlying constructor is inaccessible.
	 * @throws IllegalArgumentException if an unwrapping conversion for primitive arguments of the class' constructor fails.
	 * @throws InvocationTargetException if the constructor of <code>className</code> throws an exception.
	 * @throws ClassCastException if <code>className</code> does not extend {@link OthelloPlayer}.
	 */
	public static OthelloPlayer instantiatePlayer(String className, String playerName)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, ClassNotFoundException,
			ClassCastException
			{
		Class<?> c = Class.forName(className);
		Constructor<?> constr = (Constructor<?>)c.getDeclaredConstructor(String.class);
		Object o = constr.newInstance(playerName);
		return (OthelloPlayer)o;
			}

	final class PlayerTimerThread extends Thread {
		private Object mutex = new Object();
		private OthelloPlayer player;
		private GameState state;
		private long timeLimitMillis;
		private ThreadMXBean bean;
		private long lastCpuTime;
		volatile private long msCpuStart;
		volatile private boolean gettingCpuTime; 
		
		public PlayerTimerThread(OthelloPlayer player, GameState state) {
			super(player.getName());
			this.player = player;
			this.state = state;
			this.bean = null;
			lastCpuTime = 0;
			gettingCpuTime = false;
		}
		
		@Override
		public void start() {
			throw new IllegalThreadStateException("A PlayerTimerThread may only start itself!");
		}
		
		public long getThreadCpuTime() {
			synchronized(mutex) {
				if(isAlive() && bean != null) {
					gettingCpuTime = true;
					lastCpuTime = bean.getCurrentThreadCpuTime() / 1000000;
					gettingCpuTime = false;
				}
				return lastCpuTime;
			}
		}
		
		public long getTimeRemaining() {
			long cpuTime = getThreadCpuTime(); /* need to run this first, because it ensures that msCpuStart is set! */
			return (msCpuStart + timeLimitMillis) - cpuTime;
		}
		
		public long getTimeUsed() {
			long cpuTime = getThreadCpuTime(); /* need to run this first, because it ensures that msCpuStart is set! */
			return cpuTime - msCpuStart;
		}
		
		public boolean hasDeadline() {
			return timeLimitMillis >= 0;
		}
		
		private void terminate() {
			synchronized(mutex) {
				long startTime = System.currentTimeMillis();
				boolean printed = false;
				while(isAlive()) {
					if(!printed && System.currentTimeMillis() - startTime >= 3000) {
						/* if we have been waiting for three seconds or longer... */
						log("Waiting for the " + player.getName() + "'s thread to terminate...");
						printed = true;
					}
					interrupt(); /* wake up the thread if it is sleeping */
					try {
						this.join(500);
					} catch (InterruptedException e) {}
				}
			}
		}
		
		Square getMove(long timeLimitMillis) throws TimeoutException {
			synchronized(mutex) {
				long sleepInterval = timeLimitMillis / 60;
				this.timeLimitMillis = timeLimitMillis;
				super.start();
				try {
					mutex.wait(); /* wait for the thread to actually start */
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while(this.isAlive()) {
					long timeRemaining = player.getTimeRemaining();
					if(timeRemaining < Long.MAX_VALUE)
						ui.updateTimeRemaining(player, (int)(timeRemaining/1000));
					try {
						mutex.wait(sleepInterval);
					} catch (InterruptedException e1) {}
					if(!(player instanceof HumanOthelloPlayer) && player.hasDeadline() && player.getTimeRemaining() <= 0) {
						terminate();
						throw new TimeoutException(player.getName() + " took too long to move!");
					}
				}
				return player.getCurrentBestMove();
			}
		}

		@Override
		public void run() {
			synchronized(mutex) {
				gettingCpuTime = true;
				bean = ManagementFactory.getThreadMXBean();
				gettingCpuTime = false;
				msCpuStart = this.getThreadCpuTime();
				mutex.notifyAll(); /* let getMove() know that we have started */
			}
			player.playInternal(state);
			synchronized(mutex) {
				bean = null;
				mutex.notifyAll(); /* let getMove() know that we have finished */
			}
		}
	}

	/**
	 * Causes this othello game instance to play until completion,
	 * returning the winner.  <code>null</code> is returned if the
	 * game resulted in a tie.
	 */
	public OthelloPlayer play() {
		while(state.getStatus() == GameState.GameStatus.PLAYING) {
			if(state.getPreviousState() != null && state.getPreviousState().getCurrentPlayer() == state.getCurrentPlayer())
				log((state.getCurrentPlayer() == GameState.Player.PLAYER1 ? player1.getName() : player2.getName()) + " gets to go again!");
			ui.handleStateUpdate(state);
			OthelloPlayer player = (state.getCurrentPlayer() == GameState.Player.PLAYER1 ? player1 : player2);
			boolean validMove = true;
			do {
				Square move = null;

				if(!validMove && !(player instanceof HumanOthelloPlayer)) {
					/* the AI player made an invalud move last try, so penalize it by moving it randomly */
					Square moves[] = state.getValidMoves().toArray(new Square[0]);
					int next = state.getRandom().nextInt(moves.length);
					log("Randomly moving " + player.getName() + " to " + moves[next].toString() + "...");
					move = moves[next];
				}

				validMove = true;

				if(player instanceof HumanOthelloPlayer) {
					ui.updateTimeRemaining(player, -1); /* there is no limit for humans */
					Date start = new Date();
					player.playInternal(state);
					move = player.getCurrentBestMove();
					Date end = new Date();
					ui.updateTimeRemaining(player, -1); /* there is no limit for humans */
					if(state.getCurrentPlayer() == GameState.Player.PLAYER1) {
						p1timeUsed += end.getTime() - start.getTime();
						ui.updateTimeUsed(player, p1timeUsed);
					}
					else {
						p2timeUsed += end.getTime() - start.getTime();
						ui.updateTimeUsed(player, p2timeUsed);
					}
				} else if(move == null) {
					/* request a garbage collection before we run the AI agent */
					Runtime.getRuntime().gc();

					/* if we didn't already move the AI player randomly... */
					PlayerTimerThread ptt = new PlayerTimerThread(player, state);
					try {
						move = ptt.getMove(turnDuration);
					} catch(TimeoutException te) {
						log(te);
						/* did the agent register a best move?  if so, use that.  otherwise, move randomly */
						move = ptt.player.getCurrentBestMove();
						if(move == null) {
							Square moves[] = state.getValidMoves().toArray(new Square[0]);
							int next = state.getRandom().nextInt(moves.length);
							log("Randomly moving " + player.getName() + " to " + moves[next].toString() + "...");
							move = moves[next];
						} else
							log("Using " + player.getName() + "'s best move: " + move);
					}
					if(state.getCurrentPlayer() == GameState.Player.PLAYER1) {
						p1timeUsed += player.getTimeUsed();
						ui.updateTimeUsed(player, p1timeUsed);
					}
					else {
						p2timeUsed += player.getTimeUsed();
						ui.updateTimeUsed(player, p2timeUsed);
					}
				}
				try {
					state = state.applyMove(move);
				} catch(InvalidMoveException ime) {
					log(ime);
					ui.handleStateUpdate(state);
					validMove = false;
				}
			} while(!validMove);
		}
		ui.handleStateUpdate(state);
		switch(state.getStatus()) {
		case PLAYER1WON:
			return player1;
		case PLAYER2WON:
			return player2;
		default:
			return null;
		}
	}

	/**
	 * Logs a message to the user interface.
	 */
	public void log(Object message) {
		if(message instanceof Exception && ui instanceof Logger)
			((Logger)ui).log(message.toString(), message);
		else
			log(message.toString());
	}

	/**
	 * Logs a message to the user interface.
	 */
	public void log(String message) {
		if(ui instanceof Logger)
			((Logger)ui).log(message, this);
		else
			System.err.println(message);
	}

	static String getSimplifiedClassName(String className) {
		int lastPeriod = className.lastIndexOf(".");
		if(lastPeriod < 0)
			return className;
		else
			return className.substring(lastPeriod + 1);
	}

	static String getPlayerClassName(String partialName) {
		HashSet<Class<? extends OthelloPlayer>> possiblePlayers = ClassCreator.getClassesContaining(partialName, OthelloPlayer.class);
		/* remove classes that are not instantiable */
		Iterator<Class<? extends OthelloPlayer>> iter = possiblePlayers.iterator();
		while(iter.hasNext()) {
			Class<? extends OthelloPlayer> c = iter.next();
			int modifiers = c.getModifiers();
			if(c.isInterface() || Modifier.isAbstract(modifiers) || Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers))
				iter.remove();
		}
		if(possiblePlayers.isEmpty()) {
			System.err.println("Error: Could not find an OthelloPlayer class whose name contains the text \"" + partialName + "\"\n");
			return null;
		} else if(possiblePlayers.size() > 1) {
			System.err.println("Error: There are multiple OthelloPlayer classes whose name contains the text \"" + partialName + "\":");
			for(Class<? extends OthelloPlayer> c : possiblePlayers)
				System.err.println("\t" + c.getName());
			System.err.print("\n");
			return null;
		} else
			return possiblePlayers.iterator().next().getName();
	}
		
	/**
	 * Instantiates the players, loads the user interface, and plays the game until completion.
	 */
	public static void main(String[] args) {
		UserInterface ui = null;
		String[] sarg = new String[4];
		int sargs = 0;
		boolean printUse = false;
		long seed = 0;
		boolean seedSet = false;
		long turnDuration = -1;

		for(int i=0; i<args.length; i++) {
			if(!args[i].startsWith("-")) {
				/**
				 * This is the class name of an agent
				 */
				if(sargs < 4)
					sarg[sargs++] = args[i];
				else {
					System.err.println("Warning: unexpected argument \"" + args[i] + "\"!");
					printUse = true;
				}
			}
			else if(args[i].equals("-s")) {
				/**
				 * Set the seed to the random number generator
				 */
				if(i == args.length - 1) {
					System.err.println("Error: -s requires an argument (the number with which to seed the random number generator)");
					printUse = true;
				}
				else {
					seed = Long.parseLong(args[++i]);
					seedSet = true;
				}
			}
			else if(args[i].equals("-d")) {
				/**
				 * Set the maximum turn duration
				 */
				if(i == args.length - 1) {
					System.err.println("Error: -d requires an argument (the maximum turn duration in seconds)");
					printUse = true;
				}
				else {
					String d = args[++i];
					if(d.endsWith("ms"))
						turnDuration = Long.parseLong(d.substring(0, d.length() - 2));
					else if(d.endsWith("m"))
						turnDuration = Long.parseLong(d.substring(0, d.length() - 1)) * 60000;
					else if(d.endsWith("s"))
						turnDuration = Long.parseLong(d.substring(0, d.length() - 1)) * 1000;
					else
						turnDuration = Long.parseLong(d) * 1000;
				}
			}
			else if(args[i].equals("-nw")) {
				ui = new ConsoleUserInterface();
			}
			else if(args[i].equals("-t")) {
				ui = new TournamentUserInterface();
			}
			else {
				System.err.println("Warning: unexpected argument \"" + args[i] + "\"!");
				printUse = true;		
			}
		}

		if(ui == null)
			ui = new GraphicalUserInterface();

		OthelloPlayer players[];

		if(sargs < 2) {
			players = ui.getPlayers();
		} else {
			players = new OthelloPlayer[2];
			String player1class = getPlayerClassName(sarg[0]);
			if(player1class == null) {
				printUse = true;
			} else {
				String player1name = (sargs > 2 ? sarg[1] : getSimplifiedClassName(player1class));
				if(player1name.equals(""))
					player1name = "Player 1";
				String player2class = (sargs > 2 ? getPlayerClassName(sarg[2]) : getPlayerClassName(sarg[1]));
				if(player2class == null) {
					printUse = true;
				} else {
					String player2name = (sargs > 3 ? sarg[3] : getSimplifiedClassName(player2class));
					if(player2name.equals(player1name))
						player2name = player2name + "2";
					else if(player2name.equals(""))
						player2name = "Player 2";
					try {
						players[0] = instantiatePlayer(player1class, player1name);
					} catch(NoSuchMethodException nsme1) {
						System.err.println("Error Instantiating Agent: Make sure the agent class for player 1 (" + player1class + ")\nhas a constructor that accepts a single string as an argument!");
						printUse = true;
					} catch(Exception e1) {
						System.err.println("Error Instantiating Agent: " + e1.toString());
						printUse = true;
					}
					try {
						players[1] = instantiatePlayer(player2class, player2name);
					} catch(NoSuchMethodException nsme2) {
						System.err.println("Error Instantiating Agent: Make sure the agent class for player 2 (" + player2class + ")\nhas a constructor that accepts a single string as an argument!");
						printUse = true;
					} catch(Exception e2) {
						System.err.println("Error Instantiating Agent: " + e2.toString());
						printUse = true;
					}
				}
			}
		}

		if(printUse) {
			printUsage();
			System.exit(1);
		}

		ui.setPlayers(players[0], players[1]);
		if(ui instanceof Logger) {
			players[0].setLogger((Logger)ui);
			players[1].setLogger((Logger)ui);
		}
		Othello othello;
		if(seedSet)
			othello = new Othello(players[0], players[1], ui, seed);
		else
			othello = new Othello(players[0], players[1], ui);
		othello.turnDuration = turnDuration;
		if(ui instanceof Logger)
			((Logger)ui).log(getVersionInfo(), null);
		else
			System.out.println(getVersionInfo());
		OthelloPlayer winner = othello.play();
		if(winner == null)
			othello.log("It was a tie!");
		else
			othello.log("The winner was " + winner + "!");
	}

	static String getVersionInfo() {
		return "Othello Version " + VERSION + " " + REV_DATE + "\n" +
				"Copyright 2006--" + REV_DATE.substring(0,REV_DATE.indexOf("-")) + ", Evan A. Sultanik" + "\n" +
				"http://www.sultanik.com/" + "\n" + "\n";
	}

	/**
	 * Prints command line usage information.
	 */
	public static void printUsage() {
		System.err.println(getVersionInfo());
		System.err.println("Usage: othello [options] [player1class [player1name] player2class [player2name]]");
		System.err.println();
		System.err.println("  player1class      Class name of the agent for player1");
		System.err.println("                    (i.e., \"org.drexel.edu.cs.ai.othello.RandomOthelloPlayer\")");
		System.err.println("  player1name       The name for player1 (i.e. \"Evan's Agent\")");
		System.err.println("  player2class      Class name of the agent for player2");
		System.err.println("  player2name       The name for player2");
		System.err.println();
		System.err.println("OPTIONS:");
		System.err.println("         -s  number Seed for the simulator's random number generator.");
		System.err.println("                    If omitted, time since the epoch is used.");
		System.err.println("         -nw        Run in console mode (a GUI is used by default)");
		System.err.println("         -t         Run in tournament mode (nothing is printed except");
		System.err.println("                    the final score of each player).");
		System.err.println("         -d  number Sets the amount of time (in seconds) an agent has to make");
		System.err.println("                    its decision each turn (i.e., the deadline).");
		System.err.println("                    \"ms\" or \"m\" can be appended to the number to change the");
		System.err.println("                    time units to milliseconds or minutes, respectively.");
		System.err.println("                    A value <= 0 will result in an infinite deadline (this is");
		System.err.println("                    the default).");
	}
}
