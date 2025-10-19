package logbook.server.proxy;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Queue;

import logbook.config.AppConfig;
import logbook.constants.AppConstants;
import logbook.gui.ApplicationMain;
import logbook.internal.LoggerHolder;
import net.lightbody.bmp.mitm.KeyStoreFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.ChainedProxyManager;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.MitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

/**
 * プロキシサーバーです
 *
 */
public final class ProxyServer {

    private static final LoggerHolder LOG = new LoggerHolder(ProxyServer.class);

    private static HttpProxyServer server;

    private static String host;
    private static int port;
    private static String proxyHost;
    private static int proxyPort;
    private static boolean isTrustAllServers;

    public static void start() {
        try {
            MitmManager mitmManager = ImpersonatingMitmManager.builder()
                    .rootCertificateSource(new KeyStoreFileCertificateSource(
                            "PKCS12",
                            AppConstants.PKCS12_FILE,
                            "logbook",
                            AppConstants.PKCS12_PASSWORD))
                    .trustAllServers(isTrustAllServers)
                    .build();
            updateSetting();

            try {
                InetSocketAddress address = host != null ? new InetSocketAddress(host, port)
                        : new InetSocketAddress(port);
                HttpProxyServerBootstrap serverBootstrap = DefaultHttpProxyServer.bootstrap()
                        .withAddress(address)
                        .withManInTheMiddle(mitmManager)
                        .withFiltersSource(new HttpFiltersSourceAdapter() {
                            @Override
                            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                                return new JsonLoggingFilter(originalRequest, ctx);
                            }
                        });
                // lookupChainedProxies内で振り分けようとしたら上手くいかなかった
                if (AppConfig.get().isUseProxy()) {
                    // 上流プロキシ使用有無
                    server = serverBootstrap.withChainProxyManager(new ChainedProxyManager() {
                        @Override
                        public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> chainedProxies) {
                            chainedProxies.add(new ChainedProxyAdapter() {
                                @Override
                                public InetSocketAddress getChainedProxyAddress() {
                                    // 上流プロキシのホストとポート
                                    return new InetSocketAddress(proxyHost, proxyPort);
                                }
                            });
                        }
                    }).start();
                }
                else {
                    server = serverBootstrap.start();
                }
            } catch (Exception e) {
                handleException(e);
            }
        } catch (Exception e) {
            LOG.get().fatal("Proxyサーバーの起動に失敗しました", e);
            throw new RuntimeException(e);
        }
    }

    public static void restart() {
        try {
            if (server == null) {
                return;
            }
            if (updateSetting()) {
                server.stop();
                start();
                ApplicationMain.logPrint("プロキシサーバを再起動しました");
            }
        } catch (Exception e) {
            LOG.get().fatal("Proxyサーバーの起動に失敗しました", e);
            throw new RuntimeException(e);
        }
    }

    public static void end() {
        try {
            if (server != null) {
                server.stop();
                server = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * AppConfigの設定をローカルにコピーします。その際、更新があったか判定します。
     * @return 更新があった
     */
    private static boolean updateSetting() {
        String newHost = null;
        if (AppConfig.get().isAllowOnlyFromLocalhost() && AppConfig.get().isCloseOutsidePort()) {
            newHost = "localhost";
        }
        int newPort = AppConfig.get().getListenPort();
        String newProxyHost = null;
        int newProxyPort = 0;
        if (AppConfig.get().isUseProxy()) {
            newProxyHost = AppConfig.get().getProxyHost();
            newProxyPort = AppConfig.get().getProxyPort();
        }

        boolean isNewTrustAllServers = AppConfig.get().isTrustAllServers(); 

        if (StringUtils.equals(newHost, host) && (newPort == port) &&
                StringUtils.equals(newProxyHost, proxyHost) && (newProxyPort == proxyPort) &&
                isTrustAllServers == isNewTrustAllServers) {
            return false;
        }

        host = newHost;
        port = newPort;
        proxyHost = newProxyHost;
        proxyPort = newProxyPort;
        isTrustAllServers = isNewTrustAllServers;
        return true;
    }

    private static void handleException(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("プロキシサーバーが予期せず終了しました").append("\r\n");
        sb.append("例外 : " + e.getClass().getName()).append("\r\n");
        sb.append("原因 : " + e.getMessage()).append("\r\n");
        if (e instanceof BindException) {
            sb.append("おそらく、二重起動か同じポートを使用しているアプリケーションがあります。").append("\r\n");
        }

        final String message = sb.toString();

        Display.getDefault().asyncExec(() -> {
            MessageBox box = new MessageBox(ApplicationMain.main.getShell(), SWT.YES | SWT.ICON_ERROR);
            box.setText("プロキシサーバーが予期せず終了しました");
            box.setMessage(message);
            box.open();
        });
    }
}
