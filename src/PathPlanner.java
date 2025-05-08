import java.util.*;


public class PathPlanner {
    
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
    
    // 计算从种子点到所有其他像素的最小成本路径
    public static Map<String, PathInfo> computeMinimumCostPath(Map<String, PixelNode> graph, PixelNode seed) {
        Map<String, PathInfo> pathMap = new HashMap<>();
        PriorityQueue<PixelNode> queue = new PriorityQueue<>(
            (a, b) -> Double.compare(
                pathMap.get(a.x + "," + a.y).cost,
                pathMap.get(b.x + "," + b.y).cost
            )
        );
        
        // 已处理节点集合
        Set<String> visited = new HashSet<>();
        
        // 初始化所有节点的路径信息
        for (String key : graph.keySet()) {
            if (key.equals(seed.x + "," + seed.y)) {
                // 种子节点到自身的成本为0
                List<PixelNode> seedPath = new ArrayList<>();
                seedPath.add(seed);
                pathMap.put(key, new PathInfo(0, seedPath));
            } else {
                // 其他节点初始化为无穷大
                pathMap.put(key, new PathInfo(Double.POSITIVE_INFINITY, new ArrayList<>()));
            }
        }
        
        // 将种子节点加入队列
        queue.add(seed);
        
        // Dijkstra算法主循环
        while (!queue.isEmpty()) {
            // 取出当前成本最小的节点
            PixelNode current = queue.poll();
            String currentKey = current.x + "," + current.y;
            
            // 如果节点已处理，跳过
            if (visited.contains(currentKey)) {
                continue;
            }

            visited.add(currentKey);
            
            // 当前节点的路径信息
            PathInfo currentInfo = pathMap.get(currentKey);
            
            // 检查所有邻居
            for (PixelNode.Neighbor neighbor : current.neighbors) {
                PixelNode next = neighbor.node;
                String nextKey = next.x + "," + next.y;
                
                // 已访问的节点跳过
                if (visited.contains(nextKey)) {
                    continue;
                }
                
                // 计算移动成本（对角线移动需要乘以根号2）
                double moveCost = neighbor.link_cost;
                if (isDiagonal(current, next)) {
                    moveCost *= Math.sqrt(2);
                }
                
                // 计算新路径总成本
                double newCost = currentInfo.cost + moveCost;
                
                // 如果找到更短路径，更新信息
                if (newCost < pathMap.get(nextKey).cost) {
                    // 创建新路径（复制当前路径并添加新节点）
                    List<PixelNode> newPath = new ArrayList<>(currentInfo.path);
                    newPath.add(next);
                    
                    // 更新路径信息
                    pathMap.put(nextKey, new PathInfo(newCost, newPath));
                    
                    // 将邻居加入队列
                    queue.add(next);
                }
            }
        }
        
        return pathMap;
    }
    
    // 判断两个节点是否为对角线关系
    private static boolean isDiagonal(PixelNode a, PixelNode b) {
        return a.x != b.x && a.y != b.y;
    }
}