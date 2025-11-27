import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * RasterAlgorithmsApp — демонстрация базовых растровых алгоритмов:
 *  - Пошаговый
 *  - DDA (ЦДА)
 *  - Брезенхем (линия)
 *  - Брезенхем (окружность)
 *  - Castle-Pitteway (вариант "Castel-Pitway")
 *  - Алгоритм Ву (аппроксимация с интенсивностями)
 *
 * Интерфейс: слева параметры, справа холст.
 */
public class RasterAlgorithmsApp extends JFrame {

    // UI компоненты
    private final JComboBox<String> algoCombo;
    private final JSpinner sx1, sy1, sx2, sy2, sxc, syc, sr;
    private final JButton drawButton;
    private final JCheckBox showIdealCB;
    private final JLabel timeLabel;
    private final JLabel countLabel;
    private final JSpinner pixelSizeSpinner;

    private final DrawPanel drawPanel;

    // Алгоритмы
    private static final String STEP_BY_STEP = "Пошаговый";
    private static final String DDA = "ЦДА (DDA)";
    private static final String BRESENHAM_LINE = "Брезенхем (линия)";
    private static final String BRESENHAM_CIRCLE = "Брезенхем (окружность)";
    private static final String CASTLE = "Кастл-Питвей";
    private static final String WU = "Алгоритм Ву";

