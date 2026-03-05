package org.reco.reco_sys.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "数据冲突"),

    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名或邮箱已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    EMAIL_CODE_INVALID(1005, "验证码无效或已过期"),
    EMAIL_CODE_ALREADY_USED(1006, "验证码已使用"),

    COURSE_NOT_FOUND(2001, "课程不存在"),
    COURSE_NOT_ENROLLED(2002, "未加入该课程"),
    COURSE_ALREADY_ENROLLED(2003, "已加入该课程"),

    EXERCISE_NOT_FOUND(3001, "习题不存在"),

    UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(4002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(4003, "文件大小超出限制"),

    UPLOAD_SESSION_NOT_FOUND(5001, "上传会话不存在"),
    UPLOAD_SESSION_EXPIRED(5002, "上传会话已过期"),

    RECOMMEND_SERVICE_ERROR(6001, "推荐服务调用失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
