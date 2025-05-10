import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main extends JFrame {
    // UI组件
    private JLabel imageLabel;
    private JLabel statusLabel;
    
    // 核心管理器
    private UIManager uiManager;
    private ImageProcessor imageProcessor;
    private PathManager pathManager;
    
    // 状态数据
    private String imagePath;
    private boolean imageReady = false;
    
    /**
     * 构造函数
     */
    public Main(String imagePath) {
        this.imagePath = imagePath;
        setTitle("智能剪刀");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // 初始化组件
        initComponents();
        
        // 加载指定图像
        if (imagePath != null && !imagePath.isEmpty()) {
            imageProcessor.loadImage(imagePath);
        }
    }
    
    /**
     * 初始化所有组件
     */
    private void initComponents() {
        // 创建UI组件
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        
        statusLabel = new JLabel("请点击图像设置起点");
        add(statusLabel, BorderLayout.NORTH);
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);
        
        // 创建管理器
        EdgeDetector edgeDetector = new EdgeDetector();
        PathStabilityTracker stabilityTracker = new PathStabilityTracker(edgeDetector);
        
        imageProcessor = new ImageProcessor(this);
        pathManager = new PathManager(this, stabilityTracker, edgeDetector);
        uiManager = new UIManager(this, imageProcessor, pathManager);
        
        // 初始化按钮面板
        add(uiManager.createButtonPanel(), BorderLayout.SOUTH);
        
        // 绑定鼠标事件
        uiManager.bindMouseListeners();
    }
    
    // --- Getter/Setter 方法 ---
    
    public JLabel getImageLabel() {
        return imageLabel;
    }
    
    public JLabel getStatusLabel() {
        return statusLabel;
    }
    
    public boolean isImageReady() {
        return imageReady;
    }
    
    public void setImageReady(boolean imageReady) {
        this.imageReady = imageReady;
    }
    
    public ImageProcessor getImageProcessor() {
        return imageProcessor;
    }
    
    public PathManager getPathManager() {
        return pathManager;
    }
    
    /**
     * 程序入口
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("请选择图像文件");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "图像文件 (JPG, PNG)", "jpg", "jpeg", "png"
            ));
            fileChooser.setCurrentDirectory(new File("."));

            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String imagePath = selectedFile.getAbsolutePath();

                Main ui = new Main(imagePath);
                ui.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}