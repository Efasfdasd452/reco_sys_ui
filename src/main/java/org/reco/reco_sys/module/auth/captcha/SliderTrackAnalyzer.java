package org.reco.reco_sys.module.auth.captcha;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 滑块轨迹分析器：对采集的鼠标/触摸轨迹进行多维度打分，识别机器人行为。
 *
 * <p>评分从 100 开始，各维度扣分，最终分 >= 60 视为人类操作。
 *
 * <p>检测维度：
 * <ol>
 *   <li>总时间：&lt;150ms 直接拒绝；&lt;300ms 扣分</li>
 *   <li>Y 轴抖动：人手滑动必有轻微竖向偏移；完全水平则扣分</li>
 *   <li>速度曲线：人类有加速-匀速-减速过程；恒速是机器特征</li>
 *   <li>采样间隔均匀性：机器定时采样，间隔方差极小</li>
 *   <li>轨迹线性度：人手不会走完美直线</li>
 *   <li>采样点密度：程序模拟通常采样点极少</li>
 * </ol>
 */
@Component
public class SliderTrackAnalyzer {

    /** 最低通过分（0~100） */
    public static final int PASS_SCORE = 60;

    /**
     * 分析轨迹，返回 0~100 的得分。
     *
     * @param track     轨迹点列表（前端采集，mousemove/touchmove 每次触发均记录）
     * @param totalTime 总拖动时间（毫秒）
     * @param distance  实际拖动像素距离
     * @return 0~100，低于 PASS_SCORE 视为机器
     */
    public int analyze(List<TrackPoint> track, long totalTime, int distance) {
        if (track == null || track.size() < 5) return 0;

        int score = 100;

        // 1. 时间检测
        if (totalTime < 150) return 0;
        if (totalTime < 300) score -= 30;

        // 2. Y 轴抖动：人手必有轻微上下偏移
        double yStdDev = calcYStdDev(track);
        if (yStdDev < 0.5) score -= 40;
        else if (yStdDev < 2.0) score -= 20;

        // 3. 速度曲线：方差极低说明恒速（机器）
        score -= penaltyForConstantVelocity(track);

        // 4. 采样间隔均匀性：定时采样方差极小
        score -= penaltyForUniformInterval(track);

        // 5. 轨迹线性度：完美直线扣分
        double linearity = calcLinearity(track);
        if (linearity > 0.999) score -= 30;
        else if (linearity > 0.998) score -= 15;

        // 6. 采样点密度：每像素采样点太少说明程序模拟
        if (distance > 0) {
            double pointsPerPx = (double) track.size() / distance;
            if (pointsPerPx < 0.3) score -= 20;
        }

        return Math.max(0, score);
    }

    // -----------------------------------------------------------------------

    private double calcYStdDev(List<TrackPoint> track) {
        double baseY = track.get(0).getY();
        double[] deltas = track.stream().mapToDouble(p -> p.getY() - baseY).toArray();
        double mean = Arrays.stream(deltas).average().orElse(0);
        double variance = Arrays.stream(deltas).map(d -> (d - mean) * (d - mean)).average().orElse(0);
        return Math.sqrt(variance);
    }

    /** 计算每段速度，若速度标准差极低（恒速）则扣分 */
    private int penaltyForConstantVelocity(List<TrackPoint> track) {
        List<Double> velocities = new ArrayList<>();
        for (int i = 1; i < track.size(); i++) {
            TrackPoint a = track.get(i - 1), b = track.get(i);
            long dt = b.getT() - a.getT();
            if (dt <= 0) continue;
            velocities.add((b.getX() - a.getX()) / dt);
        }
        if (velocities.size() < 3) return 20;

        double mean = velocities.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = Math.sqrt(velocities.stream()
                .mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0));

        if (stdDev < 0.05) return 35;
        if (stdDev < 0.20) return 15;
        return 0;
    }

    /** 计算相邻采样点时间间隔的标准差；极小则扣分 */
    private int penaltyForUniformInterval(List<TrackPoint> track) {
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < track.size(); i++) {
            intervals.add(track.get(i).getT() - track.get(i - 1).getT());
        }
        if (intervals.isEmpty()) return 10;

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double stdDev = Math.sqrt(intervals.stream()
                .mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0));

        if (stdDev < 1.0) return 30;
        if (stdDev < 3.0) return 15;
        return 0;
    }

    /**
     * 计算轨迹线性度（0~1，1 = 完美直线）。
     * 方法：以首尾两点连线为基准，计算其余点到该直线的平均距离。
     */
    private double calcLinearity(List<TrackPoint> track) {
        TrackPoint s = track.get(0);
        TrackPoint e = track.get(track.size() - 1);
        double totalDist = Math.hypot(e.getX() - s.getX(), e.getY() - s.getY());
        if (totalDist == 0) return 1.0;

        double sumDev = 0;
        for (TrackPoint p : track) {
            double dev = Math.abs(
                    (e.getY() - s.getY()) * p.getX()
                    - (e.getX() - s.getX()) * p.getY()
                    + e.getX() * s.getY()
                    - e.getY() * s.getX()
            ) / totalDist;
            sumDev += dev;
        }
        double avgDev = sumDev / track.size();
        return 1.0 / (1.0 + avgDev);
    }
}
