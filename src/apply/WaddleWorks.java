package apply;

import implement.GraphAlgorithms;
import refactor.DisjointSet;
import refactor.Edge;
import refactor.MutableGraph;
import refactor.StaticGraph;
import refactor.Vertex;
import refactor.VertexDistance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
 * High-level idea:
 * This class simulates a city system with TWO separate graphs:
 *
 * 1. Roads graph (Intersection nodes)
 *    - Tracks connectivity between intersections
 *    - Uses Disjoint Set to efficiently track neighborhoods (connected components)
 *
 * 2. Electrical grid graph (Building nodes)
 *    - Used for pathfinding and optimization problems
 *    - Uses BFS, Dijkstra (im not typing this so from this point on it will be D), and Kruskal depending on the task
 */
public class WaddleWorks implements StaticWaddleWorks {

    private final MutableGraph<Intersection> roads;   // Graph of intersections (road network)
    private final MutableGraph<Building> grid;        // Graph of buildings (power grid)

    // Disjoint Set keeps track of which intersections belong to the same neighborhood
    private final DisjointSet<Intersection> roadDS;

    /**
     * Constructor.
     *
     * Builds the internal structures and initializes the Disjoint Set
     * so it reflects the current state of the roads graph.
     *
     * @param roads the road graph made of intersections
     * @param grid the electrical grid graph made of buildings
     */
    public WaddleWorks(MutableGraph<Intersection> roads,
                       MutableGraph<Building> grid) {

        this.roads = roads;
        this.grid = grid;
        this.roadDS = new DisjointSet<>();

        // Step 1: Make each intersection its own set initially
        for (Vertex<Intersection> v : roads.getVertices()) {
            roadDS.find(v.data()); // creates singleton set if not present
        }

        // Step 2: Merge sets for every existing road
        for (Edge<Intersection> e : roads.getEdges()) {
            roadDS.union(e.u().data(), e.v().data());
        }
    }

    // =====================================================
    // 1. ADD ROAD
    // =====================================================
    @Override
    public boolean addRoad(Intersection a, Intersection b, int duration) {

        Vertex<Intersection> va = new Vertex<>(a);
        Vertex<Intersection> vb = new Vertex<>(b);

        // Check if these intersections are currently in DIFFERENT components
        // (i.e., would this road connect two separate neighborhoods?)
        boolean separate = !roadDS.find(a).equals(roadDS.find(b));

        // Always add the edge to the graph
        roads.addEdge(new Edge<>(va, vb, duration));

        // Merge their components in the Disjoint Set
        roadDS.union(a, b);

        // Return true ONLY if this road connected two previously separate groups
        return separate;
    }

    // =====================================================
    // 2. ADD WIRE
    // =====================================================
    @Override
    public void addWire(Building a, Building b, int length) {
        // Simple graph edge insert
        grid.addEdge(new Edge<>(new Vertex<>(a), new Vertex<>(b), length));
    }

    // =====================================================
    // 3. NEIGHBORHOOD COUNT (O(1))
    // =====================================================
    @Override
    public int getNeighborhoodCount() {
        // Each root in the Disjoint Set represents one connected component
        return roadDS.getRoots().size();
    }

    // =====================================================
    // 4. GROUP BY CONNECTION COUNT
    // =====================================================
    @Override
    public Map<Intersection, Set<Intersection>> getNeighborhoodsByConnections(int i, int j) {

        // Map from neighborhood root -> set of intersections in that neighborhood with degree in [i, j]
        Map<Intersection, Set<Intersection>> result = new HashMap<>();

        for (Vertex<Intersection> v : roads.getVertices()) {
            Intersection inter = v.data();

            // Find the "leader" (root) of this intersection's component
            Intersection root = roadDS.find(inter);

            // Degree = number of neighbors (how many roads connect here)
            int degree = roads.getNeighbors(v).size();

            // Only include intersections within the requested degree range
            if (degree >= i && degree <= j) {

                // Group by root -> this clusters intersections by neighborhood
                result.putIfAbsent(root, new HashSet<>());
                result.get(root).add(inter);
            }
        }

        return result;
    }

