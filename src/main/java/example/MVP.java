package example;

import io.netty.channel.ChannelOption;
import io.netty.resolver.HostsFileEntriesProvider.Parser;
import java.time.Duration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

public final class MVP {

  // Regarding load testing, right now ephemeral ports are left in time_wait state. When all (16384) ports are used, load testing will report errors.
  // On client, increasing connection timeout results in fewer errors; however, it also results in more open file descriptors - which you might then run out of. The CPU speed affects eventloop speed which in turn affects errors
  // unless a custom event loop is specified multi connection performance is terrible
  // Some sites end response with \n while others do not

  public static void main(String[] args) throws Exception {
    LoopResources eventLoopGroup = LoopResources.create(
      "eventLoopGroup",
      16,
      16,
      true
    );

    HttpServer server = HttpServer.create()
      .runOn(eventLoopGroup)
      .port(80)
      .wiretap(false)
      .compress(false)
      .protocol(HttpProtocol.HTTP11)
      .idleTimeout(Duration.ofMillis(5000))
      .readTimeout(Duration.ofMillis(60000))
      .requestTimeout(Duration.ofMillis(60000))
      .option(ChannelOption.SO_BACKLOG, 16383)
      .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
      .option(ChannelOption.SO_REUSEADDR, true);

    server.warmup();
    DisposableServer disposableServer = server
      .route(routes ->
        routes
          .post("/fortune", (req, res) -> {
            res.header("content-type", "text/html");
            res.status(200);
            Mono<String> responseContent = Flux.from(
              req
                .receive()
                .aggregate()
                .asString()
                .subscribeOn(Schedulers.boundedElastic())
            )
              .next()
              .flatMap(value -> {
                return Mono.just(
                  "<!DOCTYPE html><html><head></head><body>".concat(
                      value
                    ).concat("</body></html>\n")
                );
              })
              .subscribeOn(Schedulers.parallel());

            return res.sendString(responseContent);
          })
          .get("/fortune", (req, res) -> {
            res.header("content-type", "text/html");
            res.status(200);
            String str = new String(
              "<!DOCTYPE html><html><head></head><body>Thanks for visiting</body></html>\n"
            );
            res.header("content-length", String.valueOf(str.length()));
            Mono<String> responseContent = Mono.just(str);

            return res.sendString(responseContent);
          })
      )
      .bindNow();
    Mono.when(disposableServer.onDispose()).block();
  }
}
