package implement;
// lowgurtgeniuenly its time to cook yall 

import refactor.DisjointSet;
import refactor.Edge;
import refactor.StaticGraph;
import refactor.Vertex;
import refactor.VertexDistance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
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
 * 
 * ------------------------------------------------
 * 
 * High-level overview:
 * This class implements the core graph algorithms that are used throughout the project.
 *
 * Our super swag toolkit:
 * - BFS: level-by-level traversal; it can support shortest-path discovery in
 *   unweighted graphs when combined with additional bookkeeping
 * - DFS: depth-first traversal of the vertices reachable from the start vertex
 * - Dijkstra (aka D): shortest paths with weights
 * - Prim + Kruskal: build minimum spanning trees
 *
 * BFS and DFS visit neighbors in adjacency-list order, while D's,
 * Prim's, and Kruskal's use priority-based greedy choices.
 */
public class GraphAlgorithms {

    /**
     * Breadth-First Search (BFS) aka BFFs :)
     *
     * Explores the graph outward in "layers" from the start node.
     *  * Vertices one edge away from the start are explored before vertices
     * two edges away, and so on.
     * The runtime is O(V + E) because each vertex is visited once,
     * and each edge is examined at most once.
     *
     * @param <T> the data type stored in the vertices
     * @param start the starting vertex for the traversal
     * @param graph the graph to traverse
     * @return a list of vertices in the order they are visited
     */
    public static <T> List<Vertex<T>> bfs(Vertex<T> start, StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        List<Vertex<T>> visitedOrder = new ArrayList<>(); // final traversal order
        Set<Vertex<T>> visited = new HashSet<>();         // prevents revisiting
        Queue<Vertex<T>> queue = new ArrayDeque<>();

        visited.add(start);   // mark immediately
        queue.add(start);     // begin traversal YAY

        while (!queue.isEmpty()) {
            Vertex<T> current = queue.remove(); // pull from front of queue
            visitedOrder.add(current);          // record visit in the list

            // Explore neighbors in adjacency-list order 
            // Only enqueue unvisited neighbors to maintain BFS layering and avoid cycles cuz that would be bad
            for (VertexDistance<T> neighbor : graph.getNeighbors(current)) {
                Vertex<T> next = neighbor.vertex();

                if (!visited.contains(next)) {
                    visited.add(next);     // mark BEFORE enqueueing
                    queue.add(next);       // schedule for future exploration
                }
            }
        }

        return visitedOrder;
    }

    /**
     * Depth-First Search (DFS).
     *
     * Goes as deep as possible before backtracking.
     * This implementation is recursive.
     * The runtime is O(V + E) because each vertex is visited once,
     * and each edge is examined at most once.
     *
     * @param <T> the data type stored in the vertices
     * @param start the starting vertex for the traversal
     * @param graph the graph to traverse
     * @return a list of vertices in the order they are visited
     */
    public static <T> List<Vertex<T>> dfs(Vertex<T> start, StaticGraph<T> graph) {
        // first we gotta validate it
        validateStartAndGraph(start, graph);

        List<Vertex<T>> visitedOrder = new ArrayList<>(); // this will store the order we visited nodes in
        
        // this will track which nodes we've already seen so we don't get stuck in a cycle cuz that would be bad
        Set<Vertex<T>> visited = new HashSet<>();

        dfs(start, graph, visited, visitedOrder); // kick off recursion YAY :)
        return visitedOrder;
    }

    /**
     * Recursive DFS helper.
     *
     * This is where the actual depth-first behavior happens.
     * Each call visits one vertex, records it, and recursively explores
     * each unvisited neighbor in adjacency-list order.
     *
     * @param <T> the data type stored in the vertices
     * @param curr the current vertex being explored
     * @param g the graph being traversed
     * @param vSet the set of already visited vertices
     * @param list the traversal order being built
     */
    private static <T> void dfs(Vertex<T> curr, StaticGraph<T> g,
                                Set<Vertex<T>> vSet, List<Vertex<T>> list) {

        vSet.add(curr);   // first mark visited
        list.add(curr);   // and record the visit

        // Keep going deeper until no unvisited neighbors remain
        for (VertexDistance<T> neighbor : g.getNeighbors(curr)) {
            Vertex<T> next = neighbor.vertex();

            // only recurse if we haven't seen this neighbor before to avoid 
            // cycles and infinite recursion (bc that would be bad)
            if (!vSet.contains(next)) {
                dfs(next, g, vSet, list);
            }
        }
    }

