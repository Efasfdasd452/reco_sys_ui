package org.reco.reco_sys.module.auth.captcha;

import lombok.Data;

@Data
public class TrackPoint {
    /** 鼠标/触摸的 clientX（相对页面） */
    private double x;
    /** 鼠标/触摸的 clientY */
    private double y;
    /** 时间戳（毫秒，Date.now()） */
    private long t;
}
