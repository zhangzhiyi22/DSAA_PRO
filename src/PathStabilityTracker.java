import java.util.List;


/**
 * 路径稳定性跟踪器：负责分析路径稳定性和自动生成种子点
 */
public class PathStabilityTracker {
    // 状态变量
    private boolean isPathStable = false;
    private int stableFrameCount = 0; // 连续稳定帧计数
    
    private final PathAnalyzer pathAnalyzer;
    
    /**
     * 构造函数
     */
    public PathStabilityTracker(EdgeDetector edgeDetector) {
        this.pathAnalyzer = new PathAnalyzer();
    }
    
    /**
     * 分析路径稳定性
     * @param path 要分析的路径
     * @return 路径是否稳定
     */
    public boolean analyzePathStability(List<PixelNode> path) {
        if (!pathAnalyzer.isPathValid(path)) {
            resetStability();
            return false;
        }

        // 通过PathAnalyzer分析当前路径稳定性
        boolean currentlyStable = pathAnalyzer.isPathStable(path);
        
        // 平滑稳定性判断 - 需要连续几帧都稳定才真正标记为稳定
        if (currentlyStable) {
            stableFrameCount++;
            if (stableFrameCount >= 3) { // 需要连续3帧稳定
                isPathStable = true;
            }
        } else {
            stableFrameCount = 0;
            isPathStable = false;
        }
        
        return isPathStable;
    }

    
    /**
     * 重置稳定性状态
     */
    public void resetStability() {
        isPathStable = false;
        stableFrameCount = 0;
    }
    
    /**
     * 获取当前路径是否稳定
     */
    public boolean isPathStable() {
        return isPathStable;
    }
}

/**
 * 路径分析器：负责路径的有效性和稳定性分析
 */


/**
 * 种子点生成器：负责生成智能种子点
 */

