package cn.edu.hit.core;

/**
 * 表示 HTTP 状态码的枚举类。
 */
public enum HttpStatus {

    /**
     * 继续。客户端应当继续发送请求。 HTTP/1.1 协议中新增，表示服务器已经处理了一些请求，客户端应当继续发送请求的其余部分。
     */
    CONTINUE(100, "Continue"),

    /**
     * 切换协议。服务器将遵从客户端的请求，切换到不同的协议。 只有在客户端发送了Upgrade头字段时才会发送此状态码。
     */
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),

    /**
     * 处理中。服务器已经接收了部分请求，正在处理。 表示服务器正在处理请求，但尚未完成。
     */
    PROCESSING(102, "Processing"),

    /**
     * 提示信息。用于发送额外的响应头信息，以便客户端可以提前加载资源。 表示服务器发送了一些提示信息，客户端可以据此提前加载资源。
     */
    EARLY_HINTS(103, "Early Hints"),

    /**
     * 检查点。用于请求的某个中间处理步骤。 表示服务器在处理请求的过程中达到了一个检查点。
     */
    CHECKPOINT(103, "Checkpoint"),

    /**
     * 成功。请求已成功。
     */
    OK(200, "OK"),

    /**
     * 创建成功。请求已成功，并且服务器创建了新的资源。
     */
    CREATED(201, "Created"),

    /**
     * 接受。服务器已经接受了请求，但尚未处理。
     */
    ACCEPTED(202, "Accepted"),

    /**
     * 非授权信息。请求成功，但返回的信息可能来自另一服务器。
     */
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),

    /**
     * 无内容。请求已成功，但服务器不返回任何实体内容。
     */
    NO_CONTENT(204, "No Content"),

    /**
     * 重置内容。请求已成功，但服务器告诉客户端应重置文档视图。
     */
    RESET_CONTENT(205, "Reset Content"),

    /**
     * 部分内容。服务器已成功处理了部分 GET 请求。
     */
    PARTIAL_CONTENT(206, "Partial Content"),

    /**
     * 多状态。用于返回多条信息。
     */
    MULTI_STATUS(207, "Multi-Status"),

    /**
     * 已报告。在 WebDAV 进阶协议中使用。
     */
    ALREADY_REPORTED(208, "Already Reported"),

    /**
     * IM 已使用。在 HTTP Delta encoding 传输编码中使用。
     */
    IM_USED(226, "IM Used"),

    /**
     * 多种选择。请求的资源有多个选项，需要用户选择一个。
     */
    MULTIPLE_CHOICES(300, "Multiple Choices"),

    /**
     * 永久移动。请求的资源已被永久移动到新位置。
     */
    MOVED_PERMANENTLY(301, "Moved Permanently"),

    /**
     * 找到。请求的资源已被临时移动到另一个 URI。
     */
    FOUND(302, "Found"),

    /**
     * 临时移动。请求的资源已被临时移动到另一个 URI。
     */
    MOVED_TEMPORARILY(302, "Moved Temporarily"),

    /**
     * 查看其他。请求的资源可在另一个 URI 下被获取。
     */
    SEE_OTHER(303, "See Other"),

    /**
     * 未修改。自从上次请求后，请求的资源未被修改。
     */
    NOT_MODIFIED(304, "Not Modified"),

    /**
     * 使用代理。请求必须通过代理来访问。
     */
    USE_PROXY(305, "Use Proxy"),

    /**
     * 临时重定向。请求的资源临时位于另一个 URI。
     */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),

    /**
     * 永久重定向。请求的资源永久位于另一个 URI。
     */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    /**
     * 错误请求。服务器无法理解请求，客户端不应再次发送此请求。
     */
    BAD_REQUEST(400, "Bad Request"),

    /**
     * 未授权。请求需要用户的身份认证。
     */
    UNAUTHORIZED(401, "Unauthorized"),

    /**
     * 需要付款。需要付款才能访问请求的资源。
     */
    PAYMENT_REQUIRED(402, "Payment Required"),

    /**
     * 禁止访问。服务器理解请求，但是拒绝执行。
     */
    FORBIDDEN(403, "Forbidden"),

    /**
     * 未找到。服务器找不到请求的资源。
     */
    NOT_FOUND(404, "Not Found"),

    /**
     * 方法不允许。请求行中指定的请求方法不能被用于请求相应的资源。
     */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),

    /**
     * 不可接受。请求的资源的内容特性无法满足请求头中的条件，因而无法生成响应实体。
     */
    NOT_ACCEPTABLE(406, "Not Acceptable"),

    /**
     * 代理认证要求。类似401，但是请求必须通过代理。
     */
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),

    /**
     * 请求超时。客户端没有在服务器预备等待的时间内完成请求的发送。
     */
    REQUEST_TIMEOUT(408, "Request Timeout"),

    /**
     * 冲突。服务器完成请求时发生了冲突。
     */
    CONFLICT(409, "Conflict"),

    /**
     * 资源不存在。服务器找不到请求的资源，并且临时无法获得其状态。
     */
    GONE(410, "Gone"),

    /**
     * 需要长度。服务器无法处理请求，除非客户端在请求的 Content-Length 头字段中指明了请求体的长度。
     */
    LENGTH_REQUIRED(411, "Length Required"),

    /**
     * 前提条件失败。服务器在验证条件请求时发现了一个或多个前提条件失败。
     */
    PRECONDITION_FAILED(412, "Precondition Failed"),

    /**
     * 载荷太大。服务器无法处理请求，因为请求体过大。
     */
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),

    /**
     * 请求实体太大。服务器无法处理请求，因为请求体过大。
     */
    REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),

    /**
     * URI太长。请求的资源的 URI 过长。
     */
    URI_TOO_LONG(414, "URI Too Long"),

    /**
     * 请求URI太长。请求的资源的 URI 过长。
     */
    REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),

    /**
     * 不支持的媒体类型。服务器无法处理请求，因为请求体的格式不受请求的资源支持。
     */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    /**
     * 请求范围无法满足。服务器无法提供请求的范围。
     */
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),

    /**
     * 期望失败。服务器无法满足请求头中的 Expect 头字段。
     */
    EXPECTATION_FAILED(417, "Expectation Failed"),

    /**
     * 我是一个茶壶。来自一个“超文本咖啡壶控制协议”的玩笑。
     */
    I_AM_A_TEAPOT(418, "I'm a teapot"),

    /**
     * 资源空间不足。服务器无法完成请求，因为服务器上没有足够的空间来存储资源。
     */
    INSUFFICIENT_SPACE_ON_RESOURCE(419, "Insufficient Space On Resource"),

    /**
     * 方法失败。服务器无法完成请求，因为服务器遇到了导致无法完成请求的方法失败。
     */
    METHOD_FAILURE(420, "Method Failure"),

    /**
     * 目的地锁定。服务器无法完成请求，因为目的地资源被锁定。
     */
    DESTINATION_LOCKED(421, "Destination Locked"),

    /**
     * 无法处理的实体。服务器无法处理请求，因为请求体对于请求的资源来说是无法处理的。
     */
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),

    /**
     * 锁定。服务器无法完成请求，因为资源已被锁定。
     */
    LOCKED(423, "Locked"),

    /**
     * 依赖项失败。服务器无法完成请求，因为请求的依赖项之一失败了。
     */
    FAILED_DEPENDENCY(424, "Failed Dependency"),

    /**
     * 太早。服务器无法完成请求，因为服务器无法预测是否能够及时完成请求。
     */
    TOO_EARLY(425, "Too Early"),

    /**
     * 需要升级。服务器无法完成请求，因为客户端需要切换到不同的协议。
     */
    UPGRADE_REQUIRED(426, "Upgrade Required"),

    /**
     * 前提条件需要。服务器无法完成请求，因为请求头中的前提条件没有满足。
     */
    PRECONDITION_REQUIRED(428, "Precondition Required"),

    /**
     * 请求过多。服务器无法完成请求，因为客户端发送的请求太多。
     */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    /**
     * 请求头字段太大。 服务器无法处理请求，因为请求头字段太大。
     */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    /**
     * 法律原因不可用。服务器无法完成请求，因为法律原因导致资源不可用。
     */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

    /**
     * 内部服务器错误。服务器遇到了一个阻止其完成请求的意外情况。
     */
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),

    /**
     * 未实现。服务器不支持请求的功能，无法完成请求。
     */
    NOT_IMPLEMENTED(501, "Not Implemented"),

    /**
     * 错误网关。服务器作为网关或代理，从上游服务器接收到了一个无效的响应。
     */
    BAD_GATEWAY(502, "Bad Gateway"),

    /**
     * 服务不可用。服务器暂时无法处理请求，通常是因为服务器过载或维护。
     */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),

    /**
     * 网关超时。服务器作为网关或代理，但是没有及时从上游服务器接收请求。
     */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),

    /**
     * HTTP版本不受支持。服务器不支持请求的 HTTP 版本。
     */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported"),

    /**
     * 变体也进行协商。服务器遇到了一个内部配置错误。
     */
    VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),

    /**
     * 存储不足。服务器无法完成请求，因为服务器上没有足够的存储空间。
     */
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),

    /**
     * 检测到循环。服务器在处理请求时检测到了一个循环。
     */
    LOOP_DETECTED(508, "Loop Detected"),

    /**
     * 带宽限制超出。服务器无法完成请求，因为客户端超出了其带宽限制。
     */
    BANDWIDTH_LIMIT_EXCEEDED(509, "Bandwidth Limit Exceeded"),

    /**
     * 未扩展。服务器无法完成请求，因为服务器不理解请求头中的某些扩展。
     */
    NOT_EXTENDED(510, "Not Extended"),

    /**
     * 需要网络认证。客户端需要进行网络认证才能访问请求的资源。
     */
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),

    /**
     * 未知状态。未知的HTTP状态码。
     */
    UNKNOWN(-1, "Unknown Status");

    // HTTP 状态码
    private final int code;
    // HTTP 状态码描述
    private final String description;

    /**
     * 构造函数。
     *
     * @param code 状态码
     * @param description 状态码描述
     */
    HttpStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码获取 HttpStatus 对象。
     *
     * @param code 状态码
     * @return 对应的 HttpStatus 对象
     */
    public static HttpStatus getStatusFromCode(int code) {
        for (HttpStatus status : HttpStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /**
     * 获取状态码。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态码描述。
     *
     * @return 状态码描述
     */
    public String getDescription() {
        return description;
    }
}
