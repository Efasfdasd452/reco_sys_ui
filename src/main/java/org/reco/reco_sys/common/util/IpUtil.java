package org.reco.reco_sys.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Slf4j
@Component
public class IpUtil {

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("ip2region.xdb");
            InputStream inputStream = resource.getInputStream();
            byte[] dbBytes = inputStream.readAllBytes();
            searcher = Searcher.newWithBuffer(dbBytes);
            log.info("ip2region 数据库加载成功");
        } catch (Exception e) {
            log.warn("ip2region 数据库加载失败，IP地理位置解析将不可用: {}", e.getMessage());
        }
    }

    /**
     * 解析 IP 地理位置
     * @return 例如 "中国|0|北京市|北京市|联通"，解析失败返回 "未知"
     */
    public String getLocation(String ip) {
        if (searcher == null || ip == null || ip.isBlank()) return "未知";
        try {
            String region = searcher.search(ip);
            // 格式: 国家|区域|省份|城市|ISP
            String[] parts = region.split("\\|");
            if (parts.length >= 4) {
                String province = parts[2].equals("0") ? "" : parts[2];
                String city = parts[3].equals("0") ? "" : parts[3];
                return (province + city).isBlank() ? parts[0] : province + city;
            }
            return region;
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * 从请求中获取真实 IP（兼容反向代理）
     */
    public static String getRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