    // =====================================================
    // 5. MIN ROADS BETWEEN BUILDINGS (BFS)
    // =====================================================
    @Override
    public int getMinBetween(Building from, Building to) {

        // Defensive checks: invalid buildings or no closest intersections
        if (from == null
            || to == null
            || from.closest() == null
            || to.closest() == null) {
            return -1;
        }

        // Convert buildings → their closest intersections (graph nodes)
        Vertex<Intersection> start = new Vertex<>(from.closest());
        Vertex<Intersection> end = new Vertex<>(to.closest());

        Queue<Vertex<Intersection>> queue = new LinkedList<>();
        Map<Vertex<Intersection>, Integer> dist = new HashMap<>();

        queue.add(start);
        dist.put(start, 0); // distance = number of edges (roads)

        // Standard BFS for shortest path in unweighted graph
        while (!queue.isEmpty()) {
            Vertex<Intersection> curr = queue.poll();

            // Found destination
            if (curr.equals(end)) {
                return dist.get(curr);
            }

            // Explore neighbors :)
            for (VertexDistance<Intersection> neighbor : roads.getNeighbors(curr)) {
                Vertex<Intersection> next = neighbor.vertex();

                // Only visit unvisited nodes
                if (!dist.containsKey(next)) {
                    dist.put(next, dist.get(curr) + 1); // +1 road traveled YAY!!
                    queue.add(next);
                }
            }
        }

        // No path found
        return -1;
    }

    // =====================================================
    // 6. ROUTE WITH AVOIDANCE (MODIFIED D)
    // =====================================================
    @Override
    public List<Intersection> calculateRoute(Building from, Building to,
                                             Set<Intersection.Type> avoid) {

        Vertex<Intersection> start = new Vertex<>(from.closest());
        Vertex<Intersection> end = new Vertex<>(to.closest());

        Map<Vertex<Intersection>, Integer> dist = new HashMap<>();
        Map<Vertex<Intersection>, Vertex<Intersection>> prev = new HashMap<>();

        // Priority queue ordered by current shortest distance
        PriorityQueue<Vertex<Intersection>> pq =
                new PriorityQueue<>(Comparator.comparingInt(dist::get));

        // Initialize all distances to "infinity"
        for (Vertex<Intersection> v : roads.getVertices()) {
            dist.put(v, Integer.MAX_VALUE);
        }

        dist.put(start, 0);
        pq.add(start);

        // modified D that adds a HUGE penalty to any path that goes through an intersection type we want to avoid
        while (!pq.isEmpty()) {
            Vertex<Intersection> curr = pq.poll();

            if (curr.equals(end)) {
                break; // early exit
            } 

            for (VertexDistance<Intersection> neighbor : roads.getNeighbors(curr)) {
                Vertex<Intersection> next = neighbor.vertex();

                // HUGE penalty if this intersection type should be avoided :(
                int penalty = avoid.contains(next.data().type()) ? 1000000 : 0;

                int newDist = dist.get(curr) + neighbor.distance() + penalty;

                // Relaxation step (classic D :P)
                if (newDist < dist.get(next)) {
                    dist.put(next, newDist);
                    prev.put(next, curr);
                    pq.add(next);
                }
            }
        }

        // Reconstruct path from end tostart using "prev" pointers
        List<Intersection> path = new ArrayList<>();
        Vertex<Intersection> curr = end;

        while (curr != null) {
            path.add(0, curr.data()); // prepend
            curr = prev.get(curr);
        }

        return path;
    }

    // =====================================================
    // 7. BEST POWER SITE (RUN D K TIMES)
    // =====================================================
    @Override
    public Building getBestPowerSite(List<Building> candidates) {

        Building best = null;
        double bestAvg = Double.MAX_VALUE;

        // Try each building as a potential power source
        for (Building b : candidates) {

            Vertex<Building> start = new Vertex<>(b);

            // Run D from this building
            Map<Vertex<Building>, Integer> dist =
                    GraphAlgorithms.dijkstras(start, grid);

            double sum = 0;
            int count = 0;

            // Compute average distance to all reachable buildings
            for (int d : dist.values()) {
                if (d != Integer.MAX_VALUE) {
                    sum += d;
                    count++;
                }
            }

            double avg = sum / count;

            // Keep the building with the lowest average distance
            if (avg < bestAvg) {
                bestAvg = avg;
                best = b;
            }
        }

        return best;
    }

    // =====================================================
    // 8. MST (KRUSKAL)
    // =====================================================
    @Override
    public double consolidateGrid() {

        // Original number of wires in the grid
        int original = grid.getEdges().size();

        // Build Minimum Spanning Tree (keeps only essential wires)
        Set<Edge<Building>> mst = GraphAlgorithms.kruskals(grid);

        if (mst == null) {
            return 0;
        }

        int removed = original - mst.size();

        // Return fraction of wires that were removed
        return (double) removed / original;
    }

    // =====================================================
    // 9. GETTERS
    // =====================================================
    @Override
    public StaticGraph<Intersection> getRoads() {
        return roads; 
    }

    @Override
    public StaticGraph<Building> getGrid() {
        return grid; 
    }
}