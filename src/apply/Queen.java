package apply;
// she is a queen and this is her kingdom and she will slay the day away 

import implement.GraphAlgorithms;
import refactor.Edge;
import refactor.MutableGraph;
import refactor.StaticGraph;
import refactor.Vertex;
import refactor.VertexDistance;
import refactor.DisjointSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
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
 * <p>
 * <br>
 * Agree Here: I agree
 *
 * ------------------------------------------------
 *
 * High-level overview:
 * This class acts kinda like the main "city manager" 
 * It keeps track of two different graph systems at once:
 *
 * 1. Roads graph
 *    - vertices are intersections
 *    - used for neighborhoods, travel, and routing
 *
 * 2. Grid graph
 *    - vertices are buildings
 *    - used for wire placement, power-site analysis, and MST consolidation
 *
 * The road graph leans heavily on a Disjoint Set so neighborhood membership
 * can be updated and queried efficiently after new roads are added.
 */
public class Queen implements StaticWaddleWorks {

    private final MutableGraph<Intersection> roads; // road network between intersections
    private final MutableGraph<Building> grid;      // electrical grid between buildings
    private final DisjointSet<Intersection> roadNeighborhoods; // tracks connected road components

    /**
     * Creates a new WaddleWorks instance.
     *
     * The constructor sets up the two graphs, validates that the initial data
     * is reasonable, and preloads the disjoint set so the road neighborhoods
     * already match the starting road graph.
     *
     * @param roads the initial road graph
     * @param grid the initial electrical grid graph
     */
    public Queen(MutableGraph<Intersection> roads,
                 MutableGraph<Building> grid) {
        // some basic validation first to avoid ending up in a bad state
        if (roads == null || grid == null) {
            throw new IllegalArgumentException("Graphs cannot be null");
        }

        this.roads = roads; 
        this.grid = grid;
        this.roadNeighborhoods = new DisjointSet<>();

        // Every building in the grid must point to a valid closest road intersection.
        validateInitialGridBuildings();

        if (!isConnected(grid)) {
            throw new IllegalArgumentException("Initial grid must be fully connected");
        }

        // First, ensure every road intersection exists as a singleton set.
        for (Vertex<Intersection> vertex : roads.getVertices()) {
            roadNeighborhoods.find(vertex.data());
        }

        // Then union endpoints of every existing road so the DS neighborhoods are up-to-date from the start
        for (Edge<Intersection> edge : roads.getEdges()) {
            roadNeighborhoods.union(edge.u().data(), edge.v().data());
        }
    }

    @Override
    public boolean addRoad(Intersection a, Intersection b, int duration) {
        // Basic argument validation
        if (a == null || b == null) {
            throw new IllegalArgumentException("Intersections cannot be null");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Road duration cannot be negative");
        }
        if (a.equals(b)) {
            throw new IllegalArgumentException("Roads cannot be self-loops");
        }

        // Wrap the raw data in Vertex objects so they can be used in the graph.
        Vertex<Intersection> vertexA = new Vertex<>(a);
        Vertex<Intersection> vertexB = new Vertex<>(b);
        boolean hadA = roads.getVertices().contains(vertexA);
        boolean hadB = roads.getVertices().contains(vertexB);

        // New intersections should not count as "preexisting neighborhoods"
        // for the boolean return value. So we only check for a neighborhood merge
        // if both endpoints were already present in the road graph.
        boolean mergedNeighborhoods = false;
        if (hadA && hadB) {
            mergedNeighborhoods = !roadNeighborhoods.find(a).equals(roadNeighborhoods.find(b));
        }

        // actually add the road edge to the graph.
        roads.addEdge(new Edge<>(vertexA, vertexB, duration));

        // Make sure both endpoints exist in the disjoint set, then merge them.
        roadNeighborhoods.find(a);
        roadNeighborhoods.find(b);
        roadNeighborhoods.union(a, b);

        return mergedNeighborhoods;
    }

    @Override
    public void addWire(Building a, Building b, int length) {
        // validate the building inputs before modifying the electrical grid :)
        if (a == null || b == null) {
            throw new IllegalArgumentException("Buildings cannot be null");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Wire length cannot be negative");
        }

        // Every building must point to a valid closest intersection in the roads graph
        validateBuildingClosestIntersection(a);
        validateBuildingClosestIntersection(b);

        // no self loops allowed in the grid graphs either, that would be bad
        if (a.equals(b)) {
            throw new IllegalArgumentException("Wires cannot be self-loops");
        }

        // Check if the buildings already exist in the grid. If not, we'll add them as new vertices.
        Vertex<Building> vertexA = new Vertex<>(a);
        Vertex<Building> vertexB = new Vertex<>(b);
        boolean hasA = grid.getVertices().contains(vertexA);
        boolean hasB = grid.getVertices().contains(vertexB);

        // If both buildings are brand new and the grid already has vertices,
        // this single wire would create a separate disconnected component, which is bad
        if (grid.getVertexCount() > 0 && !hasA && !hasB) {
            throw new IllegalArgumentException("Operation would disconnect the grid");
        }

        // finally add the edge to the grid graph
        grid.addEdge(new Edge<>(vertexA, vertexB, length));
    }

