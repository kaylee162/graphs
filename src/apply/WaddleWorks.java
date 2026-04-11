package apply;

import implement.GraphAlgorithms;
import refactor.*;
import java.util.*;

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
 * Concrete implementation of StaticWaddleWorks.
 *
 * This class manages:
 * - Road network (Intersections graph)
 * - Electrical grid (Buildings graph)
 *
 * Core idea:
 * - Roads use DisjointSet for connected components (neighborhoods)
 * - Grid uses graph algorithms (BFS, Dijkstra, Kruskal)
 */
public class WaddleWorks implements StaticWaddleWorks {

    private final MutableGraph<Intersection> roads;
    private final MutableGraph<Building> grid;

    // Tracks connected components for roads (neighborhoods)
    private final DisjointSet<Intersection> roadDS;

    /**
     * Constructor
     */
    public WaddleWorks(MutableGraph<Intersection> roads,
                       MutableGraph<Building> grid) {

        this.roads = roads;
        this.grid = grid;
        this.roadDS = new DisjointSet<>();

        // Initialize DS with existing graph
        for (Vertex<Intersection> v : roads.getVertices()) {
            roadDS.find(v.data());
        }

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

        boolean separate = !roadDS.find(a).equals(roadDS.find(b));

        roads.addEdge(new Edge<>(va, vb, duration));
        roadDS.union(a, b);

        return separate;
    }

    // =====================================================
    // 2. ADD WIRE
    // =====================================================
    @Override
    public void addWire(Building a, Building b, int length) {
        grid.addEdge(new Edge<>(new Vertex<>(a), new Vertex<>(b), length));
    }

    // =====================================================
    // 3. NEIGHBORHOOD COUNT (O(1))
    // =====================================================
    @Override
    public int getNeighborhoodCount() {
        return roadDS.getRoots().size();
    }

    // =====================================================
    // 4. GROUP BY CONNECTION COUNT
    // =====================================================
    @Override
    public Map<Intersection, Set<Intersection>> getNeighborhoodsByConnections(int i, int j) {

        Map<Intersection, Set<Intersection>> result = new HashMap<>();

        for (Vertex<Intersection> v : roads.getVertices()) {
            Intersection inter = v.data();
            Intersection root = roadDS.find(inter);

            int degree = roads.getNeighbors(v).size();

            if (degree >= i && degree <= j) {
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

        if (from == null || to == null ||
            from.closest() == null || to.closest() == null) {
            return -1;
        }

        Vertex<Intersection> start = new Vertex<>(from.closest());
        Vertex<Intersection> end = new Vertex<>(to.closest());

        Queue<Vertex<Intersection>> queue = new LinkedList<>();
        Map<Vertex<Intersection>, Integer> dist = new HashMap<>();

        queue.add(start);
        dist.put(start, 0);

        while (!queue.isEmpty()) {
            Vertex<Intersection> curr = queue.poll();

            if (curr.equals(end)) {
                return dist.get(curr);
            }

            for (VertexDistance<Intersection> neighbor : roads.getNeighbors(curr)) {
                Vertex<Intersection> next = neighbor.vertex();

                if (!dist.containsKey(next)) {
                    dist.put(next, dist.get(curr) + 1);
                    queue.add(next);
                }
            }
        }

        return -1;
    }

    // =====================================================
    // 6. ROUTE WITH AVOIDANCE (MODIFIED DIJKSTRA)
    // =====================================================
    @Override
    public List<Intersection> calculateRoute(Building from, Building to,
                                             Set<Intersection.Type> avoid) {

        Vertex<Intersection> start = new Vertex<>(from.closest());
        Vertex<Intersection> end = new Vertex<>(to.closest());

        Map<Vertex<Intersection>, Integer> dist = new HashMap<>();
        Map<Vertex<Intersection>, Vertex<Intersection>> prev = new HashMap<>();

        PriorityQueue<Vertex<Intersection>> pq =
                new PriorityQueue<>(Comparator.comparingInt(dist::get));

        for (Vertex<Intersection> v : roads.getVertices()) {
            dist.put(v, Integer.MAX_VALUE);
        }

        dist.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {
            Vertex<Intersection> curr = pq.poll();

            if (curr.equals(end)) break;

            for (VertexDistance<Intersection> neighbor : roads.getNeighbors(curr)) {
                Vertex<Intersection> next = neighbor.vertex();

                int penalty = avoid.contains(next.data().type()) ? 1000000 : 0;
                int newDist = dist.get(curr) + neighbor.distance() + penalty;

                if (newDist < dist.get(next)) {
                    dist.put(next, newDist);
                    prev.put(next, curr);
                    pq.add(next);
                }
            }
        }

        // Reconstruct path
        List<Intersection> path = new ArrayList<>();
        Vertex<Intersection> curr = end;

        while (curr != null) {
            path.add(0, curr.data());
            curr = prev.get(curr);
        }

        return path;
    }

    // =====================================================
    // 7. BEST POWER SITE (RUN DIJKSTRA K TIMES)
    // =====================================================
    @Override
    public Building getBestPowerSite(List<Building> candidates) {

        Building best = null;
        double bestAvg = Double.MAX_VALUE;

        for (Building b : candidates) {
            Vertex<Building> start = new Vertex<>(b);

            Map<Vertex<Building>, Integer> dist =
                    GraphAlgorithms.dijkstras(start, grid);

            double sum = 0;
            int count = 0;

            for (int d : dist.values()) {
                if (d != Integer.MAX_VALUE) {
                    sum += d;
                    count++;
                }
            }

            double avg = sum / count;

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

        int original = grid.getEdges().size();

        Set<Edge<Building>> mst = GraphAlgorithms.kruskals(grid);

        if (mst == null) {
            return 0;
        }

        int removed = original - mst.size();

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