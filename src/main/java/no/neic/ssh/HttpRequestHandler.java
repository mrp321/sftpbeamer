package no.neic.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import no.neic.ssh.constants.ConfigName;
import no.neic.ssh.constants.JsonKey;
import no.neic.ssh.constants.UrlParam;
import no.neic.tryggve.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

public final class HttpRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private static final String SEPARATOR = FileSystems.getDefault().getSeparator();

    private static final String HOST1 = "host1";
    private static final String HOST2 = "host2";

    static void fetchInfoHandler(RoutingContext routingContext) {
        String appInfo = "./conf/app.info.json";
        File file = new File(appInfo);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            if (file.exists()) {
                br = new BufferedReader(new FileReader(appInfo));
            } else {
                br = new BufferedReader(new InputStreamReader(HttpRequestHandler.class.getClassLoader().getResourceAsStream("app.info.json")));
            }
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end(sb.toString());
        } catch (IOException e) {
            logger.error(e);
            routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * This method receives a request of connecting remote host. The request body looks like
     * {"username": "", "password": "", "hostname": "", "port": 12, "otc": "", "source": ""}
     */
    static void loginHandler(RoutingContext routingContext) {

        validateRequestBody(routingContext, () -> {
            JsonObject requestJsonBody = routingContext.getBodyAsJson();
            String userName = requestJsonBody.getString(JsonKey.USERNAME);
            String otc = requestJsonBody.getString(JsonKey.OTC);
            String password = requestJsonBody.getString(JsonKey.PASSWORD);
            String hostName = requestJsonBody.getString(JsonKey.HOSTNAME);
            String port = requestJsonBody.getString(JsonKey.PORT);
            String source = requestJsonBody.getString(JsonKey.SOURCE);

            validateSpecificConstraint(routingContext, () -> (source.equals(HOST1) || source.equals(HOST2)) && StringUtils.isNumeric(port), () -> {
                logger.debug("User {} connects to host {} in port {} through source {}", userName, hostName, port, source);


                Session session = routingContext.session();
                String sessionId = session.id();

                SSHSessionManager sshSessionManager = SSHSessionManager.getManager();
                try {

                    logger.debug("User {} is Connecting to host {}", userName, hostName);
                    String otpHosts = Config.valueOf(ConfigName.OTP_HOSTS);
                    if (otpHosts.contains(hostName)) {
                        sshSessionManager.createSSHSession(sessionId, source, userName, password, otc, hostName, Integer.parseInt(port));
                    } else {
                        sshSessionManager.createSSHSession(sessionId, source, userName, password, hostName, Integer.parseInt(port));
                    }

                    logger.debug("User {} connects to host {} successfully", userName, hostName);
                    routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                } catch (JSchException e) {
                    logger.error("User {} failed to connect to host {}, because error {} happens.", userName, hostName, e.getMessage());
                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end(e.getMessage());
                }
            });
        }, JsonKey.USERNAME, JsonKey.PASSWORD, JsonKey.HOSTNAME, JsonKey.PORT, JsonKey.SOURCE);
    }

    /**
     * This method is used to create a connection, which is dedicated to downloading a file or a folder.
     */
    static void connectHandler(RoutingContext routingContext) {
        validateRequestBody(routingContext, () -> {
            JsonObject requestJsonBody = routingContext.getBodyAsJson();
            String userName = requestJsonBody.getString(JsonKey.USERNAME);
            String otc = requestJsonBody.getString(JsonKey.OTC);
            String password = requestJsonBody.getString(JsonKey.PASSWORD);
            String hostName = requestJsonBody.getString(JsonKey.HOSTNAME);
            String port = requestJsonBody.getString(JsonKey.PORT);
            String source = requestJsonBody.getString(JsonKey.SOURCE);

            validateSpecificConstraint(routingContext, () -> (source.equals(HOST1) || source.equals(HOST2)) && StringUtils.isNumeric(port), () -> {
                String sessionId = routingContext.session().id();

                // TODO 逻辑处理
//                try {
//                    SSHSessionManager.getManager().createDownloadSftpSession(sessionId, source, userName, password, otc, hostName, Integer.valueOf(port));
//
//                    routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
//                } catch (JSchException e) {
//                    logger.error("User {} failed to connect to host {}, because error {} happens.", userName, hostName, e.getMessage());
//                    routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
//                }

            });
        }, JsonKey.USERNAME, JsonKey.PASSWORD, JsonKey.HOSTNAME, JsonKey.PORT, JsonKey.SOURCE);
    }

    /**
     * This method is used to disconnect from the remote host and clean up the kept connection.
     */
    static void disconnectHandler(RoutingContext routingContext) {
        String source = routingContext.request().getParam(UrlParam.SOURCE);
        Session session = routingContext.session();
        String sessionId = session.id();
        if (source == null || source.isEmpty()) {
            SSHSessionManager.getManager().disconnectSSH(sessionId);
        } else {
            SSHSessionManager.getManager().disconnectSSH(sessionId, source);
        }

        routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
    }


    private static void validateRequestBody(RoutingContext routingContext, Runnable runnable, String... params) {
        JsonObject body = routingContext.getBodyAsJson();
        if (body == null || body.size() == 0) {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        } else {
            if (Arrays.stream(params).allMatch((param) -> body.containsKey(param))) {
                runnable.run();
            } else {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
            }
        }
    }

    private static void validateSpecificConstraint(RoutingContext routingContext, Supplier<Boolean> validator, Runnable runnable) {
        if (validator.get()) {
            runnable.run();
        } else {
            routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).end();
        }
    }

}
