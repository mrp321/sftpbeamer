package no.neic.ssh;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import no.neic.ssh.constants.ConfigName;
import no.neic.ssh.constants.UrlPath;

import javax.ws.rs.core.MediaType;

public final class HttpVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(HttpVerticle.class);


    @Override
    public void start() {

        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setUsePooledBuffers(true);
        HttpServer httpServer = vertx.createHttpServer(httpServerOptions);
        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());

        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        router.route("/ssh/*").handler(BodyHandler.create());

        router.get(UrlPath.SSH_INFO).produces(MediaType.APPLICATION_JSON).handler(HttpRequestHandler::fetchInfoHandler);

        router.post(UrlPath.SSH_LOGIN).consumes(MediaType.APPLICATION_JSON).produces(MediaType.APPLICATION_JSON).blockingHandler(HttpRequestHandler::loginHandler, false);

        router.post(UrlPath.SSH_CONNECT).consumes(MediaType.APPLICATION_JSON).blockingHandler(HttpRequestHandler::connectHandler, false);

        router.delete(UrlPath.SSH_DISCONNECT).produces(MediaType.APPLICATION_JSON).handler(HttpRequestHandler::disconnectHandler);

        router.route().handler(StaticHandler.create());
        router.route("/static/*").handler(StaticHandler.create());

        httpServer.requestHandler(router::accept).listen(
                Integer.parseInt(Config.valueOf(ConfigName.HTTP_VERTICLE_PORT)),
                Config.valueOf(ConfigName.HOST));

    }
}
