import java.util.Arrays;

public class GameOfBeans {

    enum Player {JABA, PIETON}

    private final Player firstPlayer;
    private final int depth;
    private byte[] piles;
    private int[][] pietonPrefixCache, pietonSuffixCache;

    public GameOfBeans(Player firstPlayer, int depth, byte[] piles) {
        this.firstPlayer = firstPlayer;
        this.depth = depth;
        this.piles = piles;
    }

    public int score() {
		// If Pieton plays first, change the game to a subset where JABA plays first, since Pieton is easy to calc first move
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
        pietonPrefixCache = new int[piles.length][depth];
        pietonSuffixCache = new int[piles.length][depth];
        cachePietonPlays();
        //could use unidimensional array ~piles.length * min(piles.length, depth) sized
        //There is a pattern whereby each row has -1 length than the prior, making a stair type pattern
        int rows = Math.min(piles.length + 1, 2 * depth);
        short[][] score = new short[rows][piles.length];
        score[0] = new short[piles.length + 1];
        for (int i = 0; i < piles.length; i++) {
            score[1][i] = piles[i];
        }
        for (int i = 2; i < rows; i++) {
            Arrays.fill(score[i], Short.MIN_VALUE);
        }

        for (int length = 2; length <= piles.length; length++) {
            for (int prefixIndex = 0, suffixIndex = prefixIndex + length; suffixIndex <= piles.length; prefixIndex++, suffixIndex++) {
                int maxPilesToRemove = Math.min(length, depth);
                short maxScore = Short.MIN_VALUE;
                for (int removed = 1, prefixScore = 0, suffixScore = 0; removed <= maxPilesToRemove; removed++) {
                    prefixScore += piles[prefixIndex + (removed - 1)];
                    byte pietonPlay = pietonPlayCached(prefixIndex + removed, suffixIndex);
                    byte pietonPrefix = toPrefix(pietonPlay);
                    int removedPiles = removed + pietonPrefix + toSuffix(pietonPlay);
                    maxScore = (short) Math.max(maxScore,
                            score[(length - removedPiles) % rows][prefixIndex + removed + pietonPrefix] + prefixScore);

                    suffixScore += piles[suffixIndex - removed];
                    pietonPlay = pietonPlayCached(prefixIndex, suffixIndex - removed);
                    pietonPrefix = toPrefix(pietonPlay);
                    removedPiles = removed + pietonPrefix + toSuffix(pietonPlay);
                    maxScore = (short) Math.max(maxScore,
                            score[(length - removedPiles) % rows][prefixIndex + pietonPrefix] + suffixScore);
                }
                score[length % rows][prefixIndex] = maxScore;
            }
        }
        return score[piles.length % rows][0];
    }

    private byte toSuffix(byte pietonPlay) {
        return pietonPlay > 0 ? pietonPlay : 0;
    }

    private byte toPrefix(byte pietonPlay) {
        return pietonPlay < 0 ? (byte) -pietonPlay : 0;
    }

    private void cachePietonPlays() {
        int maxPilesToRemove = Math.min(piles.length, depth);
        for (int suffixIndex = 0; suffixIndex < maxPilesToRemove; suffixIndex++) {
            cachePietonSuffixPlay(0, suffixIndex);
        }
        for (int prefixIndex = 0, suffixIndex = prefixIndex + maxPilesToRemove; suffixIndex <= piles.length; prefixIndex++, suffixIndex++) {
            cachePietonPrefixPlay(prefixIndex, suffixIndex);
            cachePietonSuffixPlay(prefixIndex, suffixIndex);
        }
        for (int prefixIndex = piles.length - maxPilesToRemove; prefixIndex <= piles.length; prefixIndex++) {
            cachePietonPrefixPlay(prefixIndex, piles.length);
        }
    }

