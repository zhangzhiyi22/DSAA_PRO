import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * 图像处理器：负责图像加载、处理和渲染
 */
public class ImageProcessor {
    private final Main mainFrame;
    
    private BufferedImage originalImage;
    private BufferedImage displayImage;
    private CostGraphGenerator costGraphGenerator;
    private double[][] costImage;
    
    /**
     * 构造函数
     */
    public ImageProcessor(Main mainFrame) {
        this.mainFrame = mainFrame;
    }
    
    /**
     * 加载图像
     */
    public void loadImage(String imagePath) {
        try {
            File imgFile = new File(imagePath);
            if (!imgFile.exists()) {
                throw new IOException("找不到图片文件: " + imagePath);
            }
            
            originalImage = ImageIO.read(imgFile);
            if (originalImage == null) {
                throw new IOException("无法读取图片");
            }
            
            // 创建显示用的图像副本
            updateDisplayImage();
            
            // 生成代价图和图结构
            costGraphGenerator = new CostGraphGenerator(imagePath);
            costImage = costGraphGenerator.getCostImage();
            
            // 更新UI和状态
            mainFrame.getStatusLabel().setText("图片已加载，请点击设置起点");
            mainFrame.setImageReady(true);
            
            // 重置路径管理器
            mainFrame.getPathManager().init(costGraphGenerator.costImageToGraph(costImage));
            
        } catch (Exception ex) {
            mainFrame.getStatusLabel().setText("图片加载失败: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "无法加载图片: " + ex.getMessage());
        }
    }
    
    /**
     * 更新显示图像
     */
    public void updateDisplayImage() {
        if (originalImage == null) return;
        
        displayImage = deepCopy(originalImage);
        mainFrame.getImageLabel().setIcon(new ImageIcon(displayImage));
    }

    
    /**
     * 将点击坐标转换为图像坐标
     */
    public Point convertPointToImageCoordinates(Point clickPoint) {
        if (originalImage == null || mainFrame.getImageLabel().getIcon() == null) return null;
        
        Rectangle imageBounds = getImageBounds();
        
        if (!imageBounds.contains(clickPoint)) return null;
        
        int imageX = (int) ((clickPoint.x - imageBounds.x) * originalImage.getWidth() / imageBounds.width);
        int imageY = (int) ((clickPoint.y - imageBounds.y) * originalImage.getHeight() / imageBounds.height);
        
        // 边界检查
        imageX = Math.max(0, Math.min(imageX, originalImage.getWidth() - 1));
        imageY = Math.max(0, Math.min(imageY, originalImage.getHeight() - 1));
        
        return new Point(imageX, imageY);
    }
    
    /**
     * 获取图像显示区域的边界
     */
    private Rectangle getImageBounds() {
        JLabel imageLabel = mainFrame.getImageLabel();
        Icon icon = imageLabel.getIcon();
        int imageWidth = icon.getIconWidth();
        int imageHeight = icon.getIconHeight();
        
        double scale = 1.0;
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        
        if (imageWidth > labelWidth || imageHeight > labelHeight) {
            double scaleX = (double) labelWidth / imageWidth;
            double scaleY = (double) labelHeight / imageHeight;
            scale = Math.min(scaleX, scaleY);
        }
        
        int displayWidth = (int) (imageWidth * scale);
        int displayHeight = (int) (imageHeight * scale);
        
        int x = (labelWidth - displayWidth) / 2;
        int y = (labelHeight - displayHeight) / 2;
        
        return new Rectangle(x, y, displayWidth, displayHeight);
    }
    
    /**
     * 提取图像
     */
    public void extractImage(List<List<PixelNode>> confirmedPaths) {
        try {
            BufferedImage extractedImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            
            // 创建掩码
            boolean[][] mask = createPathMask(confirmedPaths);
            
            // 应用掩码提取图像
            for (int x = 0; x < originalImage.getWidth(); x++) {
                for (int y = 0; y < originalImage.getHeight(); y++) {
                    if (mask[x][y]) {
                        extractedImage.setRGB(x, y, originalImage.getRGB(x, y));
                    } else {
                        extractedImage.setRGB(x, y, 0);
                    }
                }
            }
            
            // 显示结果
            mainFrame.getImageLabel().setIcon(new ImageIcon(extractedImage));
            mainFrame.getStatusLabel().setText("抠图完成");
            
            // 询问是否保存
            askToSaveImage(extractedImage);
            
        } catch (Exception ex) {
            ex.printStackTrace();
            mainFrame.getStatusLabel().setText("抠图失败: " + ex.getMessage());
        }
    }
    
    /**
     * 创建路径掩码
     */
    private boolean[][] createPathMask(List<List<PixelNode>> confirmedPaths) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        // 创建二值图像表示边界
        BufferedImage boundaryImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = boundaryImage.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        
        // 绘制边界
        for (List<PixelNode> path : confirmedPaths) {
            for (PixelNode node : path) {
                boundaryImage.setRGB(node.x, node.y, Color.WHITE.getRGB());
            }
        }
        g.dispose();
        
        // 创建掩码并填充
        boolean[][] mask = new boolean[width][height];
        Point seedPoint = findSeedPoint(confirmedPaths);
        if (seedPoint != null) {
            floodFill(seedPoint.x, seedPoint.y, boundaryImage, mask);
        }
        
        return mask;
    }
    