    /**
     * D's Algorithm
     * 
     * Computes the shortest-path distances from the start vertex
     * to every reachable vertex in the graph, leaving unreachable
     * vertices at Integer.MAX_VALUE.
     * Assumes all edge weights are non-negative.
     *
     * @param <T> the data type stored in the vertices
     * @param start the starting vertex
     * @param graph the graph on which to run Dijkstra's algorithm
     * @return a map from each vertex to its shortest distance from start
     */
    public static <T> Map<Vertex<T>, Integer> dijkstras(Vertex<T> start,
                                                        StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        // This map will store the best-known distance to each vertex from the start.
        Map<Vertex<T>, Integer> distances = new HashMap<>();

        // Initialize all distances to "infinity" bc we haven't found any paths yet 
        for (Vertex<T> vertex : graph.getVertices()) {
            distances.put(vertex, Integer.MAX_VALUE); // represents "infinity" (aka unreachable)
        }
        distances.put(start, 0); // the start vertex is distance 0 from itself

        // Min-heap ordered by smallest distance bc we always want to explore the closest vertex next
        PriorityQueue<VertexDistance<T>> pq = new PriorityQueue<>();
        pq.add(new VertexDistance<>(start, 0));

        // the strat here: keep exploring the closest vertex until we've exhausted all reachable vertices
        while (!pq.isEmpty()) {
            VertexDistance<T> currentPair = pq.remove();
            Vertex<T> current = currentPair.vertex();
            int currentDistance = currentPair.distance();

            // Skip outdated entries (for optimization ofc)
            if (currentDistance > distances.get(current)) {
                continue;
            }

            // Explore neighbors and update paths if better
            for (VertexDistance<T> neighbor : graph.getNeighbors(current)) {
                Vertex<T> next = neighbor.vertex();
                int newDistance = currentDistance + neighbor.distance();

                // we found a better path to next vertex so update the distance and add to the 
                // priority queue for future exploration
                if (newDistance < distances.get(next)) {
                    distances.put(next, newDistance);
                    pq.add(new VertexDistance<>(next, newDistance)); // add the updated distance 
                }
            }
        }

        return distances;
    }

    /**
     * Prim's algorithm for constructing a minimum spanning tree
     * of a connected graph.
     *
     * Grows an MST by repeatedly choosing the cheapest edge
     * that connects a visited vertex to an unvisited vertex.
     *
     * @param <T> the data type stored in the vertices
     * @param start the starting vertex for Prim's algorithm
     * @param graph the graph on which to build the MST
     * @return the minimum spanning tree as a set of edges, or null if the graph
     *         is disconnected
     */
    public static <T> Set<Edge<T>> prims(Vertex<T> start, StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        Set<Edge<T>> mst = new LinkedHashSet<>(); // this preserves insertion order
        Set<Vertex<T>> visited = new HashSet<>(); // this tracks which vertices are already in the growing MST
        // and this will store the candidate edges to add, ordered by weight
        PriorityQueue<Edge<T>> pq = new PriorityQueue<>(); 

        visited.add(start);
        addOutgoingEdges(start, graph, visited, pq); // seed initial edges

        // Continue until all vertices are included OR no edges remain
        while (!pq.isEmpty() && visited.size() < graph.getVertexCount()) {
            Edge<T> edge = pq.remove(); // smallest edge

            // Determine which vertex is the "next" one to visit (the one we haven't seen yet)
            Vertex<T> next = null;
            boolean uVisited = visited.contains(edge.u()); // check if u is visited
            boolean vVisited = visited.contains(edge.v()); // check if v is visited

            // Only accept edges that cross the cut (one in, one out)
            if (uVisited && !vVisited) {
                next = edge.v();
            } else if (!uVisited && vVisited) {
                next = edge.u();
            } else {
                continue; // would form a cycle so skip it bc no cycles allowed tisk tisk
            }

            mst.add(edge);   // include edge in MST
            visited.add(next);
            addOutgoingEdges(next, graph, visited, pq); // expand frontier with these new edges
        }

        // If not all vertices were reached then graph must be disconnected
        if (visited.size() != graph.getVertexCount()) {
            return null;
        }

        return mst;
    }

