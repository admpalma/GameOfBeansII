import java.util.Arrays;

public class GameOfBeans {

    public static final int DEFAULT_DEPTH = 10;

    enum Player {JABA, PIETON}

    private Player firstPlayer;
    private final int depth;
    private final byte[] piles;

    public GameOfBeans(Player firstPlayer, byte[] piles) {
        this(firstPlayer, DEFAULT_DEPTH, piles);
    }

    public GameOfBeans(Player firstPlayer, int depth, byte[] piles) {
        this.firstPlayer = firstPlayer;
        this.depth = depth;
        this.piles = piles;
    }

    public int score() {
        int dpLength = piles.length + 1;
        short[][] dp = new short[dpLength][dpLength];
        for (short[] ints : dp) {
            Arrays.fill(ints, Short.MIN_VALUE);
        }
        for (int i = 0; i < dpLength ; i++) {
            dp[i][i] = piles[i];
        }

        for (int i = 0; i < dpLength; i++) {
            for (int j = 1 + i; j < dpLength; j++) {
                int maxPilesToRemove = Math.min(j - i, depth);
                for (int k = 0, totalScorePrefix = 0; k < maxPilesToRemove; k++) {
                    totalScorePrefix += piles[i + k];
                    short pietonWouldTake = pietonWouldTake(i + k, j);
                    int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                    int pietonSuffix = (byte) pietonWouldTake;
                    dp[i][j] = (short) Math.max(dp[i + k + pietonPrefix][j - pietonSuffix], totalScorePrefix);
                }
                for (int k = 0, totalScoreSuffix = 0; k < maxPilesToRemove; k++) {
                    totalScoreSuffix += piles[j - k];
                    short pietonWouldTake = pietonWouldTake(i, j - k);
                    int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                    int pietonSuffix = (byte) pietonWouldTake;
                    dp[j][i] = (short) Math.max(dp[j + pietonPrefix][i - k - pietonSuffix], totalScoreSuffix);
                }
            }
        }

        return dp[0][piles.length];
    }

    private short pietonWouldTake(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        int totalScorePrefix = 0;
        short maxPrefixRemoved = 0;
        int totalScoreSuffix = 0;
        short maxSuffixRemoved = 0;
        for (int i = 0, score = 0; i < maxPilesToRemove; i++) {
            score += piles[ignoredPrefix + i];
            if (score > totalScorePrefix) {
                totalScorePrefix = score;
                maxPrefixRemoved = (byte) i;
            }
        }
        for (int i = 0, score = 0; i < maxPilesToRemove; i++) {
            score += piles[ignoredSuffix - i];
            if (score > totalScoreSuffix) {
                totalScoreSuffix = score;
                maxSuffixRemoved = (byte) i;
            }
        }
        return (short) ((maxPrefixRemoved << Byte.SIZE) + maxSuffixRemoved);
    }
}
