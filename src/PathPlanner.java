import java.util.*;
import java.util.List;


public class PathPlanner {


    public static boolean stabled = false; // 是否稳定
    // 路径信息类
    static class PathInfo {
        double cost;                // 从起点到当前点的总成本
        List<PixelNode> path;       // 路径节点列表
        
        // 构造函数
        public PathInfo(double cost, List<PixelNode> path) {
            this.cost = cost;
            this.path = path;
        }
    }


    public static List<PixelNode> computeShortestPathToTarget(
            Map<String, PixelNode> graph, PixelNode seed, PixelNode target) {
        stabled=false;
//        System.out.println("11"+stabled);

        String targetKey = target.x + "," + target.y;

        // 最短路径表
        Map<String, Double> costMap = new HashMap<>();
        Map<String, List<PixelNode>> pathMap = new HashMap<>();

        PriorityQueue<PixelNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(n -> costMap.getOrDefault(n.x + "," + n.y, Double.POSITIVE_INFINITY))
        );

        Set<String> visited = new HashSet<>();

        String seedKey = seed.x + "," + seed.y;
        costMap.put(seedKey, 0.0);
        pathMap.put(seedKey, new ArrayList<>(List.of(seed)));
        queue.add(seed);

        while (!queue.isEmpty()) {
            PixelNode current = queue.poll();
            String currentKey = current.x + "," + current.y;

            // 如果到达终点，立即返回
            if (currentKey.equals(targetKey)) {
                double minCost = costMap.get(targetKey);  // 获取目标节点的最小代价
                int totalPoints = pathMap.get(targetKey).size();
                if (minCost/totalPoints<1.6&&totalPoints>50) {
                    // stable status
                    stabled = true;
                }
                return pathMap.get(currentKey);
            }

            if (visited.contains(currentKey)) continue;
            visited.add(currentKey);

            double currentCost = costMap.get(currentKey);
            List<PixelNode> currentPath = pathMap.get(currentKey);

            for (PixelNode.Neighbor neighbor : current.neighbors) {
                PixelNode next = neighbor.node;
                String nextKey = next.x + "," + next.y;

                if (visited.contains(nextKey)) continue;

                double moveCost = neighbor.link_cost;
                if (isDiagonal(current, next)) {
                    moveCost *= Math.sqrt(2);
                }

                double newCost = currentCost + moveCost;
                double existingCost = costMap.getOrDefault(nextKey, Double.POSITIVE_INFINITY);

                if (newCost < existingCost) {
                    costMap.put(nextKey, newCost);
                    List<PixelNode> newPath = new ArrayList<>(currentPath);
                    newPath.add(next);
                    pathMap.put(nextKey, newPath);
                    queue.add(next);
                }
            }
        }

        // 如果找不到路径，返回空列表
        return new ArrayList<>();
    }


    // 判断两个节点是否为对角线关系
    private static boolean isDiagonal(PixelNode a, PixelNode b) {
        return a.x != b.x && a.y != b.y;
    }
}