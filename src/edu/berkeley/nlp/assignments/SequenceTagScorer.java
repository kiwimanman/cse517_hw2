package edu.berkeley.nlp.assignments;

import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.CounterMap;
import edu.berkeley.nlp.util.Counters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Most frequent tag scorer.
 */
public class SequenceTagScorer extends POSTaggerTester.MostFrequentTagScorer implements POSTaggerTester.LocalTrigramScorer {
    Set<String> knownTags                  = new HashSet<String>();
    CounterMap<String, String> bigramToTag = new CounterMap<String, String>();
    CounterMap<String, String> suffixToTag = new CounterMap<String, String>();
    Set<String> smoothedSuffixSet          = new HashSet<String>();
    int scarceWordCount         = 10;
    int maxSuffixSize           = 10;
    int trainingConfidenceCount = 5;
    double suffixSmoothingTheta = 0.08;

    public Counter<String> getLogScoreCounter( POSTaggerTester.LocalTrigramContext localTrigramContext) {
        int position = localTrigramContext.getPosition();
        String word = localTrigramContext.getWords().get(position);
        Counter<String> tagCounter;

        // For words with few occurrences, consider them unknown and use the suffix counts
        tagCounter = wordsToTags.getCounter(word);
        double trainingOccurrences = wordCounts.getCount(word);
        if (trainingOccurrences < trainingConfidenceCount) {
            tagCounter = suffixCounter(word);
        }

        Set<String> allowedFollowingTags = allowedFollowingTags(tagCounter.keySet(), localTrigramContext.getPreviousPreviousTag(), localTrigramContext.getPreviousTag());
        String bigram = makeBigramString(localTrigramContext.getPreviousPreviousTag(), localTrigramContext.getPreviousTag());
        Counter<String> qTagGivenBigram =  bigramToTag.getCounter(bigram);
        // Build a counter for each possible tag
        Counter<String> logScoreCounter = new Counter<String>();

        for (String tag : qTagGivenBigram.keySet()) {
            double tagProbability = qTagGivenBigram.getCount(tag);
            double emissionProbability = tagCounter.getCount(tag);
            double combinedProbability = tagProbability * emissionProbability;
            double logScore = Math.log(combinedProbability);
            if (!restrictTrigrams || allowedFollowingTags.isEmpty() || allowedFollowingTags.contains(tag))
                logScoreCounter.setCount(tag, logScore);
        }
        if (logScoreCounter.size() > knownTags.size())
            throw new RuntimeException("Too many tags in counter.");
        return logScoreCounter;
    }

    private Counter<String> suffixCounter(String word) {
        String largestKnownSuffix = largestSuffixCounter(word);
        if (largestKnownSuffix == null || largestKnownSuffix.length() == 0)
            return  unknownWordTags;
        if (smoothedSuffixSet.contains(largestKnownSuffix))
            return suffixToTag.getCounter(largestKnownSuffix);

        String lastLetter = largestKnownSuffix.substring(largestKnownSuffix.length() - 1);
        if (!smoothedSuffixSet.contains(lastLetter)) {
            suffixToTag.getCounter(lastLetter).normalize();
            smoothedSuffixSet.add(lastLetter);
        }

        // Smooth up to the largest known suffix and return the counter
        for (int i = 2; i <= largestKnownSuffix.length(); i++) {
            String n = largestKnownSuffix.substring(largestKnownSuffix.length() - i);
            String n1 = largestKnownSuffix.substring(largestKnownSuffix.length() - i + 1);
            if (!smoothedSuffixSet.contains(n)) {
                for (String tag : knownTags) {
                    suffixToTag.incrementCount(n, tag, suffixSmoothingTheta * suffixToTag.getCount(n1, tag));
                }
                Counter<String> counterForN = suffixToTag.getCounter(n);
                counterForN.normalize();
                smoothedSuffixSet.add(n);
            }
        }

        return suffixToTag.getCounter(largestKnownSuffix);
    }

    private String largestSuffixCounter(String word) {
        for (int i = Math.min(10, word.length()); i >= 0; i--) {
            String suffix = word.substring(word.length() - i);
            if (suffixToTag.keySet().contains(suffix))
                return suffix;
        }
        return null;
    }

    public void train(List<POSTaggerTester.LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
        super.train(labeledLocalTrigramContexts);
        // Memorize tag set
        for (POSTaggerTester.LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
            knownTags.add(labeledLocalTrigramContext.getCurrentTag());
            knownTags.add(labeledLocalTrigramContext.getPreviousTag());
            knownTags.add(labeledLocalTrigramContext.getPreviousPreviousTag());
        }
        trainBigramToTag(labeledLocalTrigramContexts);
        //trainSuffixToTag(scarceWordCount, maxSuffixSize);
    }

