import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;


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

}


