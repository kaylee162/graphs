package refactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 */

/**
 * Concrete mutable graph implementation backed by an adjacency list.
 *
 * <p>This graph is:
 * <ul>
 *     <li>Simple: no self-loops, and at most one edge between a pair of vertices</li>
 *     <li>Weighted: each edge stores an integer weight</li>
 *     <li>Undirected: every edge appears in both endpoint neighbor lists</li>
 * </ul>
 *
 * <p>The inherited {@code vertices} and {@code edges} sets come from
 * {@link MutableGraph}. This class adds the adjacency-list representation
 * required by the project.
 *
 * @param <T> the data type stored inside each vertex
 */
public class AdjacencyListGraph<T> extends MutableGraph<T> {

    /**
     * Adjacency-list representation of the graph.
     *
     * <p>Each vertex maps to a list of neighboring vertices paired with the
     * weight of the edge connecting them.
     *
     * <p>Every vertex in the graph must appear as a key in this map, even if
     * it has no edges and therefore has an empty neighbor list.
     */
    private final Map<Vertex<T>, List<VertexDistance<T>>> adjList;

    /**
     * Constructs a new mutable graph from the given vertex and edge sets.
     *
     * <p>This constructor validates that:
     * <ul>
     *     <li>the sets themselves are not null</li>
     *     <li>no vertex in the vertex set is null</li>
     *     <li>no edge in the edge set is null</li>
     *     <li>every edge endpoint exists in the vertex set</li>
     *     <li>no self-loop edges exist</li>
     * </ul>
     *
     * <p>After validation, the constructor builds the adjacency list so that
     * every vertex is present as a key and every undirected edge is inserted
     * into both endpoint neighbor lists.
     *
     * @param vertices the initial set of vertices
     * @param edges the initial set of edges
     * @throws IllegalArgumentException if the sets are null, contain null
     * vertices/edges, contain self-loops, or if an edge references a vertex
     * not contained in the vertex set
     */
    public AdjacencyListGraph(Set<Vertex<T>> vertices, Set<Edge<T>> edges) {
        super(vertices, edges);

        adjList = new HashMap<>();

        // First, validate and create empty neighbor lists for every vertex.
        for (Vertex<T> vertex : this.vertices) {
            if (vertex == null) {
                throw new IllegalArgumentException("Vertex set cannot contain null");
            }
            adjList.put(vertex, new ArrayList<>());
        }

        // Next, validate every edge and insert it into the undirected adjacency list.
        for (Edge<T> edge : this.edges) {
            validateEdgeForConstructor(edge);

            // Because the graph is undirected, each edge must appear twice:
            // once from u to v and once from v to u.
            addNeighbor(edge.u(), edge.v(), edge.weight());
            addNeighbor(edge.v(), edge.u(), edge.weight());
        }
    }

    /**
     * Returns the adjacency list of the graph.
     *
     * <p>This is an O(1) operation because we simply return an unmodifiable
     * view of the backing map.
     *
     * <p>Note: this protects the map structure itself from replacement or
     * removal by outside code. The graph still internally manages the actual
     * neighbor lists.
     *
     * @return an unmodifiable view of the adjacency list
     */
    @Override
    public Map<Vertex<T>, List<VertexDistance<T>>> getAdjList() {
        return Collections.unmodifiableMap(adjList);
    }

    /**
     * Returns the neighbors of a given vertex.
     *
     * <p>This is an O(1) lookup in the adjacency map.
     *
     * @param vertex the vertex whose neighbors should be returned
     * @return an unmodifiable view of that vertex's neighbor list
     * @throws IllegalArgumentException if the vertex is null or not in the graph
     */
    @Override
    public List<VertexDistance<T>> getNeighbors(Vertex<T> vertex) {
        if (vertex == null || !vertices.contains(vertex)) {
            throw new IllegalArgumentException("Vertex must be non-null and exist in the graph");
        }

        return Collections.unmodifiableList(adjList.get(vertex));
    }

    /**
     * Adds a vertex to the graph.
     *
     * <p>The new vertex begins with no incident edges, so it receives an empty
     * neighbor list in the adjacency map.
     *
     * @param vertex the vertex to add
     * @throws IllegalArgumentException if the vertex is null or already exists
     */
    @Override
    public void addVertex(Vertex<T> vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("Vertex cannot be null");
        }
        if (vertices.contains(vertex)) {
            throw new IllegalArgumentException("Vertex already exists in the graph");
        }

