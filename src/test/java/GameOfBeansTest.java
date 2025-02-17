import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.assertj.core.api.Assertions;

import java.util.Arrays;

public class GameOfBeansTest {

    @Property(tries = 10000)
    void matchesOldImplementation(@ForAll GameOfBeans.Player firstPlayer,
                                  @ForAll @IntRange(min = 1, max = 10) int depth,
                                  @ForAll("piles") Byte[] piles) {
        byte[] primitivePiles = new byte[piles.length];
        for (int i = 0; i < piles.length; i++) {
            primitivePiles[i] = piles[i];
        }
        GameOfBeansOracle original = new GameOfBeansOracle(firstPlayer, depth, Arrays.copyOf(primitivePiles, primitivePiles.length));
        GameOfBeans current = new GameOfBeans(firstPlayer, depth, Arrays.copyOf(primitivePiles, primitivePiles.length));
        Assertions.assertThat(current.score()).isEqualTo(original.score());
    }

    @Provide
    Arbitrary<Byte[]> piles() {
        return Arbitraries.bytes()
                .between((byte) -100, (byte) 100)
                .filter(aByte -> aByte != 0)
                .array(Byte[].class).ofMinSize(1).ofMaxSize(100);
    }

    static class GameOfBeansOracle {

        public static final int DEFAULT_DEPTH = 10;



        private final GameOfBeans.Player firstPlayer;
        private final int depth;
        private byte[] piles;

        public GameOfBeansOracle(GameOfBeans.Player firstPlayer, byte[] piles) {
            this(firstPlayer, DEFAULT_DEPTH, piles);
        }

        public GameOfBeansOracle(GameOfBeans.Player firstPlayer, int depth, byte[] piles) {
            this.firstPlayer = firstPlayer;
            this.depth = depth;
            this.piles = piles;
        }

        public int score() {
            switch (firstPlayer) {
                case JABA:
                    break;
                case PIETON:
                    short pietonWouldTake = pietonWouldTake(0, piles.length);
                    final int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                    final int pietonSuffix = (byte) pietonWouldTake;
                    piles = Arrays.copyOfRange(piles, pietonPrefix, piles.length - pietonSuffix);
                    break;
                default:
                    throw new IllegalStateException("No firstPlayer");
            }
            //TODO use unidimensional array ~piles.length * min(piles.length, depth) sized
            //there is a pattern whereby each row has -1 length than the prior
            short[][] dp = new short[piles.length + 1][piles.length];
            for (short[] scores : dp) {
                //TODO mimimi first line overwritten below
                Arrays.fill(scores, Short.MIN_VALUE);
            }
            dp[0] = new short[piles.length + 1];
            for (int i = 0; i < piles.length; i++) {
                dp[1][i] = piles[i];
            }

            //TODO consider changing exclusive to inclusive prefix and suffix for readability
            for (int length = 2; length <= piles.length; length++) {
                for (int prefixIndex = 0, suffixIndex = prefixIndex + length; suffixIndex <= piles.length; prefixIndex++, suffixIndex++) {
                    int maxPilesToRemove = Math.min(length, depth);
                    short prefixScore = Short.MIN_VALUE;
                    for (int removedPrefix = 1, totalScorePrefix = 0; removedPrefix <= maxPilesToRemove; removedPrefix++) {
                        totalScorePrefix += piles[prefixIndex + (removedPrefix - 1)];
                        short pietonWouldTake = pietonWouldTake(prefixIndex + removedPrefix, suffixIndex);
                        final int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                        final int pietonSuffix = (byte) pietonWouldTake;
                        final int removedPiles = removedPrefix + pietonPrefix + pietonSuffix;
                        prefixScore = (short) Math.max(prefixScore, dp[length - removedPiles][prefixIndex + removedPrefix + pietonPrefix] + totalScorePrefix);
                    }
                    short suffixScore = Short.MIN_VALUE;
                    for (int removedSuffix = 1, totalScoreSuffix = 0; removedSuffix <= maxPilesToRemove; removedSuffix++) {
                        totalScoreSuffix += piles[suffixIndex - removedSuffix];
                        short pietonWouldTake = pietonWouldTake(prefixIndex, suffixIndex - removedSuffix);
                        final int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                        final int pietonSuffix = (byte) pietonWouldTake;
                        final int removedPiles = removedSuffix + pietonPrefix + pietonSuffix;
                        suffixScore = (short) Math.max(suffixScore, dp[length - removedPiles][prefixIndex + pietonPrefix] + totalScoreSuffix);
                    }
                    dp[length][prefixIndex] = (short) Math.max(prefixScore, suffixScore);
                }
            }
            return dp[piles.length][0];
        }

        /**
         * TODO make me dp
         * @param ignoredPrefix ignore before ignoredPrefix (exclusive)
         * @param ignoredSuffix ignore after ignoredSuffix (exclusive)
         * @return
         */
        private short pietonWouldTake(int ignoredPrefix, int ignoredSuffix) {
            int pilesRemaining = ignoredSuffix - ignoredPrefix;
            int maxPilesToRemove = Math.min(pilesRemaining, depth);
            int totalScorePrefix = Short.MIN_VALUE;
            short maxPrefixRemoved = 0;
            int totalScoreSuffix = Short.MIN_VALUE;
            short maxSuffixRemoved = 0;
            for (int removePrefix = 1, score = 0; removePrefix <= maxPilesToRemove; removePrefix++) {
                score += piles[ignoredPrefix + (removePrefix - 1)];
                if (score > totalScorePrefix) {
                    totalScorePrefix = score;
                    maxPrefixRemoved = (byte) removePrefix;
                }
            }
            for (int removeSuffix = 1, score = 0; removeSuffix <= maxPilesToRemove; removeSuffix++) {
                score += piles[ignoredSuffix - removeSuffix];
                if (score > totalScoreSuffix) {
                    totalScoreSuffix = score;
                    maxSuffixRemoved = (byte) removeSuffix;
                }
            }
            return totalScorePrefix >= totalScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
        }
    }
}
