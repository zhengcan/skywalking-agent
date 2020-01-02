package apm.plugin.ahc.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class AsyncHttpClientInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
  @Override
  protected ClassMatch enhanceClass() {
    return byName("org.asynchttpclient.DefaultAsyncHttpClient");
  }

  @Override
  public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
    return new ConstructorInterceptPoint[0];
  }

  @Override
  public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
    return new InstanceMethodsInterceptPoint[]{
      new InstanceMethodsInterceptPoint() {
        @Override
        public ElementMatcher<MethodDescription> getMethodsMatcher() {
          return named("execute")
            .and(takesArguments(2))
            .and(takesArgument(0, named("org.asynchttpclient.Request"))
              .and(takesArgument(1, named("org.asynchttpclient.AsyncHandler"))));
        }

        @Override
        public String getMethodsInterceptor() {
          return "apm.plugin.ahc.AsyncHttpClientExecuteInterceptor";
        }

        @Override
        public boolean isOverrideArgs() {
          return true;
        }
      }
    };
  }
}
