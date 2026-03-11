package org.reco.reco_sys.module.auth.captcha;

import lombok.Data;

import java.util.List;

@Data
public class VerifyRequest {
    private String token;
    private int sliderX;
    /** 拖动过程中采集的轨迹点列表 */
    private List<TrackPoint> track;
    /** 从按下到松开的总时间（毫秒） */
    private long totalTime;
}
