# Project 3
## Overview

This project is a Java graph-based city planning simulation. It is split into two main layers:

1. **Core graph data structures and algorithms**
   - A mutable undirected weighted graph implementation
   - A disjoint set / union-find structure
   - Standard graph algorithms like BFS, DFS, Dijkstra’s, Prim’s, and Kruskal’s

2. **WaddleWorks application logic**
   - Models a **road network** using intersections
   - Models an **electrical grid** using buildings
   - Supports neighborhood tracking, route calculation, central power-site selection, and grid consolidation

At a high level, the project takes classic CS 1332 graph topics and applies them to a more realistic “city systems” setting.

---

## Project Structure

```text
Project3/
├── README.md
├── project3.pdf
├── pr3-student.iml
├── submit-unix.sh
├── submit-windows.bat
├── submission.zip
├── lib/
│   └── junit-platform-console-standalone-1.13.0-M3.jar
├── src/
│   ├── Main.java
│   ├── apply/
│   │   ├── Building.java
│   │   ├── Intersection.java
│   │   ├── Queen.java
│   │   └── StaticWaddleWorks.java
│   ├── implement/
│   │   └── GraphAlgorithms.java
│   └── refactor/
│       ├── DisjointSet.java
│       ├── DisjointSetNode.java
│       ├── Edge.java
│       ├── MutableGraph.java
│       ├── Princess.java
│       ├── StaticGraph.java
│       ├── Vertex.java
│       └── VertexDistance.java
└── tst/
    ├── DisjointSetStudentTests.java
    ├── GraphAlgorithmsStudentTests.java
    ├── MutableGraphStudentTests.java
    └── WaddleWorksStudentTests.java
````

---

## File Breakdown

### `src/Main.java`

Entry point used by the project scaffolding. It exposes factory methods that return:

* a `MutableGraph` implementation through `Princess`
* a `StaticWaddleWorks` implementation through `Queen`

This keeps the rest of the autograder and tests decoupled from the actual class names.

---

### `src/refactor/StaticGraph.java`

Interface for an undirected graph. It defines the read-only graph API:

* vertex set access
* edge set access
* adjacency list access
* neighbor lookup
* count and containment methods

This is the base abstraction used by the algorithm code.

---

### `src/refactor/MutableGraph.java`

Abstract extension of `StaticGraph` that adds mutation operations:

* `addVertex`
* `removeVertex`
* `addEdge`
* `removeEdge`

It stores the backing vertex and edge sets, while leaving the concrete adjacency-list behavior to the subclass.

---

### `src/refactor/Princess.java`

Concrete mutable graph implementation.

This class is the main graph representation used by the project. It stores:

* a set of vertices
* a set of edges
* an adjacency list mapping each vertex to a list of neighboring vertices and weights

#### High-level behavior

* The graph is **undirected**
* The graph is **weighted**
* The graph is **simple**
* Every undirected edge is stored in both directions inside the adjacency list

#### Key implementation details

* The constructor validates the graph input and builds the adjacency list
* `getNeighbors` is an adjacency-list lookup
* `addEdge` automatically adds endpoint vertices if they are missing
* `removeVertex` removes all incident edges and neighbor-list references
* `removeEdge` removes the edge from both adjacency directions

This is the low-level structure that the rest of the project depends on.

---

### `src/refactor/DisjointSet.java`

Union-find / disjoint set implementation.

This structure is used to efficiently track connected components of the road graph.

#### Key implementation details

* **Path compression** in `find`
* **Union by rank** in `union`
* An explicit `roots` set is maintained so neighborhood counting is very fast

This is especially important in `Queen`, where road neighborhoods need to be updated as roads are added.

---

### `src/refactor/DisjointSetNode.java`

Internal helper node for the disjoint set. It stores:

* parent pointer
* data
* rank

This is standard support code for the union-find structure.

---

### `src/refactor/Vertex.java`

Simple record wrapper for graph vertex data.

Used so the graph infrastructure can treat arbitrary data types uniformly.

---

### `src/refactor/Edge.java`

Record representing an undirected weighted edge.

#### Important details

* `equals` treats `(u, v)` and `(v, u)` as the same edge
* `compareTo` compares by weight
* This makes it work cleanly with MST priority queues

---

### `src/refactor/VertexDistance.java`

Record pairing a vertex with an integer distance/weight.

Used heavily in:

* adjacency lists
* Dijkstra’s priority queue
* BFS/DFS neighbor traversal

---

### `src/implement/GraphAlgorithms.java`

Contains the core graph algorithms required for the project.

#### Implemented algorithms

* **BFS**
* **DFS**
* **Dijkstra’s**
* **Prim’s**
* **Kruskal’s**

#### High-level role

This class is the reusable algorithm toolbox. The graph layer provides structure, and this class provides the actual traversal and optimization logic.

---

### `src/apply/Building.java`

Record representing a building in the WaddleWorks system.

Each building stores:

* a unique name
* its closest road intersection

The “closest intersection” is the bridge between the electrical-grid graph and the road-network graph.

---

### `src/apply/Intersection.java`

Record representing an intersection in the road network.

Each intersection stores:

* an ID
* a type

Supported intersection types include:

* `NORMAL`
* `TOLL`
* `BRIDGE`
* `TUNNEL`
* `ROUNDABOUT`
* `HIGHWAY`

These types matter for route calculation, since some route queries try to avoid certain types when possible.

---

### `src/apply/StaticWaddleWorks.java`

Interface for the application layer.

It defines the expected operations of the city-planning simulation, including:

* adding roads
* adding wires
* counting neighborhoods
* grouping intersections by connection count
* finding minimum-road travel between buildings
* calculating weighted routes while avoiding certain intersection types
* choosing the best power site
* consolidating the electrical grid

This interface also documents the intended runtime for the major operations.

---

### `src/apply/Queen.java`

Concrete implementation of the WaddleWorks system.

This is the most application-specific file in the project. It manages two separate graphs:

* **Road graph**: intersections connected by roads
* **Grid graph**: buildings connected by wires

It also maintains a disjoint set to keep track of connected road neighborhoods.

#### What it does

* Validates road and grid data during construction
* Ensures initial grid connectivity
* Updates neighborhood membership when roads are added
* Ensures new wire additions do not break required connectivity assumptions
* Uses graph algorithms to answer planning questions

#### Main operations

* `addRoad(...)`
* `addWire(...)`
* `getNeighborhoodCount()`
* `getNeighborhoodsByConnections(...)`
* `getMinBetween(...)`
* `calculateRoute(...)`
* `getBestPowerSite(...)`
* `consolidateGrid()`

---

### `tst/`

Student tests for the major components:

* `DisjointSetStudentTests.java`
* `GraphAlgorithmsStudentTests.java`
* `MutableGraphStudentTests.java`
* `WaddleWorksStudentTests.java`

These help verify correctness for the graph layer, the disjoint set, and the WaddleWorks application logic.

---

## Core Algorithms and Time Complexity

## 1. Breadth-First Search (BFS)

Used in `GraphAlgorithms.bfs(...)`.

### Purpose

Visits vertices level by level from a starting point.

### Used for

* unweighted shortest path style traversal
* minimum-number-of-edges exploration

### Time Complexity

**O(V + E)**

Each vertex is visited once, and each edge is considered at most once across the traversal.

---

## 2. Depth-First Search (DFS)

Used in `GraphAlgorithms.dfs(...)`.

### Purpose

Explores as far as possible down one path before backtracking.

### Time Complexity

**O(V + E)**

Like BFS, every vertex and edge is processed at most once.

---

## 3. Dijkstra’s Algorithm

Used in `GraphAlgorithms.dijkstras(...)`.

### Purpose

Finds shortest weighted distances from one start vertex to every other vertex in a graph with nonnegative edge weights.

### Time Complexity

**O((V + E) log V)** in the usual adjacency-list + priority-queue model
Often written more loosely as **O(E log V)**

This is also the main tool used by the power-site logic.

---

## 4. Prim’s Algorithm

Used in `GraphAlgorithms.prims(...)`.

### Purpose

Builds a minimum spanning tree starting from a selected vertex.

### Time Complexity

**O(E log V)**

The priority queue is used to repeatedly pick the cheapest boundary edge.

---

## 5. Kruskal’s Algorithm

Used in `GraphAlgorithms.kruskals(...)`.

### Purpose

Builds a minimum spanning tree by sorting edges globally and adding them if they do not form a cycle.

### Time Complexity

**O(E log E)**

The priority queue processes edges by ascending weight, and the disjoint set keeps cycle checks efficient.

---

## WaddleWorks Feature Summary

## Road Network

The road graph models travel between intersections.

### Supported behavior

* add a road between intersections
* track how many connected neighborhoods exist
* organize intersections by neighborhood and degree
* compute the minimum number of roads between buildings
* compute a route that prefers avoiding undesirable intersection types

### Key idea

Buildings are not directly used for travel. Instead, each building maps to its closest intersection, and travel runs across the road graph.

---

## Electrical Grid

The grid graph models wire connections between buildings.

### Supported behavior

* add wire connections
* keep the grid valid and connected
* choose the most central building as a potential power site
* remove unnecessary wire segments while preserving connectivity

### Key idea

This side of the project is essentially a weighted building graph. It is used for shortest-path analysis and MST-based optimization.

---

## How the Main WaddleWorks Methods Work

### `addRoad(a, b, duration)`

Adds a road edge to the road graph and updates the disjoint set.

* If both intersections already existed and were in different components, this merges neighborhoods
* Returns whether two existing neighborhoods were connected

**Target complexity:** `O(1)` amortized-style intent from the interface

---

### `addWire(a, b, length)`

Adds a wire edge to the grid graph.

* Validates both buildings
* Ensures the building-to-intersection mapping is valid
* Prevents bad operations that would introduce an invalid disconnected component

**Target complexity:** `O(1)` by interface spec

---

### `getNeighborhoodCount()`

Returns the number of connected road components.

This is fast because the disjoint set maintains a `roots` set.

**Complexity:** `O(1)`

---

### `getNeighborhoodsByConnections(i, j)`

Groups intersections by road neighborhood, but only includes those whose degree falls in the inclusive range `[i, j]`.

**Complexity:** `O(R + I)`

It scans the intersections and uses the disjoint set to identify each neighborhood representative.

---

### `getMinBetween(from, to)`

Finds the minimum number of roads required to travel between two buildings.

### How it works

* map each building to its closest intersection
* run BFS on the road graph
* return the fewest edges between those intersections

**Complexity:** `O(I + R)`

---

### `calculateRoute(from, to, avoid)`

Finds a route between two buildings while prioritizing avoidance of certain intersection types.

### Important detail

This is not just ordinary shortest path. It uses a **lexicographic cost**:

1. minimize the number of avoided intersection types encountered
2. break ties using total travel duration

That makes the routing behavior more realistic for the problem requirements.

**Complexity:** `O(R log R)` by interface spec

---

### `getBestPowerSite(candidates)`

Chooses the building that is most central in the grid.

### How it works

For each candidate:

* run Dijkstra’s from that building
* sum distances to all buildings
* compute the average
* keep the candidate with the smallest average distance

**Complexity:** `O(k(W log W))`

where `k` is the number of candidate buildings.

---

### `consolidateGrid()`

Removes unnecessary wiring while keeping the electrical grid connected.

### How it works

* compute the total existing wire length
* run Kruskal’s to build an MST
* remove all edges not in the MST
* return the fraction of wire removed

This is basically a graph optimization pass over the building network.

**Complexity:** `O(W log W)`

---

## Design Notes

### Why two graphs?

This project separates two different real-world systems:

* **roads** model physical travel between intersections
* **grid** models electrical connectivity between buildings

That separation keeps the code cleaner and makes each operation use the correct graph.

### Why use a disjoint set?

Neighborhood counting and merging happen repeatedly for roads. A disjoint set makes component tracking much more efficient than recomputing connected components from scratch every time.

### Why use MST algorithms?

For the electrical grid, the goal of consolidation is to keep everything connected as cheaply as possible. That is exactly what a minimum spanning tree solves.

### Why use BFS for `getMinBetween`?

That method wants the fewest number of roads, not the smallest total duration. BFS is the right algorithm for minimum-edge distance in an unweighted sense.

### Why use Dijkstra’s for routing and power analysis?

Whenever total edge weights matter, BFS is no longer enough. Dijkstra’s supports weighted shortest paths and is a natural fit for travel duration and wire length calculations.

---

## Running and Testing

This project includes JUnit-based student tests in the `tst/` directory and a JUnit standalone jar in `lib/`.

Typical workflow:

1. compile the source files
2. run the JUnit tests
3. verify graph operations and WaddleWorks functionality

The provided scripts:

* `submit-unix.sh`
* `submit-windows.bat`

are meant for packaging/submission convenience.

---

## Summary

This project is a strong graph-and-data-structures application built around:

* adjacency-list graph representation
* union-find / disjoint set
* BFS and DFS traversals
* Dijkstra’s shortest paths
* Prim’s and Kruskal’s MST algorithms

On top of that foundation, the WaddleWorks layer models road planning and electrical-grid optimization in a way that connects the theory from class to a more practical simulation.

The result is a project that demonstrates both:

* **core data structures knowledge**
* **how those structures solve realistic system design problems**

```