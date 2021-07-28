package no.neic.ssh.websocket;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import no.neic.ssh.Config;
import no.neic.ssh.constants.ConfigName;

public class WebSocketVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        HttpServer httpServer = vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>() {
            @Override
            public void handle(ServerWebSocket event) {
                event.textMessageHandler(new Handler<String>() {
                    @Override
                    public void handle(String event) {
                        // TODO 指令处理
                    }
                });
            }
        });
        Router router = Router.router(vertx);
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route("/ssh/*").handler(BodyHandler.create());
        router.route().handler(StaticHandler.create());
        router.route("/static/*").handler(StaticHandler.create());
        httpServer.requestHandler(router::accept).listen(
                Integer.parseInt(Config.valueOf(ConfigName.HTTP_VERTICLE_PORT)),
                Config.valueOf(ConfigName.HOST));
    }
}
