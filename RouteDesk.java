import java.nio.file.*;
import java.util.*;

/** Plan trips on a fictional campus with undirected walking paths. */
public class RouteDesk {
    record Edge(String to, int minutes, boolean stairs) {}
    record Visit(String stop, long minutes) {}
    record Route(List<String> stops, long minutes) {}

    static Map<String, List<Edge>> load(Path file) throws Exception {
        var lines = Files.readAllLines(file);
        if (lines.isEmpty() || !lines.get(0).equals("from,to,minutes,stairs"))
            throw new IllegalArgumentException("Expected header from,to,minutes,stairs");
        var graph = new TreeMap<String, List<Edge>>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            var f = lines.get(i).split(",", -1);
            if (f.length != 4) throw new IllegalArgumentException("Expected four fields on line " + (i+1));
            for (int j = 0; j < f.length; j++) f[j] = f[j].trim();
            int minutes = Integer.parseInt(f[2]);
            if (minutes < 1 || f[0].isEmpty() || f[1].isEmpty() || !Set.of("true","false").contains(f[3]))
                throw new IllegalArgumentException("Invalid path on line " + (i+1));
            boolean stairs = Boolean.parseBoolean(f[3]);
            // The CSV lists each walkway once. People can walk it in either direction.
            graph.computeIfAbsent(f[0], k -> new ArrayList<>()).add(new Edge(f[1], minutes, stairs));
            graph.computeIfAbsent(f[1], k -> new ArrayList<>()).add(new Edge(f[0], minutes, stairs));
        }
        return graph;
    }

    static Optional<Route> plan(Map<String,List<Edge>> graph, String start, String end,
                                boolean stepFree, Set<String> closed) {
        var required = new HashSet<>(closed);
        required.add(start);
        required.add(end);
        for (var stop : required)
            if (!graph.containsKey(stop)) throw new IllegalArgumentException("Unknown stop: " + stop);
        if (closed.contains(start) || closed.contains(end)) return Optional.empty();
        var best = new HashMap<String,Long>();
        var previous = new HashMap<String,String>();
        // A short walk can have more stops. Compare total minutes when choosing where to go next.
        var queue = new PriorityQueue<>(Comparator.comparingLong(Visit::minutes).thenComparing(Visit::stop));
        queue.add(new Visit(start, 0));
        best.put(start, 0L);
        while (!queue.isEmpty()) {
            var current = queue.remove();
            // We may have found a quicker way here since this visit was queued.
            if (current.minutes() != best.get(current.stop())) continue;
            if (current.stop().equals(end)) {
                var stops = new LinkedList<String>();
                // Follow the previous stops backward from the destination to rebuild the itinerary.
                for (String s = end; s != null; s = previous.get(s)) stops.addFirst(s);
                return Optional.of(new Route(List.copyOf(stops), current.minutes()));
            }
            for (var edge : graph.get(current.stop())) {
                if (closed.contains(edge.to()) || (stepFree && edge.stairs())) continue;
                long next = current.minutes() + edge.minutes();
                if (next < best.getOrDefault(edge.to(), Long.MAX_VALUE)) {
                    best.put(edge.to(), next);
                    previous.put(edge.to(), current.stop());
                    queue.add(new Visit(edge.to(), next));
                }
            }
        }
        return Optional.empty();
    }

    static void run(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException(
            "Usage: java RouteDesk.java FROM TO [--step-free] [--closed=STOP] [--map=FILE]");
        var closed = new HashSet<String>();
        boolean stepFree = false;
        Path file = Path.of("paths.csv");
        for (int i = 2; i < args.length; i++) {
            String option = args[i];
            if (option.equals("--step-free")) stepFree = true;
            else if (option.startsWith("--closed=")) closed.add(option.substring(9));
            else if (option.startsWith("--map=")) file = Path.of(option.substring(6));
            else throw new IllegalArgumentException("Unknown option: " + option);
        }
        var graph = load(file);
        var route = plan(graph, args[0], args[1], stepFree, closed);
        if (route.isEmpty()) {
            System.out.println("No route available under these conditions.");
            return;
        }
        var result = route.get();
        System.out.println(String.join(" -> ", result.stops()) + " | " + result.minutes() + " min");
        // Compare the detour with the fastest walk on a day when every path is open.
        long baseline = plan(graph, args[0], args[1], false, Set.of()).orElseThrow().minutes();
        System.out.println("Extra time versus unrestricted route: " + (result.minutes()-baseline) + " min");
    }

    public static void main(String[] args) {
        try { run(args); }
        catch (Exception e) { System.err.println(e.getMessage()); System.exit(2); }
    }
}
