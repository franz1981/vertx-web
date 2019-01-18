package io.vertx.ext.web.impl;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
@Fork(2)
public class RouterMatcherBench
{
  private static final Comparator<RouteImpl> routeComparator = (RouteImpl o1, RouteImpl o2) -> {
    // we keep a set of handlers ordered by its "order" property
    final int compare = Integer.compare(o1.order(), o2.order());
    // since we are defining the comparator to order the set we must be careful because the set
    // will use the comparator to compare the identify of the handlers and if they are the same order
    // are assumed to be the same comparator and therefore removed from the set.

    // if the 2 routes being compared by its order have the same order property value,
    // then do a more expensive equality check and if and only if the are the same we
    // do return 0, meaning same order and same identity.
    if (compare == 0)
    {
      if (o1.equals(o2))
      {
        return 0;
      }
      // otherwise we return higher so if 2 routes have the same order the second one will be considered
      // higher so it is added after the first.
      return 1;
    }
    return compare;
  };

  private RouteImpl route;
  private RoutingContextImplBase base;
  private RouterImpl router;

  @Param( {"/foo"})
  private String path = "/foo";

  @Param( {"/:bar"})
  private String additionalPath = "/:bar";

  private String mountPoint;
  private String requestPath;
  private Set<RouteImpl> routes;
  @Param( {"true", "false"})
  private boolean withRegExp = true;


  @Setup
  public void initRouter()
  {
    mountPoint = "/";//path + additionalPath;
    requestPath = path + "123";
    router = (RouterImpl) Router.router(Vertx.vertx());
    route = (RouteImpl) (withRegExp ? router.routeWithRegex(".*") : router.get(path + additionalPath));
    routes = new ConcurrentSkipListSet<>(routeComparator);
    routes.add(route);
    base = new RoutingContextImpl(null, router, new FakeHttpSeverRequest(), routes);
    route.handler(routingContext -> {
    });
  }

  @Benchmark
  @GroupThreads(1)
  public boolean matchRoute()
  {
    return route.matches(base, mountPoint, false);
  }

  @Benchmark
  @GroupThreads(2)
  public boolean matchRoute2()
  {
    return route.matches(base, mountPoint, false);
  }

  @Benchmark
  @GroupThreads(3)
  public boolean matchRoute3()
  {
    return route.matches(base, mountPoint, false);
  }


  @TearDown
  public synchronized void destroy()
  {
    router.clear();
  }

  public static void main(String[] args) throws RunnerException
  {
    final Options opt = new OptionsBuilder()
      .include(RouterMatcherBench.class.getSimpleName())
      //worst case scenario: no biased locking means just no optimizations for single threaded code
      //diagnostic debug non safepoints are enabled for https://github.com/jvm-profiling-tools/async-profiler and JFR
      .jvmArgs("-XX:-UseBiasedLocking", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints")
      .shouldDoGC(true)
      .addProfiler(GCProfiler.class)
      .build();
    new Runner(opt).run();
  }


  private class FakeHttpSeverRequest implements HttpServerRequest
  {

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler)
    {
      return null;
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler)
    {
      return null;
    }

    @Override
    public HttpServerRequest pause()
    {
      return null;
    }

    @Override
    public HttpServerRequest resume()
    {
      return null;
    }

    @Override
    public HttpServerRequest fetch(long l)
    {
      return null;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> handler)
    {
      return null;
    }

    @Override
    public HttpVersion version()
    {
      return null;
    }

    @Override
    public HttpMethod method()
    {
      return HttpMethod.GET;
    }

    @Override
    public String rawMethod()
    {
      return null;
    }

    @Override
    public boolean isSSL()
    {
      return false;
    }

    @Override
    public @Nullable String scheme()
    {
      return null;
    }

    @Override
    public String uri()
    {
      return requestPath;
    }

    @Override
    public @Nullable String path()
    {
      return requestPath;
    }

    @Override
    public @Nullable String query()
    {
      return null;
    }

    @Override
    public @Nullable String host()
    {
      return null;
    }

    @Override
    public long bytesRead()
    {
      return 0;
    }

    @Override
    public HttpServerResponse response()
    {
      return null;
    }

    @Override
    public MultiMap headers()
    {
      return null;
    }

    @Override
    public @Nullable String getHeader(String s)
    {
      return null;
    }

    @Override
    public String getHeader(CharSequence charSequence)
    {
      return null;
    }

    @Override
    public MultiMap params()
    {
      return null;
    }

    @Override
    public @Nullable String getParam(String s)
    {
      return null;
    }

    @Override
    public SocketAddress remoteAddress()
    {
      return null;
    }

    @Override
    public SocketAddress localAddress()
    {
      return null;
    }

    @Override
    public SSLSession sslSession()
    {
      return null;
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException
    {
      return new X509Certificate[0];
    }

    @Override
    public String absoluteURI()
    {
      return null;
    }

    @Override
    public NetSocket netSocket()
    {
      return null;
    }

    @Override
    public HttpServerRequest setExpectMultipart(boolean b)
    {
      return null;
    }

    @Override
    public boolean isExpectMultipart()
    {
      return false;
    }

    @Override
    public HttpServerRequest uploadHandler(@Nullable Handler<HttpServerFileUpload> handler)
    {
      return null;
    }

    @Override
    public MultiMap formAttributes()
    {
      return null;
    }

    @Override
    public @Nullable String getFormAttribute(String s)
    {
      return null;
    }

    @Override
    public ServerWebSocket upgrade()
    {
      return null;
    }

    @Override
    public boolean isEnded()
    {
      return false;
    }

    @Override
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler)
    {
      return null;
    }

    @Override
    public HttpConnection connection()
    {
      return null;
    }

    @Override
    public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler)
    {
      return null;
    }
  }

}
