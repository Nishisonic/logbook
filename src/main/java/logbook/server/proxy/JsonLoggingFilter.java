package logbook.server.proxy;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.eclipse.swt.widgets.Display;
import org.littleshoot.proxy.HttpFiltersAdapter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import logbook.config.AppConfig;
import logbook.data.Data;
import logbook.data.DataType;
import logbook.data.UndefinedData;
import logbook.data.context.GlobalContext;
import logbook.internal.LoggerHolder;

public class JsonLoggingFilter extends HttpFiltersAdapter {

    private static final LoggerHolder LOG = new LoggerHolder(JsonLoggingFilter.class);

    private ByteArrayOutputStream requestBodyBuffer;
    private ByteArrayOutputStream responseBodyBuffer;
    private HttpRequest request;
    private HttpResponse response;
    private boolean isHttps;
    private boolean isLoopback;

    public JsonLoggingFilter(HttpRequest originalRequest, ChannelHandlerContext ctx) {
        super(originalRequest, ctx);
        this.requestBodyBuffer = new ByteArrayOutputStream();
        this.responseBodyBuffer = new ByteArrayOutputStream();
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = remoteAddress.getAddress().getHostAddress();
        isLoopback = ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1");
    }

    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (AppConfig.get().isAllowOnlyFromLocalhost() && !AppConfig.get().isCloseOutsidePort()) {
            if (!isLoopback) {
                // リモートホストがローカルループバックアドレス以外の場合400を返し通信しない
                String message = "400 Bad Request - Access denied";
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.BAD_REQUEST,
                        Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.UTF_8)));
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, message.length());
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                return response;
            }
        }
        if (httpObject instanceof HttpRequest) {
            this.request = (HttpRequest) httpObject;
            this.isHttps = this.request.method().equals(HttpMethod.CONNECT);
        }
        if (httpObject instanceof HttpContent) {
            HttpContent content = (HttpContent) httpObject;
            ByteBuf buf = content.content();
            ByteBuf copied = buf.copy();
            try {
                byte[] bytes = new byte[copied.readableBytes()];
                copied.readBytes(bytes);
                this.requestBodyBuffer.write(bytes, 0, bytes.length);
            } finally {
                copied.release();
            }
        }

        return null;
    }

    @Override
    public HttpObject proxyToClientResponse(HttpObject httpObject) {
        if (httpObject instanceof HttpResponse) {
            this.response = (HttpResponse) httpObject;
        }
        if (httpObject instanceof HttpContent) {
            HttpContent content = (HttpContent) httpObject;
            ByteBuf buf = content.content();
            ByteBuf copied = buf.copy();
            try {
                byte[] bytes = new byte[copied.readableBytes()];
                copied.readBytes(bytes);
                this.responseBodyBuffer.write(bytes, 0, bytes.length);
            } finally {
                copied.release();
            }

            if (httpObject instanceof LastHttpContent) {
                try {
                    this.onResponseSuccess();
                } catch (URISyntaxException e) {
                    LOG.get().warn("受信データ処理に失敗", e);
                }
            }
        }

        return httpObject;
    }

    private void onResponseSuccess() throws URISyntaxException {
        if (Filter.isNeed(this.request.headers().get(HttpHeaderNames.HOST),
                this.response.headers().get(HttpHeaderNames.CONTENT_TYPE))) {
            if (this.requestBodyBuffer != null) {
                final String serverName = this.request.headers().get(HttpHeaderNames.HOST);
                String path = new URI(this.request.uri()).getPath();
                String fullUrl = (this.isHttps ? "https" : "http") + "://" + serverName + path;
                final UndefinedData rawData = new UndefinedData(fullUrl, path,
                        this.requestBodyBuffer.toByteArray(), this.responseBodyBuffer.toByteArray());
                final String contentEncoding = this.response.headers().get(HttpHeaderNames.CONTENT_ENCODING);

                Display.getDefault().asyncExec(() -> {
                    try {
                        UndefinedData decodedData = rawData.decode(contentEncoding);

                        // キャプチャしたバイト配列は何のデータかを決定する
                        Data data = decodedData.toDefinedData();
                        if (data.getDataType() != DataType.UNDEFINED) {
                            try {
                                // 定義済みのデータの場合にキューに追加する
                                GlobalContext.updateContext(data);

                            } catch (Exception e) {
                                LOG.get().warn("データ更新に失敗", e);
                            }

                            // サーバー名が不明の場合、サーバー名をセットする
                            if (!Filter.isServerDetected()) {
                                Filter.setServerName(serverName);
                            }

                            // TsunDB(https://tsundb.kc3.moe/api/)に送信する
                            TsunDBClient.send(data);
                        }
                    } catch (Exception e) {
                        LOG.get().warn("受信データ処理に失敗", e);
                    }
                });
            }
        }
    }
}
