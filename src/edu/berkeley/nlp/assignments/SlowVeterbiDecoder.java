package edu.berkeley.nlp.assignments;

import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.FastPriorityQueue;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Stone
 * Date: 10/29/13
 * Time: 1:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class SlowVeterbiDecoder<S> extends POSTaggerTester.TrellisDecoder<S> {
    public List<S> getBestPath(POSTaggerTester.Trellis<S> trellis) {
        Map<S, S> backPointers;
        PriorityQueue<S> openSet;
        S currentState;

        backPointers = new HashMap<S,S>();
        openSet =      new FastPriorityQueue<S>();
        openSet.setPriority(trellis.getStartState(), 0.0);

        do {
            double runningTotal = openSet.getPriority(openSet.getFirst());
            currentState = openSet.removeFirst();
            Counter<S> forwardTransitions = trellis.getForwardTransitions(currentState);
            for (S nextState : forwardTransitions.keySet()) {
                double sum = forwardTransitions.getCount(nextState) + runningTotal;
                backPointers.put(nextState, currentState);
                openSet.setPriority(nextState, sum);
            }
        } while (!currentState.equals(trellis.getEndState()));

        return buildStateFromBackPointers(trellis, backPointers);
    }
}
