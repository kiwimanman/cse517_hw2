package edu.berkeley.nlp.assignments;

import edu.berkeley.nlp.util.*;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.*;



/**
 * Created with IntelliJ IDEA.
 * User: Stone
 * Date: 10/23/13
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class BadVeterbiDecoder<S> implements POSTaggerTester.TrellisDecoder<S> {
    public List<S> getBestPath(POSTaggerTester.Trellis<S> trellis) {
        Map<S, S> backPointers = new HashMap<S,S>();
        PriorityQueue<S> maxValuesForStates = new FastPriorityQueue<S>();

        Set<S> openSet = new HashSet<S>();
        Set<S> nextSet = new HashSet<S>();
        openSet.add(trellis.getStartState());
        maxValuesForStates.setPriority(trellis.getStartState(), 0.0);
        while (!openSet.contains(trellis.getEndState())) {
            for (S currentState : openSet) {
                Counter<S> forwardTransitions = trellis.getForwardTransitions(currentState);
                for (S nextState : forwardTransitions.keySet()) {
                    nextSet.add(nextState);
                    double sum = forwardTransitions.getCount(nextState) + maxValuesForStates.getPriority(currentState);
                    if (sum > maxValuesForStates.getPriority(nextState)) {
                        backPointers.put(nextState, currentState);
                        maxValuesForStates.setPriority(nextState, sum);
                    }
                }
            }
            openSet = nextSet;
            nextSet = new HashSet<S>();
        }


        List<S> states = new ArrayList<S>();
        for (S stateIterator = trellis.getEndState(); stateIterator != null; stateIterator = backPointers.get(stateIterator)) {
            states.add(0, stateIterator);
        }
        return states;
    }
}
