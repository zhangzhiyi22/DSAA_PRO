/**
 * 路径稳定性指标类 - 存储路径分析的结果
 */
public class StabilityMetrics {
    private final double avgCost;
    private final double avgDirection;
    private final int pathLength;
    
    public StabilityMetrics(double avgCost, double avgDirection, int pathLength) {
        this.avgCost = avgCost;
        this.avgDirection = avgDirection;
        this.pathLength = pathLength;
    }
    
    public double getAvgCost() {
        return avgCost;
    }
    
    public double getAvgDirection() {
        return avgDirection;
    }
    
    public int getPathLength() {
        return pathLength;
    }
    
    @Override
    public String toString() {
        return "Path length: " + pathLength +
                ", Avg cost: " + avgCost +
                ", Avg direction: " + avgDirection;
    }
}