    @Override
    public int getNeighborhoodCount() {
        // Each root in the disjoint set represents one road neighborhood.
        return roadNeighborhoods.getRoots().size();
    }

    @Override
    public Map<Intersection, Set<Intersection>> getNeighborhoodsByConnections(int i, int j) {
        // validate the bounds before doing any work
        if (i < 0 || j < 0 || i > j) {
            throw new IllegalArgumentException("Bounds must be non-negative and ordered");
        }

        // this will be the final result map we return, mapping neighborhood roots to their member intersections
        Map<Intersection, Set<Intersection>> grouped = new HashMap<>();

        // Walk through every road intersection and inspect its degree.
        for (Vertex<Intersection> vertex : roads.getVertices()) {
            int degree = roads.getNeighbors(vertex).size();

            // Only include intersections whose connection count is inside [i, j].
            if (degree >= i && degree <= j) {
                Intersection root = roadNeighborhoods.find(vertex.data());

                // The disjoint-set root is used as the map key so all members of
                // the same neighborhood end up grouped together.
                grouped.computeIfAbsent(root, key -> new HashSet<>()).add(vertex.data());
            }
        }

        return grouped;
    }

    @Override
    public int getMinBetween(Building from, Building to) {
        // These buildings must exist in the grid and map back to valid roads.
        validateGridBuildingForTraversal(from);
        validateGridBuildingForTraversal(to);

        // make vertices for the closest intersections of the two buildings so we can run BFS on the roads graph
        Vertex<Intersection> start = new Vertex<>(from.closest());
        Vertex<Intersection> target = new Vertex<>(to.closest());

        // this will track the minimum number of roads taken to reach each visited intersection during BFS
        Queue<Vertex<Intersection>> queue = new ArrayDeque<>();
        // this will be the distance map we return if we reach the target, 
        // or use to determine that the target is unreachable if we exhaust the search
        Map<Vertex<Intersection>, Integer> distanceByRoadCount = new HashMap<>();
       
        queue.add(start); 
        distanceByRoadCount.put(start, 0); // zero roads taken at the beginning

        // Standard BFS over the road graph
        while (!queue.isEmpty()) {
            Vertex<Intersection> current = queue.remove();

            // As soon as we reach the target, we know the minimum number of roads.
            if (current.equals(target)) {
                return distanceByRoadCount.get(current);
            }

            // now we check all the neighbors of the current intersection and add them to the queue 
            // if we haven't visited them before
            for (VertexDistance<Intersection> neighbor : roads.getNeighbors(current)) {
                Vertex<Intersection> next = neighbor.vertex();

                // A missing entry means "unvisited" here.
                if (!distanceByRoadCount.containsKey(next)) {
                    distanceByRoadCount.put(next, distanceByRoadCount.get(current) + 1);
                    queue.add(next); // so we add it to the queue to explore its neighbors later
                }
            }
        }

        // If BFS never reached the target, the two buildings are basically disconnected
        return -1;
    }

