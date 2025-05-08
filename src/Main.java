import java.awt.image.BufferedImage;
import java.util.*;


public class Main {
    public static void main(String[] args) throws Exception {
        String path = "Images/img2.png"; // 图片路径
        CostGraphGenerator costGraphGenerator = new CostGraphGenerator(path); // 创建图生成器对象
        double[][] costImage = costGraphGenerator.getCostImage(); // 获取代价图

        // System.out.println(Arrays.deepToString(costImage));

        BufferedImage img = costGraphGenerator.visualizeCostGraph(costImage); // 可视化代价图
        costGraphGenerator.saveImage(img, "cost_graph.png"); // 存储代价图
        Map<String, PixelNode> costGraph = costGraphGenerator.costImageToGraph(costImage); // 将代价图转换为图
        PixelNode node0 = costGraph.get("0,0"); // 获取节点
        for (PixelNode.Neighbor neighbor : node0.neighbors) { // 遍历邻居
            System.out.println("Neighbor: " + neighbor.node.x + "," + neighbor.node.y + ", Cost: " + neighbor.link_cost);
        }


        // 获取图像尺寸
        int width = costGraphGenerator.getWidth();
        int height = costGraphGenerator.getHeight();
        
        // 定义起点
        int startX = width / 3;
        int startY = height / 2;
        String startKey = startX + "," + startY;
        
        // 定义终点
        int endX = width * 4 / 9;
        int endY = height * 15 / 18;
        String endKey = endX + "," + endY;
        
        System.out.println("起点坐标: (" + startX + "," + startY + ")");
        System.out.println("终点坐标: (" + endX + "," + endY + ")");
        
        // 测试最小成本路径算法
        PixelNode seed = costGraph.get(startKey);
        Map<String, PathPlanner.PathInfo> pathMap = PathPlanner.computeMinimumCostPath(costGraph, seed);
        
        // 获取到终点的路径信息
        PathPlanner.PathInfo pathInfo = pathMap.get(endKey);
        
        System.out.println("从(" + startX + "," + startY + ")到(" + endX + "," + endY + ")的最小成本: " + pathInfo.cost);
        System.out.println("路径长度: " + pathInfo.path.size() + "个节点");
        
        // 可视化并保存路径图像
        BufferedImage pathImage = costGraphGenerator.visualizePath(costImage, pathInfo.path);
        costGraphGenerator.saveImage(pathImage, "bird_path.png");

        
    }
}