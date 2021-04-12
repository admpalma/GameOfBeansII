import java.util.Arrays;

public class GameOfBeans {

    enum Player {JABA, PIETON}

    private final Player firstPlayer;
    private final int depth;
    private byte[] piles;

    public GameOfBeans(Player firstPlayer, int depth, byte[] piles) {
        this.firstPlayer = firstPlayer;
        this.depth = depth;
        this.piles = piles;
    }

    public int score() {
        switch (firstPlayer) {
            case JABA:
                break;
            case PIETON:
                byte pietonPlay = pietonPlay(0, piles.length);
                piles = Arrays.copyOfRange(piles, toPrefix(pietonPlay), piles.length - toSuffix(pietonPlay));
                break;
            default:
                throw new IllegalStateException("No firstPlayer");
        }
        //TODO use unidimensional array ~piles.length * min(piles.length, depth) sized
        //there is a pattern whereby each row has -1 length than the prior
        int dpRows = Math.min(piles.length + 1, 2 * depth);
        short[][] dp = new short[dpRows][piles.length];
        dp[0] = new short[piles.length + 1];
        for (int i = 0; i < piles.length; i++) {
            dp[1][i] = piles[i];
        }
        for (int i = 2; i < dpRows; i++) {
            Arrays.fill(dp[i], Short.MIN_VALUE);
        }

        for (int length = 2; length <= piles.length; length++) {
            for (int prefixIndex = 0, suffixIndex = prefixIndex + length; suffixIndex <= piles.length; prefixIndex++, suffixIndex++) {
                int maxPilesToRemove = Math.min(length, depth);
                short score = Short.MIN_VALUE;
                for (int removed = 1, prefixScore = 0, suffixScore = 0; removed <= maxPilesToRemove; removed++) {
                    prefixScore += piles[prefixIndex + (removed - 1)];
                    byte pietonPlay = pietonPlay(prefixIndex + removed, suffixIndex);
                    byte pietonPrefix = toPrefix(pietonPlay);
                    int removedPiles = removed + pietonPrefix + toSuffix(pietonPlay);
                    score = (short) Math.max(score,
                            dp[(length - removedPiles) % dpRows][prefixIndex + removed + pietonPrefix] + prefixScore);

                    suffixScore += piles[suffixIndex - removed];
                    pietonPlay = pietonPlay(prefixIndex, suffixIndex - removed);
                    pietonPrefix = toPrefix(pietonPlay);
                    removedPiles = removed + pietonPrefix + toSuffix(pietonPlay);
                    score = (short) Math.max(score,
                            dp[(length - removedPiles) % dpRows][prefixIndex + pietonPrefix] + suffixScore);
                }
                dp[length % dpRows][prefixIndex] = score;
            }
        }
        return dp[piles.length % dpRows][0];
    }

    private byte toSuffix(byte pietonPlay) {
        return pietonPlay > 0 ? pietonPlay : 0;
    }

    private byte toPrefix(byte pietonPlay) {
        return pietonPlay < 0 ? (byte) -pietonPlay : 0;
    }

    /**
     * TODO make me dp
     *
     * @param ignoredPrefix ignore before ignoredPrefix (exclusive)
     * @param ignoredSuffix ignore after ignoredSuffix (exclusive)
     * @return positive number if removed from suffix, negative otherwise, 0 if none is removed
     */
    private byte pietonPlay(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        short totalScorePrefix = Short.MIN_VALUE;
        byte maxPrefixRemoved = 0;
        short totalScoreSuffix = Short.MIN_VALUE;
        byte maxSuffixRemoved = 0;
        for (short removePrefix = 1, score = 0; removePrefix <= maxPilesToRemove; removePrefix++) {
            score += piles[ignoredPrefix + (removePrefix - 1)];
            if (score > totalScorePrefix) {
                totalScorePrefix = score;
                maxPrefixRemoved = (byte) removePrefix;
            }
        }
        for (short removeSuffix = 1, score = 0; removeSuffix <= maxPilesToRemove; removeSuffix++) {
            score += piles[ignoredSuffix - removeSuffix];
            if (score > totalScoreSuffix) {
                totalScoreSuffix = score;
                maxSuffixRemoved = (byte) removeSuffix;
            }
        }
        return totalScorePrefix >= totalScoreSuffix ? (byte) -maxPrefixRemoved : maxSuffixRemoved;
    }
}
