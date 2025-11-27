import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageProcessingApp extends JFrame {
    private BufferedImage original;
    private BufferedImage processed;

    private final JLabel originalLabel = new JLabel();
    private final JLabel processedLabel = new JLabel();

    // UI controls
    private final JButton loadBtn = new JButton("Загрузить изображение");
    private final JButton saveBtn = new JButton("Сохранить результат");
    private final JButton resetBtn = new JButton("Сброс");

    private final JButton gaussianBtn = new JButton("Гаусс (сглаживание)");
    private final JSlider gaussKernelSlider = new JSlider(3, 51, 15);
    private final JSlider gaussSigmaSlider = new JSlider(1, 50, 10); // sigma * 0.1

    private final JButton bernsenBtn = new JButton("Порог Бернсена");
    private final JSpinner bernsenR = new JSpinner(new SpinnerNumberModel(15, 3, 101, 2));
    private final JSpinner bernsenEps = new JSpinner(new SpinnerNumberModel(15, 0, 255, 1));

    private final JButton niblackBtn = new JButton("Порог Ниблака");
    private final JSpinner niblackR = new JSpinner(new SpinnerNumberModel(15, 3, 101, 2));
    private final JSpinner niblackK = new JSpinner(new SpinnerNumberModel(-0.2, -5.0, 5.0, 0.1));

    private JSpinner meanR = new JSpinner(new SpinnerNumberModel(5, 3, 99, 2));
    private JButton meanBtn = new JButton("Усредняющее сглаживание");


    public ImageProcessingApp() {
        super("Обработка изображений — Гаусс, Усредняющий фильтр, Бернсен, Ниблак");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(loadBtn);
        top.add(saveBtn);
        top.add(resetBtn);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1,2));
        originalLabel.setHorizontalAlignment(JLabel.CENTER);
        processedLabel.setHorizontalAlignment(JLabel.CENTER);
        center.add(new JScrollPane(originalLabel));
        center.add(new JScrollPane(processedLabel));
        add(center, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        // Gaussian
        JPanel gpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gpanel.setBorder(BorderFactory.createTitledBorder("Гаусс: низкочастотное сглаживание"));
        gpanel.add(new JLabel("Размер ядра (нечетное):"));
        gaussKernelSlider.setMajorTickSpacing(8);
        gaussKernelSlider.setPaintTicks(true);
        gaussKernelSlider.setPaintLabels(true);
        gpanel.add(gaussKernelSlider);
        gpanel.add(new JLabel("Sigma x0.1:"));
        gaussSigmaSlider.setMajorTickSpacing(10);
        gaussSigmaSlider.setPaintTicks(true);
        gaussSigmaSlider.setPaintLabels(true);
        gpanel.add(gaussSigmaSlider);
        gpanel.add(gaussianBtn);
        controls.add(gpanel);

        // Bernsen
        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bpanel.setBorder(BorderFactory.createTitledBorder("Локальная пороговая — Бернсен"));
        bpanel.add(new JLabel("r (окрестность):"));
        bpanel.add(bernsenR);
        bpanel.add(new JLabel("eps:"));
        bpanel.add(bernsenEps);
        bpanel.add(bernsenBtn);
        controls.add(bpanel);

        // Niblack
        JPanel npanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        npanel.setBorder(BorderFactory.createTitledBorder("Локальная пороговая — Ниблак"));
        npanel.add(new JLabel("r (окрестность):"));
        npanel.add(niblackR);
        npanel.add(new JLabel("k:"));
        npanel.add(niblackK);
        npanel.add(niblackBtn);
        controls.add(npanel);

        add(controls, BorderLayout.SOUTH);

        attachListeners();
        pack();
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setVisible(true);

        // Mean filter
        JPanel mpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mpanel.setBorder(BorderFactory.createTitledBorder("Усредняющий фильтр (Mean Filter)"));
        mpanel.add(new JLabel("r (окно, нечетное):"));
        mpanel.add(meanR);
        mpanel.add(meanBtn);
        controls.add(mpanel);

    }

    private void attachListeners() {
        loadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int ret = fc.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                try {
                    File f = fc.getSelectedFile();
                    original = ImageIO.read(f);
                    processed = deepCopy(original);
                    updateViews();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при чтении файла: " + ex.getMessage());
                }
            }
        });

        saveBtn.addActionListener(e -> {
            if (processed == null) return;
            JFileChooser fc = new JFileChooser();
            int ret = fc.showSaveDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                try {
                    File out = fc.getSelectedFile();
                    ImageIO.write(processed, "PNG", out);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при сохранении: " + ex.getMessage());
                }
            }
        });

        resetBtn.addActionListener(e -> {
            if (original != null) {
                processed = deepCopy(original);
                updateViews();
            }
        });

        gaussianBtn.addActionListener(e -> {
            if (original == null) return;
            int k = gaussKernelSlider.getValue();
            if (k % 2 == 0) k++;
            double sigma = gaussSigmaSlider.getValue() / 10.0;
            processed = gaussianBlur(original, k, sigma);
            updateViews();
        });

        bernsenBtn.addActionListener(e -> {
            if (original == null) return;
            int r = (Integer)bernsenR.getValue();
            int eps = (Integer)bernsenEps.getValue();
            processed = bernsenThreshold(original, r, eps);
            updateViews();
        });

        niblackBtn.addActionListener(e -> {
            if (original == null) return;
            int r = (Integer)niblackR.getValue();
            double k = (Double)niblackK.getValue();
            processed = niblackThreshold(original, r, k);
            updateViews();
        });

        meanBtn.addActionListener(e -> {
            if (original == null) return;

            int r = (Integer) meanR.getValue();
            if (r % 2 == 0) r++; // на всякий случай, делаем r нечётным

            processed = meanFilter(processed, r); // либо original → если хочешь применять всегда к исходному
            updateViews();
        });


    }

    private void updateViews() {
        if (original != null) originalLabel.setIcon(new ImageIcon(scaleToLabel(original, originalLabel)));
        if (processed != null) processedLabel.setIcon(new ImageIcon(scaleToLabel(processed, processedLabel)));
    }

    private Image scaleToLabel(BufferedImage img, JLabel label) {
        int w = img.getWidth();
        int h = img.getHeight();
        int lw = Math.max(200, getWidth()/2 - 50);
        int lh = Math.max(200, getHeight() - 300);
        double scale = Math.min((double)lw/w, (double)lh/h);
        if (scale >= 1.0) return img;
        return img.getScaledInstance((int)(w*scale), (int)(h*scale), Image.SCALE_SMOOTH);
    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage copy = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return copy;
    }

    private static int gray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int)(0.299*r + 0.587*g + 0.114*b);
    }

    private static BufferedImage gaussianBlur(BufferedImage src, int kernelSize, double sigma) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        double[] kernel = makeGaussianKernel(kernelSize, sigma);

        int half = kernelSize/2;
        for (int y=0;y<h;y++){
            for (int x=0;x<w;x++){
                double sr=0, sg=0, sb=0, sa=0, norm=0;
                for (int i=-half;i<=half;i++){
                    int xi = x + i;
                    if (xi < 0) xi = 0; if (xi >= w) xi = w-1;
                    int rgb = src.getRGB(xi,y);
                    double kval = kernel[i+half];
                    sa += ((rgb>>24)&0xFF)*kval;
                    sr += ((rgb>>16)&0xFF)*kval;
                    sg += ((rgb>>8)&0xFF)*kval;
                    sb += (rgb&0xFF)*kval;
                    norm += kval;
                }
                int a = clamp((int)Math.round(sa/norm));
                int r = clamp((int)Math.round(sr/norm));
                int g = clamp((int)Math.round(sg/norm));
                int b = clamp((int)Math.round(sb/norm));
                tmp.setRGB(x,y,(a<<24)|(r<<16)|(g<<8)|b);
            }
        }

        for (int y=0;y<h;y++){
            for (int x=0;x<w;x++){
                double sr=0, sg=0, sb=0, sa=0, norm=0;
                for (int i=-half;i<=half;i++){
                    int yi = y + i;
                    if (yi < 0) yi = 0; if (yi >= h) yi = h-1;
                    int rgb = tmp.getRGB(x,yi);
                    double kval = kernel[i+half];
                    sa += ((rgb>>24)&0xFF)*kval;
                    sr += ((rgb>>16)&0xFF)*kval;
                    sg += ((rgb>>8)&0xFF)*kval;
                    sb += (rgb&0xFF)*kval;
                    norm += kval;
                }
                int a = clamp((int)Math.round(sa/norm));
                int r = clamp((int)Math.round(sr/norm));
                int g = clamp((int)Math.round(sg/norm));
                int b = clamp((int)Math.round(sb/norm));
                dst.setRGB(x,y,(a<<24)|(r<<16)|(g<<8)|b);
            }
        }
        return dst;
    }

    public static BufferedImage meanFilter(BufferedImage src, int r) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int half = r / 2;
        double norm = 1.0 / (r * r);

        for (int y = half; y < height - half; y++) {
            for (int x = half; x < width - half; x++) {

                double sum = 0;
                // проходим окно r × r
                for (int j = -half; j <= half; j++) {
                    for (int i = -half; i <= half; i++) {

                        int gray = src.getRGB(x + i, y + j) & 0xFF;
                        sum += gray;
                    }
                }

                int val = (int) Math.round(sum * norm);

                val = Math.min(255, Math.max(0, val));
                int rgb = (val << 16) | (val << 8) | val;
                out.setRGB(x, y, rgb);
            }
        }

        return out;
    }

    private static double[] makeGaussianKernel(int size, double sigma) {
        if (sigma <= 0) sigma = 1.0;
        double[] k = new double[size];
        int half = size/2;
        double sum = 0;
        for (int i=0;i<size;i++){
            int x = i - half;
            k[i] = Math.exp(-(x*x)/(2*sigma*sigma));
            sum += k[i];
        }
        for (int i=0;i<size;i++) k[i] /= sum;
        return k;
    }

    private static int clamp(int v){ if (v<0) return 0; if (v>255) return 255; return v; }

    private static BufferedImage bernsenThreshold(BufferedImage src, int r, int eps) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_BINARY);
        int half = r/2;
        for (int y=0;y<h;y++){
            for (int x=0;x<w;x++){
                int xmin = Math.max(0, x-half);
                int xmax = Math.min(w-1, x+half);
                int ymin = Math.max(0, y-half);
                int ymax = Math.min(h-1, y+half);
                int jlow = 255, jhigh = 0;
                for (int yy=ymin; yy<=ymax; yy++){
                    for (int xx=xmin; xx<=xmax; xx++){
                        int g = gray(src.getRGB(xx,yy));
                        if (g < jlow) jlow = g;
                        if (g > jhigh) jhigh = g;
                    }
                }
                int C = jhigh - jlow;
                int t;
                if (C <= eps) {
                    t = (jhigh + jlow)/2;
                } else {
                    t = (jhigh + jlow)/2;
                }
                int center = gray(src.getRGB(x,y));
                int val = (center >= t) ? 0xFFFFFFFF : 0xFF000000;
                out.setRGB(x,y,val);
            }
        }
        return out;
    }

    private static BufferedImage niblackThreshold(BufferedImage src, int r, double k) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w,h,BufferedImage.TYPE_BYTE_BINARY);
        int half = r/2;
        long[][] sum = new long[h+1][w+1];
        long[][] sumsq = new long[h+1][w+1];
        for (int y=1;y<=h;y++){
            long rowsum = 0, rowsumsq = 0;
            for (int x=1;x<=w;x++){
                int g = gray(src.getRGB(x-1,y-1));
                rowsum += g;
                rowsumsq += g*g;
                sum[y][x] = sum[y-1][x] + rowsum;
                sumsq[y][x] = sumsq[y-1][x] + rowsumsq;
            }
        }
        for (int y=0;y<h;y++){
            for (int x=0;x<w;x++){
                int xmin = Math.max(0, x-half);
                int xmax = Math.min(w-1, x+half);
                int ymin = Math.max(0, y-half);
                int ymax = Math.min(h-1, y+half);
                int area = (xmax - xmin + 1)*(ymax - ymin + 1);
                int x1 = xmin, y1 = ymin, x2 = xmax, y2 = ymax;
                long s = sum[y2+1][x2+1] - sum[y1][x2+1] - sum[y2+1][x1] + sum[y1][x1];
                long ssq = sumsq[y2+1][x2+1] - sumsq[y1][x2+1] - sumsq[y2+1][x1] + sumsq[y1][x1];
                double mu = (double)s / area;
                double variance = ((double)ssq / area) - mu*mu;
                if (variance < 0) variance = 0;
                double sigma = Math.sqrt(variance);
                double t = mu + k * sigma;
                int center = gray(src.getRGB(x,y));
                int val = (center >= t) ? 0xFFFFFFFF : 0xFF000000;
                out.setRGB(x,y,val);
            }
        }
        return out;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageProcessingApp::new);
    }
}