    /**
     * 寻找种子点（使用路径的质心）
     */
    private Point findSeedPoint(List<List<PixelNode>> paths) {
        int sumX = 0, sumY = 0;
        int count = 0;
        
        for (List<PixelNode> path : paths) {
            for (PixelNode node : path) {
                sumX += node.x;
                sumY += node.y;
                count++;
            }
        }
        
        if (count > 0) {
            return new Point(sumX / count, sumY / count);
        }
        
        return null;
    }
    
    /**
     * 洪水填充算法
     */
    private void floodFill(int x, int y, BufferedImage boundary, boolean[][] mask) {
        int width = boundary.getWidth();
        int height = boundary.getHeight();
        
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(x, y));
        
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            x = p.x;
            y = p.y;
            
            if (x < 0 || x >= width || y < 0 || y >= height ||
                    mask[x][y] || boundary.getRGB(x, y) == Color.WHITE.getRGB()) {
                continue;
            }
            
            mask[x][y] = true;
            
            queue.add(new Point(x + 1, y));
            queue.add(new Point(x - 1, y));
            queue.add(new Point(x, y + 1));
            queue.add(new Point(x, y - 1));
        }
    }
    
    /**
     * 询问是否保存图像
     */
    private void askToSaveImage(BufferedImage extractedImage) {
        int option = JOptionPane.showConfirmDialog(
                mainFrame,
                "是否保存抠图结果？",
                "保存抠图",
                JOptionPane.YES_NO_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            saveImageToFile(extractedImage);
        } else if (option == JOptionPane.NO_OPTION) {
            mainFrame.getPathManager().resetAll();
        }
    }
    
    /**
     * 保存图像到文件
     */
    private void saveImageToFile(BufferedImage image) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存抠图结果");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG图像", "png"));
        fileChooser.setCurrentDirectory(new File("."));
        
        if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".png")) {
                path += ".png";
            }
            
            try {
                ImageIO.write(image, "png", new File(path));
                mainFrame.getStatusLabel().setText("图像已保存至: " + path);
            } catch (IOException ex) {
                ex.printStackTrace();
                mainFrame.getStatusLabel().setText("保存失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 创建图像深拷贝
     */
    private BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage copy = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return copy;
    }
    
    /**
     * Getter方法
     */
    public BufferedImage getOriginalImage() {
        return originalImage;
    }
    
    public BufferedImage getDisplayImage() {
        return displayImage;
    }
    
    public double[][] getCostImage() {
        return costImage;
    }
    
    public CostGraphGenerator getCostGraphGenerator() {
        return costGraphGenerator;
    }
}
