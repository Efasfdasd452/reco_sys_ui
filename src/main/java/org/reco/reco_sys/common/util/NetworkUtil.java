package org.reco.reco_sys.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 获取本机局域网 IP，用于生成跨设备上传二维码 URL
 */
@Slf4j
@Component
public class NetworkUtil {

    /**
     * 获取本机局域网 IP（192.168.x.x 或 10.x.x.x），
     * 部署到服务器时应通过配置项覆盖，此方法仅用于本地开发
     */
    public String getLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isLoopbackAddress()) continue;
                    String ip = addr.getHostAddress();
                    // 只取 IPv4 的局域网地址
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取局域网IP失败: {}", e.getMessage());
        }
        return "127.0.0.1";
    }
}
