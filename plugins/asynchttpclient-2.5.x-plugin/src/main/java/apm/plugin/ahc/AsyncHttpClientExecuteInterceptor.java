package apm.plugin.ahc;

import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.asynchttpclient.*;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;

public class AsyncHttpClientExecuteInterceptor implements InstanceMethodsAroundInterceptor {
  private static Logger logger = LoggerFactory.getLogger(AsyncHttpClientExecuteInterceptor.class);

  @Override
  public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
    Request request = (Request) allArguments[0];
    AsyncHandler<?> handler = (AsyncHandler<?>) allArguments[1];

    final ContextCarrier carrier = new ContextCarrier();
//    CarrierItem items = carrier.items();
//    HttpHeaders headers = request.getHeaders();
//    while (items.hasNext()) {
//      items = items.next();
//      String value = headers.get(items.getHeadKey());
//      if (value != null && !value.isEmpty()) {
//        items.setHeadValue(value);
//      }
//    }

    Uri uri = request.getUri();
    final String operationName = uri.getPath();
    final String remotePeer = String.format(Locale.getDefault(), "%s:%d", uri.getHost(), uri.getExplicitPort());
    final AbstractSpan span = ContextManager.createExitSpan(operationName, carrier, remotePeer);
    span.tag(new StringTag("http.host"), request.getVirtualHost());
    Tags.URL.set(span, request.getUrl());
    Tags.HTTP.METHOD.set(span, request.getMethod());
    request.getHeaders().forEach(entry -> {
      span.tag(new StringTag("http.header." + entry.getKey()), entry.getValue());
    });
    SpanLayer.asHttp(span);
    span.setComponent(ComponentsDefine.HTTP_ASYNC_CLIENT);
    span.prepareForAsync();

    RequestBuilder builder = new RequestBuilder(request);
    CarrierItem next = carrier.items();
    while (next.hasNext()) {
      next = next.next();
      builder.setHeader(next.getHeadKey(), next.getHeadValue());
    }
    allArguments[0] = builder.build();

    allArguments[1] = Proxy.newProxyInstance(
      AsyncHandler.class.getClassLoader(),
      new Class[]{AsyncHandler.class},
      new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          Object result = method.invoke(handler, args);
          if (method.getName().equals("onCompleted")) {
            Response response = (Response) result;
            Tags.STATUS_CODE.set(span, String.valueOf(response.getStatusCode()));
            if (response.getStatusCode() > 400) {
              span.errorOccurred();
            }
          }
          return result;
        }
      }
    );
  }

  @Override
  public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
    ListenableFuture<?> future = (ListenableFuture<?>) ret;
    AbstractSpan span = ContextManager.activeSpan();
    future.addListener(() -> {
      try {
        span.asyncFinish();
      } catch (Throwable t) {
        span.errorOccurred().log(t);
      }
    }, null);
    ContextManager.stopSpan(span);
    return ret;
  }

  @Override
  public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
    ContextManager.activeSpan().errorOccurred().log(t);
  }
}