    /**
     * Helper for Prim's algorithm.
     *
     * Adds all edges leaving a given vertex to the priority queue,
     * but only if they connect to unvisited vertices.
     *
     * @param <T> the data type stored in the vertices
     * @param vertex the vertex whose outgoing edges are being considered
     * @param graph the graph containing the adjacency information
     * @param visited the set of vertices already included in the MST
     * @param pq the priority queue of candidate edges
     */
    private static <T> void addOutgoingEdges(Vertex<T> vertex, StaticGraph<T> graph,
                                             Set<Vertex<T>> visited,
                                             PriorityQueue<Edge<T>> pq) {

        // Only consider edges that lead to unvisited vertices to avoid cycles                            
        for (VertexDistance<T> neighbor : graph.getNeighbors(vertex)) {
            Vertex<T> next = neighbor.vertex();

            // Skip edges that lead to already visited vertices (bc that would create a cycle and thats bad :( )
            if (!visited.contains(next)) {
                pq.add(new Edge<>(vertex, next, neighbor.distance())); // candidate edge
            }
        }
    }

    /**
     * Kruskal's algorithm for a minimum spanning tree.
     *
     * Processes edges in increasing weight order and greedily adds
     * an edge when it does not create a cycle.
     *
     * @param <T> the data type stored in the vertices
     * @param graph the graph on which to build the MST
     * @return the minimum spanning tree as a set of edges, or null if the graph
     *         is disconnected
     */
    public static <T> Set<Edge<T>> kruskals(StaticGraph<T> graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null.");
        }

        // Kruskal's algorithm uses a Disjoint Set to track which vertices
        // belong to the same connected component in the growing forest,
        // so edges that would create cycles can be skipped.
        Set<Edge<T>> mst = new LinkedHashSet<>();
        PriorityQueue<Edge<T>> pq = new PriorityQueue<>(graph.getEdges());
        DisjointSet<Vertex<T>> disjointSet = new DisjointSet<>();

        // Initialize each vertex as its own component
        for (Vertex<T> vertex : graph.getVertices()) {
            disjointSet.find(vertex);
        }

        // Process edges in increasing weight order
        while (!pq.isEmpty() && mst.size() < graph.getVertexCount() - 1) {
            Edge<T> edge = pq.remove();

            Vertex<T> uRoot = disjointSet.find(edge.u());
            Vertex<T> vRoot = disjointSet.find(edge.v());

            // Only add edge if it connects two different components
            if (!uRoot.equals(vRoot)) {
                mst.add(edge);
                // Merge the components
                disjointSet.union(edge.u(), edge.v()); 
            }
        }

        // If we didn’t get enough edges, then graph wasn’t fully connected
        if (mst.size() != graph.getVertexCount() - 1) {
            return null;
        }

        return mst;
    }

    /**
     * Shared validation helper.
     *
     * Validates that the start vertex and graph are non-null and that
     * the start vertex exists in the graph.
     * 
     * @param <T> the data type stored in the vertices
     * @param start the starting vertex to validate
     * @param graph the graph to validate against
     */
    private static <T> void validateStartAndGraph(Vertex<T> start,
                                                  StaticGraph<T> graph) {
        // Check for null inputs we cant have any of those                                           
        if (start == null || graph == null) {
            throw new IllegalArgumentException("Start vertex and graph cannot be null.");
        }

        // Ensure the start actually exists in the graph, bc if it didn't that'd be bad
        if (!graph.containsVertex(start)) {
            throw new IllegalArgumentException("Start vertex must exist in the graph.");
        }
    }
}