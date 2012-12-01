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
		switch(newState.getStatus()) {
		case PLAYER1WON:
			System.out.println("1");
			break;
		case PLAYER2WON:
			System.out.println("2");
			break;
		case TIE:
			System.out.println("0");
			break;
		}
	}

	@Override
	public void log(String message, Object source) {}
}
