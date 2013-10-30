package edu.berkeley.nlp.assignments;

import edu.berkeley.nlp.util.*;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.*;



/**
 * @author Keith Stone
 * Implements the Veterbi Algorithm to find the highest scoring path through the trellis.
 */
public class VeterbiDecoder<S> extends POSTaggerTester.TrellisDecoder<S> {
    public List<S> getBestPath(POSTaggerTester.Trellis<S> trellis) {
        Map<S, S> backPointers = new HashMap<S,S>();
        Map<S, Double> maxValuesForStates = new HashMap<S, Double>();

        // Work iterating over possible unresolved states
        // For whatever reason, keeping a next set separate from the open makes more sense to me
        Set<S> openSet = new HashSet<S>();
        Set<S> nextSet = new HashSet<S>();

        // Bootstrap open set and store initial value
        openSet.add(trellis.getStartState());
        maxValuesForStates.put(trellis.getStartState(), 0.0);

        // Stop when the graph has been exhausted
        while (openSet.size() > 0) {
            for (S currentState : openSet) {
                Counter<S> forwardTransitions = trellis.getForwardTransitions(currentState);
                for (S nextState : forwardTransitions.keySet()) {
                    nextSet.add(nextState);

                    // Given each forward transition check to see if this route produces the best value
                    // and update back pointers appropriately.
                    double sum = forwardTransitions.getCount(nextState) + maxValuesForStates.get(currentState);
                    if (!maxValuesForStates.keySet().contains(nextState) || sum > maxValuesForStates.get(nextState)) {
                        backPointers.put(nextState, currentState);
                        maxValuesForStates.put(nextState, sum);
                    }
                }
            }
            // Cycle the sets, again, likely not needed but seems like an easy enough sanity check.
            openSet = nextSet;
            nextSet = new HashSet<S>();
        }

        // The back pointers now contain the max value at any point in the graph, specifically at the end point.
        // Follow the back pointers from the end to the beginning to produce the best set of tags.
        return buildStateFromBackPointers(trellis, backPointers);
    }
}