    /**
     * Retrieves pieton plays from cache
     *
     * @param ignoredPrefix ignore before ignoredPrefix (exclusive)
     * @param ignoredSuffix ignore after ignoredSuffix (exclusive)
     * @return positive number if removed from suffix, negative otherwise, 0 if none is removed
     */
    private byte pietonPlayCached(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        if (maxPilesToRemove == 0) {
            return 0;
        }
        int prefixPlay = pietonPrefixCache[ignoredPrefix][maxPilesToRemove - 1];
        int suffixPlay = pietonSuffixCache[ignoredSuffix - 1][maxPilesToRemove - 1];
        return (byte) (toScore(prefixPlay) >= toScore(suffixPlay) ? -prefixPlay : suffixPlay);
    }


	// Its preferable to spend a little memory on two functions to not have branching if statements here
    private void cachePietonPrefixPlay(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        short totalScorePrefix = Short.MIN_VALUE;
        byte maxPrefixRemoved = 0;
        for (short removePrefix = 1, score = 0; removePrefix <= maxPilesToRemove; removePrefix++) {
            score += piles[ignoredPrefix + (removePrefix - 1)];
            if (score > totalScorePrefix) {
                totalScorePrefix = score;
                maxPrefixRemoved = (byte) removePrefix;
            }
            pietonPrefixCache[ignoredPrefix][removePrefix - 1] = toCacheFormat(totalScorePrefix, maxPrefixRemoved);
        }
    }

    private void cachePietonSuffixPlay(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        short totalScoreSuffix = Short.MIN_VALUE;
        byte maxSuffixRemoved = 0;
        for (short removeSuffix = 1, score = 0; removeSuffix <= maxPilesToRemove; removeSuffix++) {
            score += piles[ignoredSuffix - removeSuffix];
            if (score > totalScoreSuffix) {
                totalScoreSuffix = score;
                maxSuffixRemoved = (byte) removeSuffix;
            }
            pietonSuffixCache[ignoredSuffix - 1][removeSuffix - 1] = toCacheFormat(totalScoreSuffix, maxSuffixRemoved);
        }
    }

    private short toScore(int pietonCacheEntry) {
        return (short) (pietonCacheEntry >> Short.SIZE);
    }

    /**
     * Encodes the given score on an int with the following pattern: 0xff00
     * and the number of removed piles with the following pattern: 0x000f
     * (independent of prefix vs suffix play)
     * @param score this Pieton play score
     * @param removed this Pieton play removed piles
     * @return encoded Pieton play
     */
    private int toCacheFormat(short score, byte removed) {
        return (score << Short.SIZE) + removed;
    }

    /**
     *
     * @param ignoredPrefix ignore before ignoredPrefix (exclusive)
     * @param ignoredSuffix ignore after ignoredSuffix (exclusive)
     * @return positive number if removed from suffix, negative otherwise, 0 if none is removed
     */
    private byte pietonPlay(int ignoredPrefix, int ignoredSuffix) {
        int prefixPlay = pietonPrefixPlay(ignoredPrefix, ignoredSuffix);
        int suffixPlay = pietonSuffixPlay(ignoredPrefix, ignoredSuffix);
        return (byte) (toScore(prefixPlay) >= toScore(suffixPlay) ? -(byte) prefixPlay : suffixPlay);
    }

    private int pietonPrefixPlay(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        short totalScorePrefix = Short.MIN_VALUE;
        byte maxPrefixRemoved = 0;
        for (short removePrefix = 1, score = 0; removePrefix <= maxPilesToRemove; removePrefix++) {
            score += piles[ignoredPrefix + (removePrefix - 1)];
            if (score > totalScorePrefix) {
                totalScorePrefix = score;
                maxPrefixRemoved = (byte) removePrefix;
            }
        }
        return toCacheFormat(totalScorePrefix, maxPrefixRemoved);
    }

    private int pietonSuffixPlay(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        int maxPilesToRemove = Math.min(pilesRemaining, depth);
        short totalScoreSuffix = Short.MIN_VALUE;
        byte maxSuffixRemoved = 0;
        for (short removeSuffix = 1, score = 0; removeSuffix <= maxPilesToRemove; removeSuffix++) {
            score += piles[ignoredSuffix - removeSuffix];
            if (score > totalScoreSuffix) {
                totalScoreSuffix = score;
                maxSuffixRemoved = (byte) removeSuffix;
            }
        }
        return toCacheFormat(totalScoreSuffix, maxSuffixRemoved);
    }
}
