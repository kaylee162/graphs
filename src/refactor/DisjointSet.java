package refactor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to store a DisjointSet data structure. This data structure has two
 * main functions: find and union. find will look for the root (parent) of a
 * DisjointSet. Calling find on two different T data will check if those two are
 * part of the same set. union will join two sets together if not already.
 * <p>
 * See the PDF for more information on Disjoint Sets.
 * <p>
 * @author Kaylee Henry
 * @version 1.0
 * @userid khenry61
 * @GTID 904065531
 * <br>
 * <p>
 * Collaborators: None
 * <p>
 * Resources: None
 * <p>
 * <br>
 * By typing 'I agree' below, you are agreeing that this is your
 * own work and that you are responsible for the contents of all
 * submitted files. If this is left blank, this project will lose
 * points.
 *<p>
 *<br>
 * Agree Here: I agree  
 */
public class DisjointSet<T> {

    /**
     * Maps each data item to its node in the union-find forest.
     */
    private final Map<T, DisjointSetNode<T>> disjointSet;

    /**
     * Tracks the current representative/root data values for every disjoint set.
     *
     * <p>This backing set is what allows getRoots() to run in O(1), since the
     * method can simply return an unmodifiable view of this already-maintained
     * set instead of recomputing the roots each time.
     */
    private final Set<T> roots;

    /**
     * Initializes the disjoint sets by instantiating a HashMap.
     */
    public DisjointSet() {
        disjointSet = new HashMap<>();
        roots = new HashSet<>();
    }

    /**
     * Finds the root node of the disjoint set containing {@code data}.
     * Puts the data in the disjoint sets if it does not already exist.
     *
     * @param data the data to search for
     * @return the disjoint set's root data
     * @implNote think about how you can modify this method to ensure
     * the roots set is properly updated.
     */
    public T find(T data) {
        // If this data has never appeared before, create a brand-new singleton set.
        // A brand-new node is its own parent, so it is also the root of its own set.
        if (!disjointSet.containsKey(data)) {
            disjointSet.put(data, new DisjointSetNode<>(data));
            roots.add(data);
        }

        // Find the root node and return the root's data.
        return find(disjointSet.get(data)).getData();
    }

    /**
     * Recursively finds the root of the DisjointSetNode. Performs path
     * compression such that all DisjointSetNodes along the path to the root
     * will all directly point to the root.
     *
     * @param curr the current DisjointSetNode to find the root of
     * @return the root of the current node
     */
    private DisjointSetNode<T> find(DisjointSetNode<T> curr) {
        DisjointSetNode<T> parent = curr.getParent();
        if (parent == curr) {
            return curr;
        } else {
            parent = find(curr.getParent());
            curr.setParent(parent);
            return parent;
        }
    }

    /**
     * Attempts to join the two data into the same set by pointing the parent
     * of one set to the parent of another set.
     *
     * @param first The first data to find the parent of
     * @param second The second data to find the parent of
     */
    public void union(T first, T second) {
        // Ensure both elements exist in the structure.
        // The provided PDF explains that find adds an element if it is not already present.
        find(first);
        find(second);

        union(disjointSet.get(first), disjointSet.get(second));
    }

    /**
     * This is where the work is done for union(). This method finds the
     * roots of both passed in nodes and checks if they are the same root.
     * If not the same root, then the root with the least rank will point
     * to the node with higher rank using merge by rank.
     *
     * @param first The first DisjointSetNode to find the parent of
     * @param second The second DisjointSetNode to find the parent of
     * @implNote think about how you can modify this method to ensure
     * the roots set is properly updated.
     */
    private void union(DisjointSetNode<T> first, DisjointSetNode<T> second) {
        // Find the current roots of each node's set.
        DisjointSetNode<T> firstParent = find(first);
        DisjointSetNode<T> secondParent = find(second);

        // If they already share the same root, they are already in the same set,
        // so there is nothing to merge and the roots set should not change.
        if (firstParent != secondParent) {
            if (firstParent.getRank() < secondParent.getRank()) {
                // firstParent loses root status and becomes a child of secondParent.
                firstParent.setParent(secondParent);
                roots.remove(firstParent.getData());
            } else {
                // secondParent loses root status and becomes a child of firstParent.
                secondParent.setParent(firstParent);
                roots.remove(secondParent.getData());

                // If the ranks were equal, the chosen root's rank increases by 1.
                if (firstParent.getRank() == secondParent.getRank()) {
                    firstParent.setRank(firstParent.getRank() + 1);
                }
            }
        }
    }

    /**
     * Gets the set of all representative vertices (roots) in the disjoint set.
     *
     * @return the set of all roots in the disjoint set.
     * @implSpec
     * <p> {@code O(1)} runtime
     * <p> The returned set must not allow modifications to the underlying graph.
     */
    public Set<T> getRoots() {
        return Collections.unmodifiableSet(roots);
    }
}