    private void trainBigramToTag(List<POSTaggerTester.LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
        // Count tags
        for (POSTaggerTester.LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
            String bigram = makeBigramString(labeledLocalTrigramContext.getPreviousPreviousTag(), labeledLocalTrigramContext.getPreviousTag());
            bigramToTag.incrementCount(bigram, labeledLocalTrigramContext.getCurrentTag(), 1.0);
        }
        // Add one to all known combinations this smooths the tag emissions
        if (false) {
            for (String firstTag : knownTags) {
                for (String secondTag : knownTags) {
                    for (String thirdTag : knownTags) {
                        String bigram = makeBigramString(firstTag, secondTag);
                        bigramToTag.incrementCount(bigram, thirdTag, 1.0);
                    }
                }
            }
        }
        this.bigramToTag = Counters.conditionalNormalize(bigramToTag);
    }

    private void trainSuffixToTag(int scarceWordCount, int maxSuffixSize) {
        smoothedSuffixSet = new HashSet<String>();
        suffixToTag       = new CounterMap<String, String>();
        for (String word : wordsToTags.keySet()) {
            Counter<String> tagCounter = wordsToTags.getCounter(word);
            if (scarceWordCount < 0 || wordCounts.getCount(word) < scarceWordCount) {
                for (int i = 0; i < Math.min(maxSuffixSize, word.length()); i++) {
                    String suffix = word.substring(word.length() - i);
                    for (String tag : tagCounter.keySet()) {
                        double increment = wordsToTags.getCount(word, tag) * wordCounts.getCount(word);
                        suffixToTag.incrementCount(suffix, tag, increment);
                    }
                }
            }
        }
        this.suffixToTag = Counters.conditionalNormalize(suffixToTag);
    }

    public void validate(List< POSTaggerTester.LabeledLocalTrigramContext> localTrigramContexts) {
        // Keep track of best state discovered
        double best = Double.NEGATIVE_INFINITY;
        int leastMisses = Integer.MAX_VALUE;
        int bestScarceWordCount = scarceWordCount;
        int bestMaxSuffixSize = maxSuffixSize;
        int bestTrainingConfidenceCount = trainingConfidenceCount;
        double bestSuffixSmoothingTheta = suffixSmoothingTheta;

        int spaceIncrement = 4;

        for (int scarceWordCount = -1; scarceWordCount < 20; scarceWordCount += spaceIncrement) {
            for (int maxSuffixSize = 0; maxSuffixSize < 20; maxSuffixSize += spaceIncrement) {
                for (int trainingConfidenceCount = 0; trainingConfidenceCount < 13; trainingConfidenceCount += spaceIncrement) {
                    for (double suffixSmoothingTheta = 0.0; suffixSmoothingTheta < 0.12; suffixSmoothingTheta += spaceIncrement * 0.01) {
                        // Switch to new state
                        this.scarceWordCount = scarceWordCount;
                        this.maxSuffixSize = maxSuffixSize;
                        this.trainingConfidenceCount = trainingConfidenceCount;
                        this.suffixSmoothingTheta = suffixSmoothingTheta;
                        trainSuffixToTag(scarceWordCount, maxSuffixSize);
                        double sum = 0.0;
                        int misses = 0;
                        // Calculate Expectation
                        for (POSTaggerTester.LabeledLocalTrigramContext trigram : localTrigramContexts) {
                            String currentTag = trigram.getCurrentTag();
                            // String predictedTag = getLogScoreCounter(trigram).argMax();
                            double increment = getLogScoreCounter(trigram).getCount(currentTag);
                            if (increment == Double.NEGATIVE_INFINITY) {
                                misses++;
                            } else {
                                sum += increment;
                            }
                        }
                        System.out.println(scarceWordCount + "," + maxSuffixSize + "," + trainingConfidenceCount + "," + suffixSmoothingTheta + "," + misses + "," + sum);
                        // Keeping track of misses but cannot figure out a way to use them here.
                        if (misses < leastMisses || (misses == leastMisses && sum > best)) {
                            best = sum;
                            leastMisses = misses;
                            bestScarceWordCount = scarceWordCount;
                            bestMaxSuffixSize = maxSuffixSize;
                            bestTrainingConfidenceCount = trainingConfidenceCount;
                            bestSuffixSmoothingTheta = suffixSmoothingTheta;
                        }
                    }
                }
            }
        }
        // Assign bests
        this.scarceWordCount         = bestScarceWordCount;
        this.maxSuffixSize           = bestMaxSuffixSize;
        this.trainingConfidenceCount = bestTrainingConfidenceCount;
        this.suffixSmoothingTheta    = bestSuffixSmoothingTheta;
        System.out.println(scarceWordCount + " " + maxSuffixSize + " " + trainingConfidenceCount + " " + suffixSmoothingTheta + " " + leastMisses + " " + best);
        trainSuffixToTag(scarceWordCount, maxSuffixSize);
    }

    public SequenceTagScorer(boolean restrictTrigrams) {
        super(restrictTrigrams);
    }
}
