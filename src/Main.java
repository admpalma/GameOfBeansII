import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
		TurboScanner in = new TurboScanner(System.in);
		int testCases = in.nextInt();
		for (int i = 0;i<testCases;i++){
			int pileN = in.nextInt();
			byte[] piles = new byte[pileN];
			int depth = in.nextInt();
			for (int p = 0;p<pileN;p++){
			    piles[p] = (byte) in.nextInt();
			}
			GameOfBeans.Player p = in.next().equals("Jaba") ?GameOfBeans.Player.JABA:GameOfBeans.Player.PIETON;
			GameOfBeans gb = new GameOfBeans(p,depth,piles);
			System.out.println(gb.score());
		}
    }
}
