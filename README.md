# RouteDesk · Campus route planner

A shortcut from the library to the lab takes five minutes and includes stairs. A visitor who needs a stair-free path can go through the garden in eight minutes. Close the garden and RouteDesk finds a third route. The map and travel times are fictional.

| Library to Lab | Route selected | Time |
|---|---|---:|
| All paths open | Library → Commons → Lab | 5 min |
| Avoid stairs | Library → Garden → Lab | 8 min |
| Avoid stairs and close Garden | Library → Commons → Studio → Lab | 9 min |

## Try it on Windows
Install a Java Development Kit (JDK) version 17 or newer. Extract the ZIP, open the RouteDesk folder in VS Code, then open Terminal → New Terminal. From the folder containing paths.csv, run:

```powershell
java RouteDesk.java Library Lab
java RouteDesk.java Library Lab --step-free
java RouteDesk.java Library Lab --step-free --closed=Garden
```

Each command prints the itinerary and the extra minutes compared with an unrestricted trip. Use stop names exactly as written. Repeat --closed=STOP to close multiple locations. To try your own dataset, pass --map=other.csv using the same header as paths.csv. Each row is a bidirectional edge with a positive integer travel time and a true/false stairs field. Stop names may contain spaces (quote them on the command line) but cannot contain commas.

## How it works
The Java program loads a CSV into an adjacency list. Dijkstra’s algorithm uses a priority queue ordered by elapsed time, skips stale queue entries, and records predecessors so it can reconstruct the actual path. During each search it skips edges with stairs when requested and excludes closed stops. A separate unrestricted run gives the comparison time. Positive edge weights make the first settled visit to the destination optimal.

With V stops and E paths, the search uses O((V + E) log(E + 1)) time and O(V + E) space. The queue can contain multiple entries for a stop when a shorter path is discovered.

## Verify
With Python 3 installed alongside the JDK, run the CLI regression suite:

```powershell
py -m unittest -v
```

Checks include baseline routing, stairs, closures, an unreachable destination, a zero-length trip, and an unknown stop.

**Interview discussion:** Why would a breadth-first search give the wrong answer on a map with unequal walking times? Add a direct Library-to-Lab path with a cost of 20 minutes to a copy of the CSV, then demonstrate the difference between fewest edges and least travel time.
