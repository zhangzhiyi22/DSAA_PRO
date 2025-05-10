import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * UI管理器：负责界面交互和事件处理
 */
public class UIManager {
    private final Main mainFrame;
    private final ImageProcessor imageProcessor;
    private final PathManager pathManager;
    
    public UIManager(Main mainFrame, ImageProcessor imageProcessor, PathManager pathManager) {
        this.mainFrame = mainFrame;
        this.imageProcessor = imageProcessor;
        this.pathManager = pathManager;
    }
    
    /**
     * 创建按钮面板
     */
    public JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        
        JButton resetButton = new JButton("重置点");
        resetButton.addActionListener(e -> pathManager.resetAll());
        
        JButton openButton = new JButton("打开文件");
        openButton.addActionListener(e -> openImage());
        
        buttonPanel.add(resetButton);
        buttonPanel.add(openButton);
        
        return buttonPanel;
    }
    
    /**
     * 绑定鼠标事件监听器
     */
    public void bindMouseListeners() {
        JLabel imageLabel = mainFrame.getImageLabel();
        
        // 鼠标点击监听器
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isLeftClickValid(e)) return;
                
                handleMouseClick(e);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                imageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                imageLabel.setCursor(Cursor.getDefaultCursor());
            }
        });
        
        // 鼠标移动监听器
        imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!mainFrame.isImageReady() || !pathManager.hasStartPoint()) return;
                
                handleMouseMove(e);
            }
        });
    }
    
    /**
     * 处理鼠标点击事件
     */
    private void handleMouseClick(MouseEvent e) {
        Point imagePoint = imageProcessor.convertPointToImageCoordinates(e.getPoint());
        if (imagePoint == null) return;
        
        // 如果按Ctrl并有推荐点，使用推荐点
        Point targetPoint = imagePoint;
        if (pathManager.hasSuggestedPoint() && e.isControlDown()) {
            targetPoint = pathManager.getSuggestedPoint();
            mainFrame.getStatusLabel().setText("已接受建议点");
        } else if (pathManager.hasSnappedPoint()) {
            targetPoint = pathManager.getCurrentSnappedPoint();
        }
        
        // 处理点击位置
        pathManager.handleClickAtPoint(targetPoint);
    }
    
    /**
     * 处理鼠标移动事件
     */
    private void handleMouseMove(MouseEvent e) {
        Point imagePoint = imageProcessor.convertPointToImageCoordinates(e.getPoint());
        if (imagePoint == null) return;
        
        pathManager.handleMouseMove(imagePoint);
    }
    
    /**
     * 打开图像文件
     */
    private void openImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择图像文件");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "图像文件 (JPG, PNG)", "jpg", "jpeg", "png"));
        fileChooser.setCurrentDirectory(new File("."));
        
        int result = fileChooser.showOpenDialog(mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String imagePath = selectedFile.getAbsolutePath();
            
            try {
                imageProcessor.loadImage(imagePath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame, "打开图像失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 判断左键点击是否有效
     */
    private boolean isLeftClickValid(MouseEvent e) {
        return e.getButton() == MouseEvent.BUTTON1 && mainFrame.isImageReady();
    }
}
