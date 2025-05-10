import java.awt.*;

/**
 * 边缘检测类：负责边缘吸附和智能点推荐
 */
public class EdgeDetector {
    
    /**
     * 寻找最佳边缘点（吸附点）
     * @param center 中心点
     * @param radius 搜索半径
     * @param costImage 代价图
     * @return 找到的最佳边缘点
     */
    public Point findBestEdgeFromCost(Point center, int radius, double[][] costImage) {
        if (center == null || costImage == null) {
            return null;
        }
        
        int width = costImage.length;
        int height = costImage[0].length;
        
        // 确保中心点在图像范围内
        if (center.x < 0 || center.x >= width || center.y < 0 || center.y >= height) {
            return null;
        }

        Point bestPoint = null;
        double minCost = Double.POSITIVE_INFINITY;

        // 动态分析梯度，找出显著边缘
        double[][] gradientMagnitude = new double[2*radius+1][2*radius+1];
        double maxGradient = 0;

        // 第一步：计算邻域内的梯度幅值
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = center.x + dx;
                int y = center.y + dy;

                if (x < 0 || y < 0 || x >= width || y >= height) continue;

                // costImage的值越小表示边缘越显著
                gradientMagnitude[dx+radius][dy+radius] = 1 - costImage[x][y];
                maxGradient = Math.max(maxGradient, gradientMagnitude[dx+radius][dy+radius]);
            }
        }

        // 如果没有找到有效梯度，返回中心点
        if (maxGradient <= 0.001) {
            return new Point(center);
        }

        // 第二步：根据梯度强度设置动态阈值
        double threshold = maxGradient * 0.7; // 可调整阈值比例

        // 第三步：在显著边缘中找到最佳点
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = center.x + dx;
                int y = center.y + dy;

                if (x < 0 || y < 0 || x >= width || y >= height) continue;

                // 只考虑显著边缘点
                if (gradientMagnitude[dx+radius][dy+radius] >= threshold) {
                    if (costImage[x][y] < minCost) {
                        minCost = costImage[x][y];
                        bestPoint = new Point(x, y);
                    }
                }
            }
        }

        // 如果没有找到符合条件的边缘点，回退到原始算法
        if (bestPoint == null) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int x = center.x + dx;
                    int y = center.y + dy;

                    if (x < 0 || y < 0 || x >= width || y >= height) continue;

                    if (costImage[x][y] < minCost) {
                        minCost = costImage[x][y];
                        bestPoint = new Point(x, y);
                    }
                }
            }
        }

        return bestPoint;
    }
}