    @Override
    public List<Intersection> calculateRoute(Building from, Building to,
                                             Set<Intersection.Type> avoid) {
        // These buildings must exist in the grid and map back to valid roads 
        if (avoid == null) {
            throw new IllegalArgumentException("Avoid set cannot be null");
        }

        // validate the building inputs before trying to calculate a route between them :)
        validateGridBuildingForTraversal(from);
        validateGridBuildingForTraversal(to);

        // make vertices for the closest intersections of the two buildings so we can run a 
        // modified Dijkstra's on the roads graph
        Vertex<Intersection> start = new Vertex<>(from.closest());
        Vertex<Intersection> target = new Vertex<>(to.closest());

        // This method uses a lexicographic shortest-path strat with two cost metrics:
        // 1. minimize how many "bad" intersections are used
        // 2. break ties by total travel duration
        Map<Vertex<Intersection>, Integer> badCount = new HashMap<>();
        Map<Vertex<Intersection>, Integer> duration = new HashMap<>();
        Map<Vertex<Intersection>, Vertex<Intersection>> previous = new HashMap<>();
        // this tracks which vertices have been finalized with optimal routes so we can skip stale queue entries
        Set<Vertex<Intersection>> finalized = new HashSet<>(); 

        // Start by treating every vertex as unreachable / infinitely costly.
        for (Vertex<Intersection> vertex : roads.getVertices()) {
            badCount.put(vertex, Integer.MAX_VALUE);
            duration.put(vertex, Integer.MAX_VALUE);
        }

        badCount.put(start, 0);
        duration.put(start, 0);

        // Priority order:
        //   fewer avoided/bad intersections first,
        //   then lower total travel time,
        //   then string order as a stable final tiebreaker.
        PriorityQueue<RouteState> pq = new PriorityQueue<>(
            Comparator.comparingInt(RouteState::badSeen)
                .thenComparingInt(RouteState::totalDuration)
                .thenComparing(state -> state.vertex().data().toString())
        );
        // add a RouteState to the priority queue every time we find a better path to a vertex, 
        // even if that vertex is already in the queue with a worse cost.
        pq.add(new RouteState(start, 0, 0));

        while (!pq.isEmpty()) {
            RouteState state = pq.remove();
            Vertex<Intersection> current = state.vertex();

            // Skip stale queue entries that are no longer optimal
            if (state.badSeen() != badCount.get(current)
                || state.totalDuration() != duration.get(current)) {
                continue;
            }

            // Once finalized, this vertex has already been processed optimally.
            if (!finalized.add(current)) {
                continue;
            }

            if (current.equals(target)) {
                break; // best route to target has been found
            }

            // Explore neighbors and update their best-known cost if this path is better
            for (VertexDistance<Intersection> neighbor : roads.getNeighbors(current)) {
                Vertex<Intersection> next = neighbor.vertex();

                int nextBadCount = state.badSeen()
                    + (avoid.contains(next.data().type()) ? 1 : 0);
                int nextDuration = state.totalDuration() + neighbor.distance();

                // This path is better if it has fewer bad intersections, or the same number 
                // of bad intersections but less total duration.
                boolean betterBad = nextBadCount < badCount.get(next);
                boolean sameBadBetterTime = nextBadCount == badCount.get(next)
                    && nextDuration < duration.get(next);

                // Update the best-known route only if this path improves the
                // lexicographic cost pair.
                if (betterBad || sameBadBetterTime) {
                    badCount.put(next, nextBadCount);
                    duration.put(next, nextDuration);
                    previous.put(next, current);
                    pq.add(new RouteState(next, nextBadCount, nextDuration));
                }
            }
        }

        // If target was never reached, no legal route exists.
        if (duration.get(target) == Integer.MAX_VALUE) {
            return null;
        }

        // Reconstruct the route by walking backward through the previous map.
        List<Intersection> route = new ArrayList<>();
        Vertex<Intersection> current = target;
        while (current != null) {
            route.add(0, current.data()); // prepend while walking backward
            current = previous.get(current); // move to the previous vertex on the route
        }

        return route;
    }

    @Override
    public Building getBestPowerSite(List<Building> candidates) {
        // validate the candidate list before doing any work
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidate list cannot be null or empty");
        }

        Building best = null;
        double bestAverage = Double.MAX_VALUE;

        // Try each candidate as the source and measure how central it is
        // by averaging shortest-path distances to all buildings in the grid.
        for (Building candidate : candidates) {
            if (candidate == null || !grid.getVertices().contains(new Vertex<>(candidate))) {
                throw new IllegalArgumentException("Every candidate must exist in the grid");
            }

            // this will give us the shortest distance from the candidate to every other building in the grid
            Map<Vertex<Building>, Integer> distances =
                GraphAlgorithms.dijkstras(new Vertex<>(candidate), grid);

            long total = 0;
            // sum up all the distances so we can calculate the average distance to all other buildings
            for (Integer distance : distances.values()) {
                total += distance;
            }

            double average = (double) total / grid.getVertexCount();

            // Smaller average distance means a more central power site.
            if (average < bestAverage) {
                bestAverage = average;
                best = candidate; // update the best candidate (seen so far)
            }
        }

