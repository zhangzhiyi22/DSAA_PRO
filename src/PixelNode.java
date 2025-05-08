import java.util.*;

// 图节点类
public class PixelNode {
    // 图节点的坐标
    int x,y;
    // 节点的邻居列表
    ArrayList<Neighbor> neighbors = new ArrayList<>();

    // 构造函数
    public PixelNode(int x, int y) {
        this.x = x;
        this.y = y;
    }

    static class Neighbor {
        // 邻居节点
        PixelNode node;
        // 邻居节点的代价
        double link_cost;

        // 构造函数
        public Neighbor(PixelNode node, double link_cost) {
            this.node = node;
            this.link_cost = link_cost;
        }
    }
}


