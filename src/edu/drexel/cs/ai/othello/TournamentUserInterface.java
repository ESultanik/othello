package edu.drexel.cs.ai.othello;

public class TournamentUserInterface extends ConsoleUserInterface implements Logger {
	@Override
	public void setPlayers(OthelloPlayer player1, OthelloPlayer player2) {
		if(player1 instanceof HumanOthelloPlayer || player2 instanceof HumanOthelloPlayer)
			throw new IllegalArgumentException("The TournamentUserInterface may not be used with the HumanOthelloPlayer!");
		super.setPlayers(player1, player2);
	}
	
	@Override
	public void updateTimeUsed(OthelloPlayer player, long millisUsed) {}
	
	public void handleStateUpdate(GameState newState) {
		int move = newState.getScore(newState.getCurrentPlayer()) + newState.getScore(newState.getOpponent(newState.getCurrentPlayer())) - 4;
		System.out.println("Move " + move);
		if(!newState.getStatus().equals(GameState.GameStatus.PLAYING))
			System.out.println(newState.getScore(GameState.Player.PLAYER1) + "\t" + newState.getScore(GameState.Player.PLAYER2));
	}

	@Override
	public void log(String message, Object source) {}
}
