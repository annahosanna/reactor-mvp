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
  // I haven't tried tuning the ChannelOptions yet, just put in some recommened values
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
            res.status(200);

            String str = new String(
              "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"3 3 16 16\"><g transform=\"matrix(1.99997 0 0 1.99997-10.994-2071.68)\" fill=\"#da4453\"><rect y=\"1037.36\" x=\"7\" height=\"8\" width=\"8\" fill=\"#32c671\" rx=\"4\"/><path d=\"m123.86 12.966l-11.08-11.08c-1.52-1.521-3.368-2.281-5.54-2.281-2.173 0-4.02.76-5.541 2.281l-53.45 53.53-23.953-24.04c-1.521-1.521-3.368-2.281-5.54-2.281-2.173 0-4.02.76-5.541 2.281l-11.08 11.08c-1.521 1.521-2.281 3.368-2.281 5.541 0 2.172.76 4.02 2.281 5.54l29.493 29.493 11.08 11.08c1.52 1.521 3.367 2.281 5.54 2.281 2.172 0 4.02-.761 5.54-2.281l11.08-11.08 58.986-58.986c1.52-1.521 2.281-3.368 2.281-5.541.0001-2.172-.761-4.02-2.281-5.54\" fill=\"#fff\" transform=\"matrix(.0436 0 0 .0436 8.177 1039.72)\" stroke=\"none\" stroke-width=\"9.512\"/></g></svg>"
            );
            // System.out.println(str);
            // "<!DOCTYPE html><html><head></head><body>Thanks for visiting</body></html>\n"
            res.header("content-type", "image/svg+xml");
            res.header("content-length", String.valueOf(str.length()));
            Mono<String> responseContent = Mono.just(str);

            return res.sendString(responseContent);
          })
      )
      .bindNow();
    Mono.when(disposableServer.onDispose()).block();
  }
}