        vertices.add(vertex);
        adjList.put(vertex, new ArrayList<>());
    }

    /**
     * Removes a vertex from the graph.
     *
     * <p>Removing a vertex also removes every edge incident to that vertex.
     * That means:
     * <ol>
     *     <li>remove the vertex from the vertex set</li>
     *     <li>remove all matching edges from the edge set</li>
     *     <li>remove the vertex from every neighbor list in the adjacency map</li>
     *     <li>remove the vertex's own adjacency-list entry</li>
     * </ol>
     *
     * @param vertex the vertex to remove
     * @throws IllegalArgumentException if the vertex is null or not in the graph
     */
    @Override
    public void removeVertex(Vertex<T> vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("Vertex cannot be null");
        }
        if (!vertices.contains(vertex)) {
            throw new IllegalArgumentException("Vertex does not exist in the graph");
        }

        // Remove all edges incident to this vertex from the edge set.
        edges.removeIf(edge -> edge.u().equals(vertex) || edge.v().equals(vertex));

        // Remove the vertex from every other vertex's neighbor list.
        for (Vertex<T> other : vertices) {
            if (!other.equals(vertex)) {
                removeNeighbor(other, vertex);
            }
        }

        // Remove the vertex's own adjacency-list entry.
        adjList.remove(vertex);

        // Finally remove the vertex from the graph's vertex set.
        vertices.remove(vertex);
    }

    /**
     * Adds an undirected edge to the graph.
     *
     * <p>If either endpoint vertex does not already exist in the graph,
     * it is added automatically. This matches the project note that the
     * edge should be added "including its endpoints."
     *
     * <p>Because the graph is undirected, the edge is stored once in the
     * edge set but represented twice in the adjacency list.
     *
     * @param edge the edge to add
     * @throws IllegalArgumentException if the edge is null, already exists,
     * or is a self-loop
     */
    @Override
    public void addEdge(Edge<T> edge) {
        validateEdgeForMutation(edge);

        if (edges.contains(edge)) {
            throw new IllegalArgumentException("Edge already exists in the graph");
        }

        // Ensure both endpoint vertices exist in the graph.
        if (!vertices.contains(edge.u())) {
            addVertex(edge.u());
        }
        if (!vertices.contains(edge.v())) {
            addVertex(edge.v());
        }

        edges.add(edge);

        // Add both directions because this is an undirected graph.
        addNeighbor(edge.u(), edge.v(), edge.weight());
        addNeighbor(edge.v(), edge.u(), edge.weight());
    }

    /**
     * Removes an edge from the graph.
     *
     * <p>Because the graph is undirected, the edge must be removed from both
     * endpoint neighbor lists as well as from the graph's edge set.
     *
     * @param edge the edge to remove
     * @throws IllegalArgumentException if the edge is null or not in the graph
     */
    @Override
    public void removeEdge(Edge<T> edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }
        if (!edges.contains(edge)) {
            throw new IllegalArgumentException("Edge does not exist in the graph");
        }

        edges.remove(edge);
        removeNeighbor(edge.u(), edge.v());
        removeNeighbor(edge.v(), edge.u());
    }

    /**
     * Validates an edge while the constructor is building the initial graph.
     *
     * @param edge the edge to validate
     * @throws IllegalArgumentException if the edge is null, has a null endpoint,
     * references a vertex not in the graph, or is a self-loop
     */
    private void validateEdgeForConstructor(Edge<T> edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Edge set cannot contain null");
        }
        if (edge.u() == null || edge.v() == null) {
            throw new IllegalArgumentException("Edge endpoints cannot be null");
        }
        if (edge.u().equals(edge.v())) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        if (!vertices.contains(edge.u()) || !vertices.contains(edge.v())) {
            throw new IllegalArgumentException("Every edge endpoint must be in the vertex set");
        }
    }

    /**
     * Validates an edge for add/remove mutation operations.
     *
     * @param edge the edge to validate
     * @throws IllegalArgumentException if the edge is null, has a null endpoint,
     * or is a self-loop
     */
    private void validateEdgeForMutation(Edge<T> edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }
        if (edge.u() == null || edge.v() == null) {
            throw new IllegalArgumentException("Edge endpoints cannot be null");
        }
        if (edge.u().equals(edge.v())) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
    }

    /**
     * Adds one neighbor entry to the adjacency list.
     *
     * <p>This method only inserts one direction. For undirected behavior,
     * callers must invoke it for both endpoint directions.
     *
     * @param from the source vertex whose neighbor list will be updated
     * @param to the neighboring vertex to add
     * @param weight the edge weight to store
     */
    private void addNeighbor(Vertex<T> from, Vertex<T> to, int weight) {
        adjList.get(from).add(new VertexDistance<>(to, weight));
    }

    /**
     * Removes a single neighbor entry from the adjacency list.
     *
     * <p>This removes the {@code to} vertex from the {@code from} vertex's
     * neighbor list, regardless of the stored edge weight.
     *
     * @param from the vertex whose neighbor list should be edited
     * @param to the neighbor vertex to remove
     */
    private void removeNeighbor(Vertex<T> from, Vertex<T> to) {
        List<VertexDistance<T>> neighbors = adjList.get(from);
        neighbors.removeIf(vertexDistance -> vertexDistance.vertex().equals(to));
    }
}