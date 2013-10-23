package edu.berkeley.nlp.assignments;

import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.CounterMap;
import edu.berkeley.nlp.util.Counters;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Stone
 * Date: 10/22/13
 * Time: 10:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class SequenceTagScorer extends POSTaggerTester.MostFrequentTagScorer implements POSTaggerTester.LocalTrigramScorer {
    CounterMap<String, String> bigramToTag = new CounterMap<String, String>();
    CounterMap<String, String> suffixToTag = new CounterMap<String, String>();

    public Counter<String> getLogScoreCounter( POSTaggerTester.LocalTrigramContext localTrigramContext) {
        int position = localTrigramContext.getPosition();
        String word = localTrigramContext.getWords().get(position);
        Counter<String> tagCounter;
        if (wordsToTags.keySet().contains(word)) {
            tagCounter = wordsToTags.getCounter(word);
        } else {
            tagCounter = largestSuffixCounter(word);
        }
        Set<String> allowedFollowingTags = allowedFollowingTags(tagCounter.keySet(), localTrigramContext.getPreviousPreviousTag(), localTrigramContext.getPreviousTag());
        String bigram = makeBigramString(localTrigramContext.getPreviousPreviousTag(), localTrigramContext.getPreviousTag());
        Counter<String> logScoreCounter = new Counter<String>();
        for (String tag : tagCounter.keySet()) {
            double tagProbability = bigramToTag.getCount(bigram, tag);
            double emissionProbability = tagCounter.getCount(tag);
            double logScore = Math.log(tagProbability * emissionProbability);
            if (!restrictTrigrams || allowedFollowingTags.isEmpty() || allowedFollowingTags.contains(tag))
                logScoreCounter.setCount(tag, logScore);
        }
        return logScoreCounter;
    }

    private Counter<String> largestSuffixCounter(String word) {
        for (int i = Math.min(10, word.length()); i >= 0; i--) {
            String suffix = word.substring(word.length() - i);
            if (suffixToTag.keySet().contains(suffix))
                return suffixToTag.getCounter(suffix);
        }
        return unknownWordTags;
    }

    public void train(List<POSTaggerTester.LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
        super.train(labeledLocalTrigramContexts);
        trainBigramToTag(labeledLocalTrigramContexts);
        trainSuffixToTag(10,10);
    }

    private void trainBigramToTag(List<POSTaggerTester.LabeledLocalTrigramContext> labeledLocalTrigramContexts) {
        for (POSTaggerTester.LabeledLocalTrigramContext labeledLocalTrigramContext : labeledLocalTrigramContexts) {
            String bigram = makeBigramString(labeledLocalTrigramContext.getPreviousPreviousTag(), labeledLocalTrigramContext.getPreviousTag());
            bigramToTag.incrementCount(bigram, labeledLocalTrigramContext.getCurrentTag(), 1.0);
        }
        this.bigramToTag = Counters.conditionalNormalize(bigramToTag);
    }

    private void  trainSuffixToTag(int scarceWordCount, int maxSuffixSize) {
        for (String word : wordsToTags.keySet()) {
            Counter<String> tagCounter = wordsToTags.getCounter(word);
            if (tagCounter.totalCount() < scarceWordCount) {
                for (int i = 0; i < Math.min(maxSuffixSize, word.length()); i++) {
                    String suffix = word.substring(word.length() - i);
                    for (String tag : tagCounter.keySet()) {
                        suffixToTag.incrementCount(suffix, tag, 1.0);
                    }
                }
            }
        }
        this.suffixToTag = Counters.conditionalNormalize(suffixToTag);
    }

    public SequenceTagScorer(boolean restrictTrigrams) {
        super(restrictTrigrams);
    }
}
