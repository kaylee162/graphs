import apply.Building;
import apply.Intersection;
import apply.StaticWaddleWorks;
import refactor.AdjacencyListGraph;
import refactor.Edge;
import refactor.MutableGraph;
import refactor.Vertex;

import java.util.Set;

/**
 * Entry point for accessing your project 3 files.
 *
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
public class Main {

    /**
     * Creates and returns a new instance of your class extending
     * {@link MutableGraph}.
     *
     * @param vertices the vertex set
     * @param edges the edge set
     * @param <T> the type of vertex data in the graph
     * @return a new {@link MutableGraph} instance
     * @throws IllegalArgumentException if any of the arguments are null, or
     * if the vertex set doesn't contain all the vertices.
     */
    public static <T> MutableGraph<T> getMutableGraphInstance(Set<Vertex<T>> vertices,
                                                              Set<Edge<T>> edges) {
        return new AdjacencyListGraph<>(vertices, edges);
    }

    /**
     * Creates and returns a new instance of your class extending
     * {@link StaticWaddleWorks}.
     *
     * @param roads the initial graph of the road network
     * @param grid the initial graph of the electrical grid network
     * @return a new {@link StaticWaddleWorks} instance
     */
    public static StaticWaddleWorks getWaddleWorksInstance(MutableGraph<Intersection> roads,
                                                           MutableGraph<Building> grid) {
        // Part 3 method. Leave this as a stub until you implement WaddleWorks.
        throw new UnsupportedOperationException("Instantiate your WaddleWorks class here in Part 3.");
    }
}