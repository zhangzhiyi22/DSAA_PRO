import java.util.List;

/**
 * 稳定性指标类 - 存储路径的统计数据
 */
/**
 * 路径分析器 - 负责分析路径的稳定性与质量
 */
public class PathAnalyzer {
    // 配置参数
    private static final double STABILITY_THRESHOLD = 1.8; // 代价稳定性阈值
    private static final double DIRECTION_THRESHOLD = 0.4; // 方向稳定性阈值
    private static final int MIN_PATH_LENGTH = 50; // 最小路径长度要求
    
    /**
     * 检查路径是否有效
     */
    public boolean isPathValid(List<PixelNode> path) {
        return path != null && path.size() >= MIN_PATH_LENGTH;
    }
    
    /**
     * 判断路径是否稳定
     */
    public boolean isPathStable(List<PixelNode> path) {
        StabilityMetrics metrics = calculatePathMetrics(path);
        
        boolean isStable = metrics.getAvgCost() < STABILITY_THRESHOLD && 
                            metrics.getAvgDirection() < DIRECTION_THRESHOLD;
        
        // 输出调试信息
        System.out.println(metrics + ", Stable: " + isStable);
        
        return isStable;
    }
    
    /**
     * 计算路径指标
     */
    public StabilityMetrics calculatePathMetrics(List<PixelNode> path) {
        double totalCost = 0;
        double totalDirection = 0;
        
        // 计算代价和方向变化
        for (int i = 1; i < path.size(); i++) {
            PixelNode prev = path.get(i-1);
            PixelNode curr = path.get(i);

            // 计算代价
            for (PixelNode.Neighbor neighbor : prev.neighbors) {
                if (neighbor.node.x == curr.x && neighbor.node.y == curr.y) {
                    totalCost += neighbor.link_cost;
                    break;
                }
            }

            // 如果有至少3个点，计算方向变化
            if (i > 1) {
                PixelNode prevprev = path.get(i-2);
                double angle1 = Math.atan2(prev.y - prevprev.y, prev.x - prevprev.x);
                double angle2 = Math.atan2(curr.y - prev.y, curr.x - prev.x);
                double angleDiff = Math.abs(angle2 - angle1);
                if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                totalDirection += angleDiff;
            }
        }
        
        // 计算平均值
        double avgCost = totalCost / path.size();
        double avgDirection = path.size() > 2 ? totalDirection / (path.size() - 2) : 0;
        
        return new StabilityMetrics(avgCost, avgDirection, path.size());
    }
}
