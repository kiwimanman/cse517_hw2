package edu.berkeley.nlp.assignments;

import edu.berkeley.nlp.util.*;
import edu.berkeley.nlp.util.PriorityQueue;

import java.util.*;



/**
 * @author Keith Stone
 * Implements the Veterbi Algorithm to find the highest scoring path through the trellis.
 * Runs in O(n*T^3) as shown below where n is the length of the trellis and T is the size of the tag set
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
        // O(n) as this triggers a stepping process
        while (openSet.size() > 0) {
            // O(T * T) as the open set grows to be at most |TagSet| squared
            // in fact if the tag set is smoothed as well as emission except near the beginning and end
            // the open set should be T*T
            for (S currentState : openSet) {
                Counter<S> forwardTransitions = trellis.getForwardTransitions(currentState);
                // O(T) as we must consider each possible transition to the next tag
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
            if (nextSet.size() > Math.pow(47.0, 2.0))
                throw new RuntimeException("Size of the next set is too large");
            openSet = nextSet;
            nextSet = new HashSet<S>();
        }

        // The back pointers now contain the max value at any point in the graph, specifically at the end point.
        // Follow the back pointers from the end to the beginning to produce the best set of tags.
        return buildStateFromBackPointers(trellis, backPointers);
    }
}
