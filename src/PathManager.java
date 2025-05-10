import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 路径管理器：负责路径的创建、更新和管理
 */
public class PathManager {
    private final Main mainFrame;
    private final PathStabilityTracker stabilityTracker;
    private final EdgeDetector edgeDetector;
    
    // 路径数据
    private Map<String, PixelNode> costGraph;
    private List<List<PixelNode>> confirmedPaths = new ArrayList<>();
    private Point lastConfirmedPoint = null;
    private Point firstPoint = null;
    private Point currentSnappedPoint = null;
    private Point suggestedNextPoint = null;
    
    // 路径状态
    private boolean isClosable = false;
    private static final int CLOSE_PATH_THRESHOLD = 15; // 闭合判定阈值
    private static final int SNAP_RADIUS = 9; // 吸附搜索半径

    private static final long STABILITY_MESSAGE_DURATION = 2000; // 0.5秒显示时间
    private Timer stabilityTimer;
    private boolean showingStabilityMessage = false;
    
    /**
     * 构造函数
     */
    public PathManager(Main mainFrame, PathStabilityTracker stabilityTracker, EdgeDetector edgeDetector) {
        this.mainFrame = mainFrame;
        this.stabilityTracker = stabilityTracker;
        this.edgeDetector = edgeDetector;
    }
    
    /**
     * 初始化路径管理器
     */
    public void init(Map<String, PixelNode> costGraph) {
        this.costGraph = costGraph;
        resetAll();
    }
    