    public RasterAlgorithmsApp() {
        super("Лабораторная №3 — Базовые растровые алгоритмы");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Панель управления слева
        JPanel control = new JPanel();
        control.setLayout(new BoxLayout(control, BoxLayout.Y_AXIS));
        control.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        control.add(new JLabel("Выберите алгоритм:"));
        algoCombo = new JComboBox<>(new String[]{
                STEP_BY_STEP, DDA, BRESENHAM_LINE, BRESENHAM_CIRCLE, CASTLE, WU
        });
        control.add(algoCombo);
        control.add(Box.createRigidArea(new Dimension(0,10)));

        JPanel linePanel = new JPanel(new GridLayout(4,2,4,4));
        linePanel.setBorder(BorderFactory.createTitledBorder("Параметры отрезка"));
        sx1 = new JSpinner(new SpinnerNumberModel(10, -1000, 1000, 1));
        sy1 = new JSpinner(new SpinnerNumberModel(10, -1000, 1000, 1));
        sx2 = new JSpinner(new SpinnerNumberModel(80, -1000, 1000, 1));
        sy2 = new JSpinner(new SpinnerNumberModel(60, -1000, 1000, 1));
        linePanel.add(new JLabel("X1:")); linePanel.add(sx1);
        linePanel.add(new JLabel("Y1:")); linePanel.add(sy1);
        linePanel.add(new JLabel("X2:")); linePanel.add(sx2);
        linePanel.add(new JLabel("Y2:")); linePanel.add(sy2);
        control.add(linePanel);
        control.add(Box.createRigidArea(new Dimension(0,10)));

        JPanel circlePanel = new JPanel(new GridLayout(3,2,4,4));
        circlePanel.setBorder(BorderFactory.createTitledBorder("Параметры окружности"));
        sxc = new JSpinner(new SpinnerNumberModel(50, -1000, 1000, 1));
        syc = new JSpinner(new SpinnerNumberModel(50, -1000, 1000, 1));
        sr  = new JSpinner(new SpinnerNumberModel(30, 1, 1000, 1));
        circlePanel.add(new JLabel("Xc:")); circlePanel.add(sxc);
        circlePanel.add(new JLabel("Yc:")); circlePanel.add(syc);
        circlePanel.add(new JLabel("R:"));  circlePanel.add(sr);
        control.add(circlePanel);
        control.add(Box.createRigidArea(new Dimension(0,10)));

        showIdealCB = new JCheckBox("Показать идеальную фигуру (контур)", true);
        control.add(showIdealCB);
        control.add(Box.createRigidArea(new Dimension(0,10)));

        control.add(new JLabel("Размер пикселя (в точках):"));
        pixelSizeSpinner = new JSpinner(new SpinnerNumberModel(6, 1, 40, 1));
        control.add(pixelSizeSpinner);
        control.add(Box.createRigidArea(new Dimension(0,10)));

        drawButton = new JButton("Нарисовать");
        control.add(drawButton);
        control.add(Box.createRigidArea(new Dimension(0,10)));

        timeLabel = new JLabel("Время: —");
        countLabel = new JLabel("Пикселей: —");
        control.add(timeLabel);
        control.add(countLabel);

        control.add(Box.createVerticalGlue());
        add(control, BorderLayout.WEST);

        // Панель рисования
        drawPanel = new DrawPanel();
        add(drawPanel, BorderLayout.CENTER);

        // Поведение при выборе алгоритма: скрываем/показываем блоки параметров
        algoCombo.addActionListener(e -> updateParamVisibility());
        updateParamVisibility();

        drawButton.addActionListener(e -> runAndRender());

        // Быстрая реакция на изменение размера пикселей — перерисовать
        pixelSizeSpinner.addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) { drawPanel.repaint(); }
        });
    }

    private void updateParamVisibility() {
        String algo = (String) algoCombo.getSelectedItem();
        boolean isCircle = BRESENHAM_CIRCLE.equals(algo);
        // Отрезок параметры видимые если не окружность
        sx1.setEnabled(!isCircle);
        sy1.setEnabled(!isCircle);
        sx2.setEnabled(!isCircle);
        sy2.setEnabled(!isCircle);
        // Окружность только если окружность
        sxc.setEnabled(isCircle);
        syc.setEnabled(isCircle);
        sr.setEnabled(isCircle);
    }

    private void runAndRender() {
        String algo = (String) algoCombo.getSelectedItem();

        // Считываем параметры
        int x1 = (Integer) sx1.getValue();
        int y1 = (Integer) sy1.getValue();
        int x2 = (Integer) sx2.getValue();
        int y2 = (Integer) sy2.getValue();
        int xc = (Integer) sxc.getValue();
        int yc = (Integer) syc.getValue();
        int r  = (Integer) sr.getValue();

        // Выполнение выбранного алгоритма и замер времени
        long t0 = System.nanoTime();

        // Мы возвращаем список пикселей с интенсивностью (1.0 = полная)
        List<Pixel> pixels = new ArrayList<>();
        switch (algo) {
            case STEP_BY_STEP:
                for (Point p : stepByStepLine(x1, y1, x2, y2)) pixels.add(new Pixel(p.x, p.y, 1.0f));
                break;
            case DDA:
                for (Point p : ddaLine(x1, y1, x2, y2)) pixels.add(new Pixel(p.x, p.y, 1.0f));
                break;
            case BRESENHAM_LINE:
                for (Point p : bresenhamLine(x1, y1, x2, y2)) pixels.add(new Pixel(p.x, p.y, 1.0f));
                break;
            case BRESENHAM_CIRCLE:
                for (Point p : bresenhamCircle(xc, yc, r)) pixels.add(new Pixel(p.x, p.y, 1.0f));
                break;
            case CASTLE:
                for (Point p : castlePitteway(x1, y1, x2, y2)) pixels.add(new Pixel(p.x, p.y, 1.0f));
                break;
            case WU:
                List<Pixel> wu = wuAntialiasingLine(x1, y1, x2, y2);
                pixels.addAll(wu);
                break;
            default:
                break;
        }

        long t1 = System.nanoTime();
        double ms = (t1 - t0) / 1_000_000.0;
        timeLabel.setText(String.format("Время: %.4f мс", ms));
        countLabel.setText("Пикселей: " + pixels.size());

        // Передаем данные в панель и перерисовываем
        drawPanel.setData(pixels, algo, x1, y1, x2, y2, xc, yc, r, showIdealCB.isSelected(), (Integer) pixelSizeSpinner.getValue());
    }

    // ------------------ Алгоритмы ------------------

    // Pixel с интенсивностью для антиалиасинга
    private static class Pixel {
        int x, y;
        float intensity; // 0..1
        Pixel(int x, int y, float intensity) { this.x = x; this.y = y; this.intensity = intensity; }
    }

    // Побочные структуры
    private static class Point { int x, y; Point(int x, int y){this.x=x;this.y=y;} }

    /**
     * 1) Пошаговый алгоритм (step-by-step).
     * Поддерживает вертикальные и наклонные отрезки.
     */
    public List<Point> stepByStepLine(int x1, int y1, int x2, int y2) {
        List<Point> pixels = new ArrayList<>();
        if (x1 == x2) {
            int sy = Math.min(y1, y2);
            int ey = Math.max(y1, y2);
            for (int y = sy; y <= ey; y++) pixels.add(new Point(x1, y));
            return pixels;
        }

        // Сортировка по x для простоты
        if (x1 > x2) { int tx=x1, ty=y1; x1=x2; y1=y2; x2=tx; y2=ty; }

        int dx = x2 - x1;
        int dy = y2 - y1;
        double m = (double) dy / dx;

        if (Math.abs(dx) >= Math.abs(dy)) {
            for (int x = x1; x <= x2; x++) {
                int y = (int) Math.round(y1 + m * (x - x1));
                pixels.add(new Point(x, y));
            }
        } else {
            // доминирует y
            if (y1 > y2) { int tx=x1, ty=y1; x1=x2; y1=y2; x2=tx; y2=ty; }
            double mInv = (double) dx / (y2 - y1);
            for (int y = y1; y <= y2; y++) {
                int x = (int) Math.round(x1 + mInv * (y - y1));
                pixels.add(new Point(x, y));
            }
        }
        return pixels;
    }

    /**
     * 2) DDA (Digital Differential Analyzer)
     */
    public List<Point> ddaLine(int x1, int y1, int x2, int y2) {
        List<Point> pixels = new ArrayList<>();
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { pixels.add(new Point(x1, y1)); return pixels; }
        double xincr = dx / (double) steps;
        double yincr = dy / (double) steps;
        double x = x1;
        double y = y1;
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i <= steps; i++) {
            int rx = (int) Math.round(x);
            int ry = (int) Math.round(y);
            long key = (((long)rx)<<32) ^ (ry & 0xffffffffL);
            if (!seen.contains(key)) {
                pixels.add(new Point(rx, ry));
                seen.add(key);
            }
            x += xincr; y += yincr;
        }
        return pixels;
    }

    /**
     * 3) Брезенхем (линия)
     */
    public List<Point> bresenhamLine(int x0, int y0, int x1, int y1) {
        List<Point> pts = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0, y = y0;
        while (true) {
            pts.add(new Point(x, y));
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                if (x == x1) break;
                err += dy;
                x += sx;
            }
            if (e2 <= dx) {
                if (y == y1) break;
                err += dx;
                y += sy;
            }
        }
        return pts;
    }

    /**
     * 4) Брезенхем для окружностей (симметричный)
     */
    public List<Point> bresenhamCircle(int xc, int yc, int r) {
        Set<Long> set = new HashSet<>();
        int x = 0, y = r;
        int d = 3 - 2 * r;
        while (x <= y) {
            addCircleSym(xc, yc, x, y, set);
            if (d < 0) {
                d = d + 4 * x + 6;
            } else {
                d = d + 4 * (x - y) + 10;
                y--;
            }
            x++;
        }
        List<Point> out = new ArrayList<>();
        for (Long key : set) {
            Long key2 = key >>32;
            int px = key2.intValue();
            int py = key.intValue();
            out.add(new Point(px, py));
        }
        return out;
    }

    private void addCircleSym(int xc, int yc, int dx, int dy, Set<Long> set) {
        addToSet(xc+dx, yc+dy, set);
        addToSet(xc-dx, yc+dy, set);
        addToSet(xc+dx, yc-dy, set);
        addToSet(xc-dx, yc-dy, set);
        addToSet(xc+dy, yc+dx, set);
        addToSet(xc-dy, yc+dx, set);
        addToSet(xc+dy, yc-dx, set);
        addToSet(xc-dy, yc-dx, set);
    }
    private void addToSet(int x, int y, Set<Long> set) {
        long key = (((long)x)<<32) ^ (y & 0xffffffffL);
        set.add(key);
    }

    /**
     * 5) Castle-Pitteway (генерация строки движений и преобразование в пиксели)
     * Реализация следует логике из приведенного вами Python-кода.
     */
    public List<Point> castlePitteway(int x1, int y1, int x2, int y2) {
        int dxTotal = Math.abs(x2 - x1);
        int dyTotal = Math.abs(y2 - y1);
        boolean swapped = false;
        int a = dxTotal, b = dyTotal;
        if (b > a) { int t=a; a=b; b=t; swapped = true; }

        String moveString;
        if (b == 0) {
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<a;i++) sb.append('s');
            moveString = sb.toString();
        } else if (a == b) {
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<a;i++) sb.append('d');
            moveString = sb.toString();
        } else {
            int x = a - b;
            int y = b;
            String m1 = "s";
            String m2 = "d";
            while (x != y) {
                if (x > y) {
                    x -= y;
                    m2 = m1 + m2;
                } else {
                    y -= x;
                    m1 = m2 + m1;
                }
            }
            String seq = m2 + m1;
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<x;i++) sb.append(seq);
            moveString = sb.toString();
        }

        List<Point> pixels = new ArrayList<>();
        int currX = x1, currY = y1;
        pixels.add(new Point(currX, currY));
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        for (char mv : moveString.toCharArray()) {
            if (mv == 'd') {
                currX += sx;
                currY += sy;
            } else {
                if (swapped) currY += sy;
                else currX += sx;
            }
            pixels.add(new Point(currX, currY));
        }
        return pixels;
    }

    /**
     * 6) Алгоритм Ву (аппроксимация).
     * Возвращает пиксели с интенсивностями.
     * Замечание: для упрощения реализации и отображения интенсивность используется как прозрачность.
     */
    public List<Pixel> wuAntialiasingLine(int x1, int y1, int x2, int y2) {
        List<Pixel> result = new ArrayList<>();
        double dx = x2 - x1;
        double dy = y2 - y1;
        boolean steep = Math.abs(dy) > Math.abs(dx);
        if (steep) {
            // swap x и y
            int tmp;
            tmp = x1; x1 = y1; y1 = tmp;
            tmp = x2; x2 = y2; y2 = tmp;
            dx = x2 - x1; dy = y2 - y1;
        }
        boolean swapped = false;
        if (x1 > x2) {
            // swap endpoints
            int tmp;
            tmp = x1; x1 = x2; x2 = tmp;
            tmp = y1; y1 = y2; y2 = tmp;
            dx = x2 - x1; dy = y2 - y1;
            swapped = true;
        }
        double gradient = (dx == 0) ? 1.0 : dy / dx;

        // стартовая точка
        result.add(new Pixel(x1, y1, 1.0f));
        result.add(new Pixel(x2, y2, 1.0f));

        double y = y1 + gradient;
        for (int x = x1 + 1; x <= x2 - 1; x++) {
            double frac = y - Math.floor(y);
            int p1y = (int) Math.floor(y);
            int p2y = p1y + 1;
            float i1 = (float)(1.0 - frac);
            float i2 = (float)(frac);
            if (steep) {
                result.add(new Pixel(p1y, x, i1));
                result.add(new Pixel(p2y, x, i2));
            } else {
                result.add(new Pixel(x, p1y, i1));
                result.add(new Pixel(x, p2y, i2));
            }
            y += gradient;
        }
        return result;
    }

    // ------------------ Панель для рисования ------------------

    private static class DrawPanel extends JPanel {
        private List<Pixel> pixels = Collections.emptyList();
        private String algo = "";
        private int x1,y1,x2,y2,xc,yc,r;
        private boolean showIdeal = true;
        private int pixelSize = 6;

        public DrawPanel() {
            setBackground(Color.WHITE);
        }

        public void setData(List<Pixel> pixels, String algo,
                            int x1, int y1, int x2, int y2,
                            int xc, int yc, int r, boolean showIdeal, int pixelSize) {
            this.pixels = new ArrayList<>(pixels);
            this.algo = algo;
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.xc = xc; this.yc = yc; this.r = r;
            this.showIdeal = showIdeal;
            this.pixelSize = Math.max(1, pixelSize);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();

            // Центрирование координат: даём небольшие отступы
            int w = getWidth();
            int h = getHeight();
            int offset = 20;
            // Сдвигаем систему так, чтобы координата (0,0) отображалась примерно в левом-верхнем квадрате
            // Мы будем рисовать в "пиксельных координатах" где (x,y) преобразуются в экран:
            // screenX = offset + x * pixelSize; screenY = offset + y * pixelSize
            // Для поддержки отрицательных координат, можно вычислить minX/minY в данных — но для простоты пусть будет фикс.
            // Если нужно — можно изменить в будущих итерациях.

            // Нарисуем идеальный отрезок/окружность (как тонкий контур)
            if (showIdeal) {
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(1f));
                if (BRESENHAM_CIRCLE.equals(algo)) {
                    // Идеальная окружность
                    g.drawOval(offset + xc*pixelSize - r*pixelSize, offset + yc*pixelSize - r*pixelSize,
                            r*2*pixelSize, r*2*pixelSize);
                } else {
                    // Идеальный отрезок
                    g.drawLine(offset + x1*pixelSize, offset + y1*pixelSize, offset + x2*pixelSize, offset + y2*pixelSize);
                }
            }

            // Рисуем пиксели: мы хотим, чтобы пиксель был квадрат размером pixelSize.
            // Для антиалиасинга используем прозрачность AlphaComposite.
            for (Pixel p : pixels) {
                int sx = offset + p.x * pixelSize;
                int sy = offset + p.y * pixelSize;
                if (p.intensity >= 0.999f) {
                    // Полный пиксель
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g.setColor(Color.BLUE);
                    g.fillRect(sx, sy, pixelSize, pixelSize);
                    // рамка для читаемости
                    g.setColor(Color.DARK_GRAY);
                    g.drawRect(sx, sy, pixelSize, pixelSize);
                } else {
                    // Антиалиасный пиксель: рисуем с прозрачностью, цвет — тёмно-синий
                    float alpha = clamp(p.intensity, 0f, 1f);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    // dark blue with transparency
                    g.setColor(new Color(0x00,0x00,0x80, (int)(255*alpha)));
                    g.fillRect(sx, sy, pixelSize, pixelSize);
                }
            }

            // Сброс композита
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            // Рисунок осей (опционально)
            g.setColor(Color.LIGHT_GRAY);
            // Вертикальная и горизонтальная центральные линии для ориентира
            g.drawLine(10, h/2, w-10, h/2);
            g.drawLine(w/2, 10, w/2, h-10);

            g.dispose();
        }

        private float clamp(float v, float a, float b) {
            return Math.max(a, Math.min(b, v));
        }
    }

    // ------------------ Main ------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RasterAlgorithmsApp app = new RasterAlgorithmsApp();
            app.setVisible(true);
        });
    }
}