        return best;
    }

    @Override
    public double consolidateGrid() {
        int originalTotalLength = 0;

        // Sum the full current wire cost before trimming anything.
        for (Edge<Building> edge : grid.getEdges()) {
            originalTotalLength += edge.weight();
        }

        if (originalTotalLength == 0) {
            return 0.0; // nothing to consolidate
        }

        Set<Edge<Building>> mst = GraphAlgorithms.kruskals(grid);
        if (mst == null) {
            return 0.0; // disconnected graph means no valid MST
        }

        int keptLength = 0;
        // Sum up the total length of the wires we need to keep based on the MST
        for (Edge<Building> edge : mst) {
            keptLength += edge.weight();
        }

        // Collect edges not chosen by the MST, then remove them afterward.
        // Using a temporary list avoids modifying the graph while iterating over it.
        List<Edge<Building>> toRemove = new ArrayList<>();
        for (Edge<Building> edge : grid.getEdges()) {
            if (!mst.contains(edge)) {
                toRemove.add(edge);
            }
        }

        // Remove the unnecessary wires that aren't part of the MST, which consolidates the grid.
        for (Edge<Building> edge : toRemove) {
            grid.removeEdge(edge);
        }

        // Return the fraction of wire length that was eliminated.
        return (double) (originalTotalLength - keptLength) / originalTotalLength;
    }

    @Override
    public StaticGraph<Intersection> getRoads() {
        return roads; 
    }

    @Override
    public StaticGraph<Building> getGrid() {
        return grid;
    }

    /**
     * Ensures every building already present in the initial electrical grid
     * references a closest intersection that actually exists in the road graph.
     */
    private void validateInitialGridBuildings() {
        for (Vertex<Building> vertex : grid.getVertices()) {
            validateBuildingClosestIntersection(vertex.data());
        }
    }

    /**
     * Validates that a building's closest intersection, when present, belongs
     * to the road graph.
     *
     * @param building the building to validate
     */
    private void validateBuildingClosestIntersection(Building building) {
        if (building.closest() != null
            && !roads.getVertices().contains(new Vertex<>(building.closest()))) {
            throw new IllegalArgumentException("Closest intersection must exist in the road graph");
        }
    }

    /**
     * Validation helper for path-based road operations.
     *
     * A building must:
     * - not be null
     * - exist in the grid
     * - have a closest intersection
     * - have that closest intersection present in the roads graph
     *
     * @param building the building being checked
     */
    private void validateGridBuildingForTraversal(Building building) {
        // all of these conditions are necessary to ensure the building is properly connected to the road graph so 
        // pathfinding operations can run correctly. If any of them were false, we might end up with null pointer 
        // exceptions or basically disconnected components, which would be bad.
        if (building == null) {
            throw new IllegalArgumentException("Building cannot be null");
        }
        if (!grid.getVertices().contains(new Vertex<>(building))) {
            throw new IllegalArgumentException("Building must exist in the grid");
        }
        if (building.closest() == null) {
            throw new IllegalArgumentException("Building must have a closest intersection");
        }
        if (!roads.getVertices().contains(new Vertex<>(building.closest()))) {
            throw new IllegalArgumentException("Closest intersection must exist in the road graph");
        }
    }

    /**
     * Quick graph connectivity helper used during constructor validation.
     *
     * This runs a BFS from an arbitrary start vertex and checks whether every
     * vertex in the graph becomes reachable.
     *
     * @param graph the graph to inspect
     * @param <T> the vertex data type
     * @return whether every vertex can be reached from an arbitrary start vertex
     */
    private static <T> boolean isConnected(StaticGraph<T> graph) {
        if (graph.getVertexCount() == 0) {
            return true; // empty graph is trivially connected for this use case
        }

        // this will be the arbitrary start vertex for BFS
        Vertex<T> start = graph.getVertices().iterator().next();
        // this set tracks which vertices have been visited so far during BFS to avoid cycles and redundant work
        Set<Vertex<T>> visited = new HashSet<>();
        // this queue tracks which vertex to explore next during BFS. It starts with the arbitrary start vertex and 
        // grows as we discover new vertices to explore.
        Queue<Vertex<T>> queue = new ArrayDeque<>();

        visited.add(start);
        queue.add(start);

        // keep exploring vertices until there are no more reachable ones left. Each time we explore a vertex,
        // we add all of its unvisited neighbors to the queue to explore later.
        while (!queue.isEmpty()) {
            Vertex<T> current = queue.remove();

            for (VertexDistance<T> neighbor : graph.getNeighbors(current)) {
                // visited.add(...) returns true only the first time we see a vertex
                if (visited.add(neighbor.vertex())) {
                    queue.add(neighbor.vertex());
                }
            }
        }

        return visited.size() == graph.getVertexCount();
    }

    /**
     * Small helper record used by calculateRoute.
     *
     * badSeen is the primary cost metric.
     * totalDuration is the secondary tiebreaker.
     */
    private record RouteState(Vertex<Intersection> vertex, int badSeen, int totalDuration) {
    }
}
// slay the day away