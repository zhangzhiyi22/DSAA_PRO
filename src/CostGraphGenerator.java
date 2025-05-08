import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;


// 将图片转换为一个代价图片的类
public class CostGraphGenerator {
    // Sobel 算子，对应指引中的S_x和S_y
    private static final int[][] s_x = {{-1, 0 ,1}, {-2, 0, 2}, {-1, 0, 1}};
    private static final int[][] s_y = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
    private final BufferedImage img; // 图片对象
    private final int width; // 图片宽度
    private final int height; // 图片高度

    // 构造函数，初始化图片信息
    public CostGraphGenerator(String imgPath) throws Exception {
        this.img = ImageIO.read(new File(imgPath)); // 读取图片
        this.width = img.getWidth(); // 获取图片宽度
        this.height = img.getHeight(); // 获取图片高度
    }

    public int getWidth() {
        return this.width;
    }
    
    public int getHeight() {
        return this.height;
    }
    

    // 获取代价矩阵
    public double[][] getCostImage() {
        double[][] costImage = new double[this.width][this.height]; // 创建一个二维数组来存储代价值
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                // 初始化三个颜色通道的x,y分量梯度
                double r_gx = 0, r_gy = 0;
                double g_gx = 0, g_gy = 0;
                double b_gx = 0, b_gy = 0;
                // 遍历3x3的邻域进行卷积
                for (int k = -1; k <= 1; k++) {
                    for (int l = -1; l <= 1; l++) {
                        // 确保不越界
                        if (i + k >= 0 && i + k < this.width && j + l >= 0 && j + l < this.height) {
                            int rgb = this.img.getRGB(i + k, j + l); // 获取像素的RGB值
                            int r = (rgb >> 16) & 0xff; // 提取红色分量
                            int g = (rgb >> 8) & 0xff; // 提取绿色分量
                            int b = rgb & 0xff; // 提取蓝色分量

                            r_gx += s_x[k + 1][l + 1] * r; // 红色分量的梯度
                            r_gy += s_y[k + 1][l + 1] * r; // 红色分量的梯度
                            g_gx += s_x[k + 1][l + 1] * g; // 绿色分量的梯度
                            g_gy += s_y[k + 1][l + 1] * g; // 绿色分量的梯度
                            b_gx += s_x[k + 1][l + 1] * b; // 蓝色分量的梯度
                            b_gy += s_y[k + 1][l + 1] * b; // 蓝色分量的梯度
                        }
                    }
                }
                double G_r = Math.sqrt(r_gx * r_gx + r_gy * r_gy); // 计算红色分量的梯度幅值
                double G_g = Math.sqrt(g_gx * g_gx + g_gy * g_gy); // 计算绿色分量的梯度幅值
                double G_b = Math.sqrt(b_gx * b_gx + b_gy * b_gy); // 计算蓝色分量的梯度幅值

                costImage[i][j] = Math.sqrt(G_r * G_r + G_g * G_g + G_b * G_b); // 计算总的代价值
            }
        }
        // 归一化代价矩阵到 [0, 1]
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                double val = costImage[i][j];
                if (val > max) max = val;
                if (val < min) min = val;
            }
        }

        // 边缘部分代价小，使用1-costGraph[i][j]来表示
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                costImage[i][j] = 1-(costImage[i][j] - min) / (max - min + 1e-8); // 归一化
            }
        }
        return costImage; // 返回代价矩阵
    }

    //将代价矩阵转换为图，每条边的值 link_cost = costGraph(x) + costGraph，使用HashMap存储<坐标，节点>
    public Map<String, PixelNode> costImageToGraph (double[][] costGraph){
        Map<String, PixelNode> graph = new java.util.HashMap<>();
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                PixelNode node = new PixelNode(i, j);
                graph.put(i + "," + j, node); // 将节点添加到图中
            }
        }
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                PixelNode node = graph.get(i + "," + j);
                // 遍历邻居
                for (int k = -1; k <= 1; k++) {
                    for (int l = -1; l <= 1; l++) {
                        if (k == 0 && l == 0) continue;
                        int ni = i + k;
                        int nj = j + l;
                        if (ni >= 0 && ni < this.width && nj >= 0 && nj < this.height) {
                            PixelNode neighbor = graph.get(ni + "," + nj);
                            double link_cost = costGraph[i][j] + costGraph[ni][nj]; // 计算边的代价
                            node.neighbors.add(new PixelNode.Neighbor(neighbor, link_cost)); // 添加邻居
                        }
                    }
                }
            }
        }
        return graph;
    }

    // 可视化代价矩阵，用于测试
    public BufferedImage visualizeCostGraph(double[][] costGraph) {
        int width = costGraph.length;
        int height = costGraph[0].length;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 归一化到 [0, 255]，值越小代表越像边缘（成本低）
                int gray = (int) (255 * (costGraph[i][j]));
                gray = Math.max(0, Math.min(255, gray));
                int rgb = new Color(gray, gray, gray).getRGB();
                img.setRGB(i, j, rgb);
            }
        }
        return img;
    }

    // 保存图片
    public void saveImage(BufferedImage img, String filename) {
        try {
            File output = new File(filename);
            ImageIO.write(img, "png", output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public BufferedImage visualizePath(double[][] costImage, List<PixelNode> path) {
    
        BufferedImage pathImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // 将代价值转换为灰度值（0-255）
                int grayValue = (int)(costImage[x][y] * 255);
                int rgb = new Color(grayValue, grayValue, grayValue).getRGB();
                pathImage.setRGB(x, y, rgb);
            }
        }
        
        // 绘制路径
        Color pathColor = Color.RED;
        for (PixelNode node : path) {
            if (node.x >= 0 && node.x < width && node.y >= 0 && node.y < height) {
                pathImage.setRGB(node.x, node.y, pathColor.getRGB());
            }
        }
        
        // 标记起点和终点
        if (!path.isEmpty()) {
            // 起点
            PixelNode start = path.get(0);
            markPoint(pathImage, start.x, start.y, Color.GREEN, 2);
            
            // 终点
            PixelNode end = path.get(path.size() - 1);
            markPoint(pathImage, end.x, end.y, Color.BLUE, 2);
        }
        
        return pathImage;
    }

    private void markPoint(BufferedImage image, int x, int y, Color color, int size) {
        int radius = size / 2;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int px = x + i;
                int py = y + j;
                if (px >= 0 && px < width && py >= 0 && py < height) {
                    image.setRGB(px, py, color.getRGB());
                }
            }
        }
    }
}