    /**
     * 处理点击事件
     */
    public void handleClickAtPoint(Point targetPoint) {
        if (!isValidPoint(targetPoint)) return;
        
        String key = pointToKey(targetPoint);
        
        // 如果是第一个点
        if (lastConfirmedPoint == null) {
            lastConfirmedPoint = firstPoint = targetPoint;
            mainFrame.getStatusLabel().setText("起点已设置 (" + key + ")");
            return;
        }
        
        try {
            PixelNode start = getNode(lastConfirmedPoint);
            PixelNode end = isClosable ? getNode(firstPoint) : getNode(currentSnappedPoint);
            List<PixelNode> path = PathPlanner.computeShortestPathToTarget(costGraph, start, end);
            
            if (path.isEmpty()) {
                mainFrame.getStatusLabel().setText("路径不可达");
                return;
            }
            
            confirmedPaths.add(path);
            lastConfirmedPoint = currentSnappedPoint;
            
            if (isClosable) {
                mainFrame.getStatusLabel().setText("路径已闭合，正在抠图...");
                mainFrame.getImageProcessor().extractImage(confirmedPaths);
            } else {
                updatePreviewWithPath(null, false);
                mainFrame.getStatusLabel().setText("路径已确认至 (" + key + ")");
            }
        } catch (Exception ex) {
            mainFrame.getStatusLabel().setText((isClosable ? "闭合路径失败: " : "路径确认失败: ") + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * 处理鼠标移动
     */
    public void handleMouseMove(Point imagePoint) {
        if (costGraph == null || lastConfirmedPoint == null) return;

        // 尝试找到边缘点
        double[][] costImage = mainFrame.getImageProcessor().getCostImage();
        Point snapped = edgeDetector.findBestEdgeFromCost(imagePoint, SNAP_RADIUS, costImage);

        if (snapped != null) {
            currentSnappedPoint = snapped;

            // 根据当前点和上一个确认点计算临时路径
            PixelNode start = getNode(lastConfirmedPoint);
            PixelNode end = getNode(currentSnappedPoint);
            List<PixelNode> tempPath = PathPlanner.computeShortestPathToTarget(costGraph, start, end);

            // 分析路径稳定性
            boolean isStable = stabilityTracker.analyzePathStability(tempPath);
            // 如果路径稳定，生成推荐点
            if (isStable) {
                confirmedPaths.add(tempPath);
                lastConfirmedPoint = currentSnappedPoint;
            }
            updatePreviewWithPath(tempPath, isStable);

            // 更新状态栏
            updateStatusBarForPath(tempPath);
        }

//         计算是否可以闭合路径
        Point target = currentSnappedPoint != null ? currentSnappedPoint : imagePoint;
        if (isValidPoint(target)) {
            try {
                PixelNode start = getNode(lastConfirmedPoint);
                PixelNode end = getNode(target);
                List<PixelNode> livePath = PathPlanner.computeShortestPathToTarget(costGraph, start, end);

                if (!livePath.isEmpty()) {
                    // 检查是否足够接近起点可以闭合
                    isClosable = firstPoint != null &&
                            confirmedPaths.size() >= 1 &&
                            target.distance(firstPoint) <= CLOSE_PATH_THRESHOLD;
                }
            } catch (Exception ex) {
                mainFrame.getStatusLabel().setText("路径预览失败: " + ex.getMessage());
            }
        }
    }

    // 右键撤销上一次操作
    public void handleRightClick(Point imagePoint) {
        if (isValidPoint(imagePoint)) {
            undoLastConfirmedPath();
        }
    }
    
    /**
     * 用临时路径和稳定状态更新预览
     */
    private void updatePreviewWithPath(List<PixelNode> tempPath, boolean isStable) {
        // 创建一个与原图大小相同的BufferedImage
        BufferedImage tempDisplay = new BufferedImage(
                mainFrame.getImageProcessor().getOriginalImage().getWidth(),
                mainFrame.getImageProcessor().getOriginalImage().getHeight(),
                BufferedImage.TYPE_INT_RGB);
        
        // 获取Graphics2D对象
        Graphics2D g = tempDisplay.createGraphics();
        // 将原图绘制到tempDisplay上
        g.drawImage(mainFrame.getImageProcessor().getOriginalImage(), 0, 0, null);


        // 首先绘制所有已确认的路径 (添加这部分代码)
        for (List<PixelNode> path : confirmedPaths) {
            for (PixelNode node : path) {
                // 将已确认的路径上的点设置为红色
                tempDisplay.setRGB(node.x, node.y, Color.RED.getRGB());
            }
        }

        // 然后绘制临时路径
        try {
            if (tempPath != null) {
                for (PixelNode node : tempPath) {
                    // 将临时路径上的点设置为蓝色
                    tempDisplay.setRGB(node.x, node.y, Color.BLUE.getRGB());
                }
            }
        } catch (Exception e) {
        }

        // 绘制当前吸附点
        if (currentSnappedPoint != null) {
            // 先绘制黄色外圈（更大更明显）
            g.setColor(new Color(255, 255, 0, 180));
            g.fillOval(currentSnappedPoint.x - 6, currentSnappedPoint.y - 6, 13, 13);

            // 然后绘制黄色实心
            g.setColor(Color.YELLOW);
            g.fillOval(currentSnappedPoint.x - 4, currentSnappedPoint.y - 4, 9, 9);
            System.out.println("Snapped Point: " + currentSnappedPoint);
        }


        // 释放Graphics2D对象
        g.dispose();
        // 将tempDisplay设置为ImageIcon，并显示在mainFrame的ImageLabel上
        mainFrame.getImageLabel().setIcon(new ImageIcon(tempDisplay));
    }
    
    /**
     * 更新状态栏显示路径信息
     */
    private void updateStatusBarForPath(List<PixelNode> path) {
        final int MIN_PATH_LENGTH = 50;
        StringBuilder status = new StringBuilder();
        
        if (path.size() >= MIN_PATH_LENGTH) {
            if (stabilityTracker.isPathStable()) {
                status.append("【路径稳定】");
                if (suggestedNextPoint != null) {
                    status.append(" ✓ 绿色点为建议点 - 按住Ctrl点击接受");
                }
            } else {
                status.append("沿边缘移动可触发自动建议点");
            }
            status.append(" | 路径长度: ").append(path.size());
        } else {
            status.append("继续沿边缘移动...(需要至少").append(MIN_PATH_LENGTH).append("个点)");
        }
        
        mainFrame.getStatusLabel().setText(status.toString());
    }
    
    /**
     * 重置所有路径和状态
     */
    public void resetAll() {
        lastConfirmedPoint = null;
        confirmedPaths.clear();
        firstPoint = null;
        isClosable = false;
        currentSnappedPoint = null;
        suggestedNextPoint = null;
        
        stabilityTracker.resetStability();
        
        if (mainFrame.getImageProcessor() != null) {
            mainFrame.getImageProcessor().updateDisplayImage();
        }
        mainFrame.getStatusLabel().setText("请点击图像设置起点");
    }

    public void undoLastConfirmedPath() {
        if (!confirmedPaths.isEmpty()) {
            // 移除最后一段路径
            confirmedPaths.remove(confirmedPaths.size() - 1);

            // 恢复上一个确认点
            if (confirmedPaths.isEmpty()) {
                lastConfirmedPoint = firstPoint = null;
                mainFrame.getStatusLabel().setText("已撤销所有路径，请重新设置起点");
            } else {
                // 获取最新路径的终点作为 lastConfirmedPoint
                List<PixelNode> lastPath = confirmedPaths.get(confirmedPaths.size() - 1);
                PixelNode lastNode = lastPath.get(lastPath.size() - 1);
                lastConfirmedPoint = new Point(lastNode.x, lastNode.y);
                mainFrame.getStatusLabel().setText("已撤销上一个路径");
            }

            // 更新预览显示
            updatePreviewWithPath(null, false);
        } else {
            mainFrame.getStatusLabel().setText("无可撤销路径");
        }
    }


    /**
     * 将点坐标转换为图中的键
     */
    private String pointToKey(Point p) {
        return p.x + "," + p.y;
    }
    
    /**
     * 从图中获取指定点的节点
     */
    private PixelNode getNode(Point p) {
        return costGraph.get(pointToKey(p));
    }
    
    /**
     * 判断点是否有效（在图中存在）
     */
    private boolean isValidPoint(Point p) {
        return p != null && costGraph != null && costGraph.containsKey(pointToKey(p));
    }
    
    /**
     * Getter方法
     */
    public boolean hasStartPoint() {
        return lastConfirmedPoint != null;
    }
    
    public boolean hasSnappedPoint() {
        return currentSnappedPoint != null;
    }
    
    public Point getCurrentSnappedPoint() {
        return currentSnappedPoint;
    }
    
    public boolean hasSuggestedPoint() {
        return suggestedNextPoint != null;
    }
    
    public Point getSuggestedPoint() {
        return suggestedNextPoint;
    }
}
