package apm.plugin.ahc.define;

import net.bytebuddy.description.method.MethodDescription;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Request;
import org.junit.Test;

import static org.junit.Assert.*;

public class AsyncHttpClientInstrumentationTest {

  @Test
  public void getInstanceMethodsInterceptPoints() throws NoSuchMethodException {
    AsyncHttpClientInstrumentation instrumentation = new AsyncHttpClientInstrumentation();
    InstanceMethodsInterceptPoint point = instrumentation.getInstanceMethodsInterceptPoints()[0];
    MethodDescription.ForLoadedMethod method = new MethodDescription.ForLoadedMethod(
      DefaultAsyncHttpClient.class.getDeclaredMethod("execute", Request.class, AsyncHandler.class)
    );
    assertTrue(point.getMethodsMatcher().matches(method));
  }

}
