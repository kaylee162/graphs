package implement;

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
 * Our toolkit:
 * - BFS: shortest path in unweighted graphs (level-by-level exploration, breath-first search)
 * - DFS: full exploration (deep-first search)
 * - Dijkstra (aka D): shortest paths with weights
 * - Prim + Kruskal: build minimum spanning trees
 *
 * Each method is kinda just the textbook algorithms, but they all still respect adjacency list order
 */
public class GraphAlgorithms {

    /**
     * Breadth-First Search (BFS)
     *
     * Explores the graph outward in "layers" from the start node.
     * All nodes at distance 1 are visited before distance 2, etc.
     */
    public static <T> List<Vertex<T>> bfs(Vertex<T> start, StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        List<Vertex<T>> visitedOrder = new ArrayList<>(); // final traversal order
        Set<Vertex<T>> visited = new HashSet<>();         // prevents revisiting
        Queue<Vertex<T>> queue = new ArrayDeque<>();      // FIFO structure

        visited.add(start);   // mark immediately
        queue.add(start);     // begin traversal here

        while (!queue.isEmpty()) {
            Vertex<T> current = queue.remove(); // pull from front of queue
            visitedOrder.add(current);          // record visit

            // Explore neighbors in adjacency-list order (important for grading)
            for (VertexDistance<T> neighbor : graph.getNeighbors(current)) {
                Vertex<T> next = neighbor.vertex();

                if (!visited.contains(next)) {
                    visited.add(next);     // mark BEFORE enqueueing
                    queue.add(next);       // schedule for future processing
                }
            }
        }

        return visitedOrder;
    }

    /**
     * Depth-First Search (DFS)
     *
     * Goes as deep as possible before backtracking.
     * This implementation is recursive
     */
    public static <T> List<Vertex<T>> dfs(Vertex<T> start, StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        List<Vertex<T>> visitedOrder = new ArrayList<>();
        Set<Vertex<T>> visited = new HashSet<>();

        dfs(start, graph, visited, visitedOrder); // kick off recursion :)
        return visitedOrder;
    }

    /**
     * Recursive DFS helper.
     *
     * This is where the actual "detph-first" behavior happens
     * Each call dives deeper into the graph until it can't go further.
     */
    private static <T> void dfs(Vertex<T> curr, StaticGraph<T> g,
                                Set<Vertex<T>> vSet, List<Vertex<T>> list) {

        vSet.add(curr);   // mark visited
        list.add(curr);   // record visit

        // Keep going deeper until no unvisited neighbors remain
        for (VertexDistance<T> neighbor : g.getNeighbors(curr)) {
            Vertex<T> next = neighbor.vertex();

            if (!vSet.contains(next)) {
                dfs(next, g, vSet, list); // recursive descent
            }
        }
    }

    /**
     * D's Algorithm
     *
     * Computes shortest paths from a single source to all other vertices.
     * Works only with non-negative edge weights
     */
    public static <T> Map<Vertex<T>, Integer> dijkstras(Vertex<T> start,
                                                        StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        // This map will store the best-known distance to each vertex from the start.
        Map<Vertex<T>, Integer> distances = new HashMap<>();

        // Initialize all distances to "infinity"
        for (Vertex<T> vertex : graph.getVertices()) {
            distances.put(vertex, Integer.MAX_VALUE);
        }
        distances.put(start, 0); // start is distance 0

        // Min-heap ordered by smallest distance
        PriorityQueue<VertexDistance<T>> pq = new PriorityQueue<>();
        pq.add(new VertexDistance<>(start, 0));

        while (!pq.isEmpty()) {
            VertexDistance<T> currentPair = pq.remove();
            Vertex<T> current = currentPair.vertex();
            int currentDistance = currentPair.distance();

            // Skip outdated entries (classic optimization)
            if (currentDistance > distances.get(current)) {
                continue;
            }

            // Explore neighbors and update paths if better
            for (VertexDistance<T> neighbor : graph.getNeighbors(current)) {
                Vertex<T> next = neighbor.vertex();
                int newDistance = currentDistance + neighbor.distance();

                // Found a better path to next vertex
                if (newDistance < distances.get(next)) {
                    distances.put(next, newDistance);
                    pq.add(new VertexDistance<>(next, newDistance)); // push updated path
                }
            }
        }

        return distances;
    }

    /**
     * Prim's Algorithm (MST)
     *
     * Grows a tree by always picking the cheapest edge that connects
     * a visited node to an unvisited node.
     */
    public static <T> Set<Edge<T>> prims(Vertex<T> start, StaticGraph<T> graph) {
        validateStartAndGraph(start, graph);

        Set<Edge<T>> mst = new LinkedHashSet<>(); // preserves insertion order
        Set<Vertex<T>> visited = new HashSet<>();
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
                continue; // would form a cycle so skip because NO
            }

            mst.add(edge);   // include edge in MST
            visited.add(next);
            addOutgoingEdges(next, graph, visited, pq); // expand frontier
        }

        // If not all vertices were reached → graph is disconnected
        if (visited.size() != graph.getVertexCount()) {
            return null;
        }

        return mst;
    }

    /**
     * Kruskal's Algorithm (MST)
     *
     * Sorts all edges and greedily picks the smallest ones that
     * don't form a cycle
     */
    public static <T> Set<Edge<T>> kruskals(StaticGraph<T> graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null.");
        }

        // Kruskal's algorithm relies on a Disjoint Set to efficiently track
        // which vertices are already connected in the growing forest of trees.
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
                disjointSet.union(edge.u(), edge.v()); // merge components
            }
        }

        // If we didn’t get enough edges, graph wasn’t fully connected
        if (mst.size() != graph.getVertexCount() - 1) {
            return null;
        }

        return mst;
    }

    /**
     * Shared validation helper.
     *
     * Keeps checks consistent across BFS, DFS, Dijkstra, and Prim.
     */
    private static <T> void validateStartAndGraph(Vertex<T> start,
                                                  StaticGraph<T> graph) {
        // Check for null inputs                                            
        if (start == null || graph == null) {
            throw new IllegalArgumentException("Start vertex and graph cannot be null.");
        }

        // Ensure the start actually exists in the graph
        if (!graph.containsVertex(start)) {
            throw new IllegalArgumentException("Start vertex must exist in the graph.");
        }
    }

    /**
     * Helper for Prim's algorithm.
     *
     * Adds all edges leaving a vertex into the priority queue,
     * but only if they go to unvisited nodes.
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
}