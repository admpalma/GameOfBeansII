import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class GameOfBeans {

    public static final int DEFAULT_DEPTH = 10;

    enum Player {JABA, PIETON}

    private final Player firstPlayer;
    private final int depth;
    private byte[] piles;

    public GameOfBeans(Player firstPlayer, byte[] piles) {
        this(firstPlayer, DEFAULT_DEPTH, piles);
    }

    public GameOfBeans(Player firstPlayer, int depth, byte[] piles) {
        this.firstPlayer = firstPlayer;
        this.depth = depth;
        this.piles = piles;
    }

    private short[][] pieton;

    public int score() {
        switch (firstPlayer) {
            case JABA:
                break;
            case PIETON:
                short pietonWouldTake = pietonWouldTake(0, piles.length);
                final int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                final int pietonSuffix = (byte) pietonWouldTake;
                piles = Arrays.copyOfRange(piles, pietonPrefix, piles.length - pietonSuffix);
                if (piles.length == 0) {
                    return 0;
                }
                break;
            default:
                throw new IllegalStateException("No firstPlayer");
        }
        pieton = pietonKms();
        //TODO use unidimensional array ~piles.length * min(piles.length, depth) sized
        //there is a pattern whereby each row has -1 length than the prior
        int dpRows = Math.min(piles.length + 1, 2 * depth);
        short[][] dp = new short[dpRows][piles.length];
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
                    short pietonWouldTake = pietonWouldTake2(prefixIndex + removedPrefix, suffixIndex);
                    final int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                    final int pietonSuffix = (byte) pietonWouldTake;
                    final int removedPiles = removedPrefix + pietonPrefix + pietonSuffix;
                    prefixScore = (short) Math.max(prefixScore, dp[(length - removedPiles) % dpRows][prefixIndex + removedPrefix + pietonPrefix] + totalScorePrefix);
                }
                short suffixScore = Short.MIN_VALUE;
                for (int removedSuffix = 1, totalScoreSuffix = 0; removedSuffix <= maxPilesToRemove; removedSuffix++) {
                    totalScoreSuffix += piles[suffixIndex - removedSuffix];
                    short pietonWouldTake = pietonWouldTake2(prefixIndex, suffixIndex - removedSuffix);
                    final int pietonPrefix = pietonWouldTake >>> Byte.SIZE;
                    final int pietonSuffix = (byte) pietonWouldTake;
                    final int removedPiles = removedSuffix + pietonPrefix + pietonSuffix;
                    suffixScore = (short) Math.max(suffixScore, dp[(length - removedPiles) % dpRows][prefixIndex + pietonPrefix] + totalScoreSuffix);
                }
                dp[length % dpRows][prefixIndex] = (short) Math.max(prefixScore, suffixScore);
            }
        }
        return dp[piles.length % dpRows][0];
    }

    private short[][] pietonUgh() {
        int dpRows = Math.min(piles.length, depth) + 1;
        short[][] dp = new short[piles.length + 1][piles.length];
        short[][] dp2 = new short[piles.length + 1][piles.length];
        Arrays.fill(dp[1], (short) (1 << Byte.SIZE));
        Arrays.fill(dp2[1], (short) 1);
        for (int length = 2; length <= piles.length; length++) {
            int totalScorePrefix = Short.MIN_VALUE;
            short maxPrefixRemoved = 1;
            int totalScoreSuffix = Short.MIN_VALUE;
            short maxSuffixRemoved = 1;

            int prefixIndex = 0, suffixIndex = prefixIndex + length;
            int remove = 1, prefixScore = 0, suffixScore = 0;
            for (; remove <= Math.min(length, depth); remove++) {
                prefixScore += piles[prefixIndex + (remove - 1)];
                if (prefixScore > totalScorePrefix) {
                    totalScorePrefix = prefixScore;
                    maxPrefixRemoved = (byte) remove;
                }
                suffixScore += piles[suffixIndex - remove];
                if (suffixScore > totalScoreSuffix) {
                    totalScoreSuffix = suffixScore;
                    maxSuffixRemoved = (byte) remove;
                }
            }
            dp[length][prefixIndex] = totalScorePrefix >= totalScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
            dp2[length][prefixIndex] = totalScorePrefix < totalScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
            remove--;
            prefixIndex++;
            suffixIndex++;
            int suffixBacktracks = 0, prefixIgnored = 0;
            for (; suffixIndex <= piles.length; prefixIndex++, suffixIndex++) {
                byte addedPrefixScore = piles[prefixIndex + (remove - 1)];
                byte removedPrefixScore = piles[prefixIndex - 1];
                prefixScore -= removedPrefixScore;
                prefixScore += addedPrefixScore;

                if (remove == 1) {
                    totalScorePrefix = prefixScore;
                    maxPrefixRemoved = 1;
                } else {
                    //TODO tem potencial para ser melhor do que a anterior
                    if (prefixScore >= totalScorePrefix - removedPrefixScore) {
                        totalScorePrefix = prefixScore;
                        maxPrefixRemoved = (byte) remove;
                    } else if (length > depth) {
                        totalScorePrefix -= removedPrefixScore; //TODO haha have fun linear ou smartass shit
                        maxPrefixRemoved = (byte) ((dp[length - 1][prefixIndex] | dp2[length - 1][prefixIndex]) >>> Byte.SIZE);
                    } else {

                    }
                }


                byte addedSuffixScore = piles[suffixIndex - 1];
                byte removedSuffixScore = 0;
                if (suffixBacktracks == 0) {
                    removedSuffixScore = piles[suffixIndex - remove - 1];
                    suffixScore -= removedSuffixScore;
                } else {
                    suffixBacktracks--;
                }
                suffixScore += addedSuffixScore;
                for (; piles[suffixIndex - remove + suffixBacktracks] < 0 && suffixBacktracks < remove - 1; suffixBacktracks++) {
                    //we can backtrack further
                    removedSuffixScore += (int) piles[suffixIndex - remove + suffixBacktracks];
                    suffixScore -= piles[suffixIndex - remove + suffixBacktracks];
                }

                if (remove == 1) {
                    totalScoreSuffix = suffixScore;
                    maxSuffixRemoved = 1;
                } else {
                    //TODO tem potencial para ser melhor do que a anterior
                    if (suffixScore >= totalScoreSuffix + addedSuffixScore) {
                        totalScoreSuffix = suffixScore;
                        maxSuffixRemoved = (byte) (remove - suffixBacktracks);
                    } else {
                        totalScoreSuffix -= removedSuffixScore; //TODO haha have fun linear ou smartass shit
                        totalScoreSuffix += addedSuffixScore;
                        maxSuffixRemoved = (byte) (dp[length - 1][prefixIndex] | dp2[length - 1][prefixIndex]);
                    }
                }

                dp[length][prefixIndex] = totalScorePrefix >= totalScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
                dp2[length][prefixIndex] = totalScorePrefix < totalScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
            }
        }
        return dp;
    }

    private short[][] pietonKms() {
        int dpRows = Math.min(piles.length, depth) + 1;
        short[][] dp = new short[piles.length + 1][piles.length];
        short[][] dp2 = new short[piles.length + 1][piles.length];
        Arrays.fill(dp[1], (short) (1 << Byte.SIZE));
        Arrays.fill(dp2[1], (short) 1);
        Deque<Integer> prefixSubsums = new ArrayDeque<>(depth);
        Deque<Integer> suffixSubsums = new ArrayDeque<>(depth);

        for (int length = 2; length <= piles.length; length++) {
            int usedScorePrefix = Short.MIN_VALUE;
            short maxPrefixRemoved = 1;
            int usedScoreSuffix = Short.MIN_VALUE;
            short maxSuffixRemoved = 1;

            int prefixIndex = 0, suffixIndex = prefixIndex + length;
            int remove = 1, prefixScore = 0, suffixScore = 0;
            int maxWindow = Math.min(length, depth);
            boolean finalizedSuffix = false;
            suffixSubsums.addFirst(0);
            for (; remove <= maxWindow; remove++) {
                prefixScore += piles[prefixIndex + (remove - 1)];
                if (prefixSubsums.isEmpty() || (piles[prefixIndex + (remove - 1)] < 0 && piles[prefixIndex + (remove - 2)] > 0)) {
                    prefixSubsums.addLast((piles[prefixIndex + (remove - 1)] << Short.SIZE) + 1);
                } else {
                    int lastSubsum = prefixSubsums.pollLast();
                    int subsumValue = lastSubsum >> Short.SIZE;
                    int subsumNum = (short) lastSubsum;
                    prefixSubsums.addLast(((subsumValue + piles[prefixIndex + (remove - 1)]) << Short.SIZE) + (subsumNum + 1));
                }
                if (prefixScore > usedScorePrefix) {
                    usedScorePrefix = prefixScore;
                    maxPrefixRemoved = (byte) remove;
                    prefixSubsums.clear();
                }
                suffixScore += piles[suffixIndex - remove];
                if (finalizedSuffix) {
                    suffixSubsums.addFirst((piles[suffixIndex - remove] << Short.SIZE) + 1);
                } else {
                    int lastSubsum = suffixSubsums.pollFirst();
                    int subsumValue = lastSubsum >> Short.SIZE;
                    int subsumNum = (short) lastSubsum;
                    suffixSubsums.addFirst(((subsumValue + piles[suffixIndex - remove]) << Short.SIZE) + (subsumNum + 1));
                }
                finalizedSuffix = piles[suffixIndex - remove] > 0;
                if (suffixScore > usedScoreSuffix) {
                    usedScoreSuffix = suffixScore;
                    maxSuffixRemoved = (byte) remove;
                }
            }
            int ignoredIndexSuffix = maxWindow - maxSuffixRemoved;
            for (int ignoredIndexes = 0; ignoredIndexes < ignoredIndexSuffix; ) {
                int lastSubsum = suffixSubsums.pollFirst();
                ignoredIndexes += (short) lastSubsum;
            }
            if (suffixSubsums.isEmpty()) {
                suffixSubsums.addFirst((piles[suffixIndex - 1] << Short.SIZE) + 1);
            }
            dp[length][prefixIndex] = usedScorePrefix >= usedScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
            dp2[length][prefixIndex] = usedScorePrefix < usedScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;

            int ignoredIndexPrefix = maxWindow - maxPrefixRemoved;
            int ignoredScorePrefix = prefixScore - usedScorePrefix;
            int ignoredScoreSuffix = suffixScore - usedScoreSuffix;
            prefixIndex++;
            suffixIndex++;
            for (; suffixIndex <= piles.length; prefixIndex++, suffixIndex++) {

                byte addedPrefixScore = piles[prefixIndex + (maxWindow - 1)];
                byte removedPrefixScore = piles[prefixIndex - 1];

                if (ignoredIndexPrefix > 0) {
                    assert !prefixSubsums.isEmpty();
                    if (addedPrefixScore < 0) {//check finalize
                        int lastSubsum = prefixSubsums.pollLast();
                        int subsumValue = lastSubsum >> Short.SIZE;
                        int subsumNum = (short) lastSubsum;
                        ignoredIndexPrefix++;
                        maxPrefixRemoved--;
                        if (subsumValue < 0) {
                            prefixSubsums.addLast(((subsumValue + addedPrefixScore) << Short.SIZE) + (subsumNum + 1));
                        } else {
                            prefixSubsums.addLast((subsumValue << Short.SIZE) + subsumNum);
                            prefixSubsums.addLast((addedPrefixScore << Short.SIZE) + 1);
                        }
                        if (maxPrefixRemoved < 1) {
                            assert !prefixSubsums.isEmpty();
                            int lastSubsum2 = prefixSubsums.pollFirst();
                            int subsumValue2 = lastSubsum2 >> Short.SIZE;
                            int subsumNum2 = (short) lastSubsum2;

                            if (subsumValue2 < 0) {
                                if (maxWindow > 1 && subsumNum2 > 1) {
                                    usedScorePrefix += piles[prefixIndex];
                                    maxPrefixRemoved++;
                                    ignoredIndexPrefix--;
                                    prefixSubsums.addFirst(((subsumValue - piles[prefixIndex]) << Short.SIZE) + (subsumNum - 1));
                                } else {
                                    usedScorePrefix += subsumValue2;
                                    maxPrefixRemoved += subsumNum2;
                                    ignoredIndexPrefix -= subsumNum2;
                                }
                            } else if (subsumNum2 == 0) {
//TODO concatenar
                                int s = 2;
                                s++;
                            } else {
                                usedScorePrefix += subsumValue2;
                                maxPrefixRemoved += subsumNum2;
                                ignoredIndexPrefix -= subsumNum2;
                            }
                        }
                    } else {
                        int lastSubsum = prefixSubsums.pollLast();
                        int subsumValue = lastSubsum >> Short.SIZE;
                        int subsumNum = (short) lastSubsum;
                        ignoredIndexPrefix++;
                        maxPrefixRemoved--;
                        prefixSubsums.addLast(((subsumValue + addedPrefixScore) << Short.SIZE) + (subsumNum + 1));
                        if (maxPrefixRemoved < 1) {
                            assert !prefixSubsums.isEmpty();
                            int lastSubsum2 = prefixSubsums.pollFirst();
                            int subsumValue2 = lastSubsum2 >> Short.SIZE;
                            int subsumNum2 = (short) lastSubsum2;
                            usedScorePrefix += subsumValue2;
                            maxPrefixRemoved += subsumNum2;
                            ignoredIndexPrefix -= subsumNum2;
                        }
                    }
                } else {
                    if (addedPrefixScore < 0 && maxWindow > 1) {
                        ignoredIndexPrefix++;
                        maxPrefixRemoved--;
                        prefixSubsums.addLast((addedPrefixScore << Short.SIZE) + 1);
                    } else {
                        usedScorePrefix += addedPrefixScore;
                    }
                }
                usedScorePrefix -= removedPrefixScore;



//                if (prefixSubsums.isEmpty()) {
//                    if (maxWindow == 1 || addedPrefixScore > 0) {
//                        usedScorePrefix += addedPrefixScore;
//                    } else {
//                        ignoredScorePrefix += addedPrefixScore;
//                        prefixSubsums.addFirst((addedPrefixScore << Short.SIZE) + 1);
//                        maxPrefixRemoved--;
//                    }
//                    usedScorePrefix -= removedPrefixScore;
//                } else {
//                    if (addedPrefixScore < 0 && (prefixSubsums.peekLast() >> Short.SIZE) > 0) {
//                        prefixSubsums.addLast((addedPrefixScore << Short.SIZE) + 1);
//                    } else {
//                        int lastSubsum = prefixSubsums.pollLast();
//                        int subsumValue = lastSubsum >> Short.SIZE;
//                        int subsumNum = (short) lastSubsum;
//                        prefixSubsums.addLast(((subsumValue + addedPrefixScore) << Short.SIZE) + (subsumNum + 1));
//                    }
//                    assert ignoredScorePrefix <= 0;
//                    ignoredScorePrefix += addedPrefixScore;
//
//                    if (ignoredScorePrefix > 0 || (maxPrefixRemoved == 1 && ignoredScorePrefix == 0)) {
//                        usedScorePrefix += ignoredScorePrefix;
//                        maxPrefixRemoved = (short) maxWindow;
//                        ignoredScorePrefix = 0;
//                        prefixSubsums.clear();
//                    } else if (maxPrefixRemoved == 1) {
//                        int lastSubsum = prefixSubsums.pollFirst();
//                        int subsumValue = lastSubsum >> Short.SIZE;
//                        int subsumNum = (short) lastSubsum;
//                        if (subsumValue > piles[prefixIndex]) {
//                            ignoredScorePrefix -= (subsumValue - piles[prefixIndex]);
//                            assert ignoredScorePrefix <= 0;
//                            usedScorePrefix += subsumValue;
//                            maxPrefixRemoved = (short) subsumNum;
//                        } else {
//                            ignoredScorePrefix -= piles[prefixIndex];
//                            if (ignoredScorePrefix <= 0) {
//                                usedScorePrefix += piles[prefixIndex];
//                                if (subsumNum > 1) {
//                                    prefixSubsums.addFirst(((subsumValue - piles[prefixIndex]) << Short.SIZE) + (subsumNum - 1));
//                                }
//                            } else {
//                                usedScorePrefix += piles[prefixIndex];
//                                usedScorePrefix += ignoredScorePrefix;
//                                ignoredScorePrefix = 0;
//                                maxPrefixRemoved = (short) maxWindow;
//                                prefixSubsums.clear();
//                            }
//                        }
//                    } else {
//                        maxPrefixRemoved--;
//                    }
//                    usedScorePrefix -= removedPrefixScore;
//                }

                byte addedSuffixScore = piles[suffixIndex - 1];
                byte removedSuffixScore = piles[suffixIndex - maxWindow - 1];
                if (ignoredIndexSuffix > 0) {
                    assert !suffixSubsums.isEmpty();
                    int lastSubsum = suffixSubsums.pollFirst();
                    int subsumValue = lastSubsum >> Short.SIZE;
                    int subsumNum = (short) lastSubsum;
                    if (subsumValue <= 0) {
                        //assert suffixSubsums.isEmpty();
                        usedScoreSuffix -= subsumValue;
                        maxSuffixRemoved -= subsumNum;
                        ignoredIndexSuffix += subsumNum;
                        suffixSubsums.addLast((addedSuffixScore << Short.SIZE) + 1);
                    } else {
                        if (addedSuffixScore > 0) {
                            suffixSubsums.addFirst((subsumValue << Short.SIZE) + subsumNum);
                            suffixSubsums.addLast((addedSuffixScore << Short.SIZE) + 1);
                        } else if (!suffixSubsums.isEmpty()) {
                            int lastSubsum2 = suffixSubsums.pollLast();
                            int subsumValue2 = lastSubsum2 >> Short.SIZE;
                            int subsumNum2 = (short) lastSubsum2;
                            suffixSubsums.addFirst((subsumValue << Short.SIZE) + subsumNum);
                            suffixSubsums.addLast(((subsumValue2 + addedSuffixScore) << Short.SIZE) + (subsumNum2 + 1));
                        } else {
                            suffixSubsums.addFirst(((subsumValue + addedSuffixScore) << Short.SIZE) + (subsumNum + 1));
                        }
                    }
                } else {
                    assert !suffixSubsums.isEmpty();
                    int lastSubsum = suffixSubsums.pollFirst();
                    int subsumValue = lastSubsum >> Short.SIZE;
                    int subsumNum = (short) lastSubsum;

                    usedScoreSuffix -= subsumValue;
                    maxSuffixRemoved -= subsumNum;
                    ignoredIndexSuffix += subsumNum;

                    if (addedSuffixScore > 0) {
                        suffixSubsums.addLast((addedSuffixScore << Short.SIZE) + 1);
                    } else if (!suffixSubsums.isEmpty()) {
                        int lastSubsum2 = suffixSubsums.pollLast();
                        int subsumValue2 = lastSubsum2 >> Short.SIZE;
                        int subsumNum2 = (short) lastSubsum2;
                        suffixSubsums.addLast(((subsumValue2 + addedSuffixScore) << Short.SIZE) + (subsumNum2 + 1));
                    } else {
                        suffixSubsums.addFirst((addedSuffixScore << Short.SIZE) + 1);
                    }
                }
                ignoredIndexSuffix--;
                maxSuffixRemoved++;
                usedScoreSuffix += addedSuffixScore;

                dp[length][prefixIndex] = usedScorePrefix >= usedScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
                dp2[length][prefixIndex] = usedScorePrefix < usedScoreSuffix ? (short) (maxPrefixRemoved << Byte.SIZE) : maxSuffixRemoved;
            }
            prefixSubsums.clear();
            suffixSubsums.clear();
        }
        return dp;
    }

    private short pietonWouldTake2(int ignoredPrefix, int ignoredSuffix) {
        int pilesRemaining = ignoredSuffix - ignoredPrefix;
        return pieton[pilesRemaining][Math.min(ignoredPrefix, piles.length - 1)];
    }

    /**
     * TODO make me dp
     *
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
