package org.jboss.as.test.integration.ejb.interceptor.annotatedejbclient;


import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
public class TestSLSB implements TestRemote {

    @Resource
    private SessionContext context;


    private String wasClientInterceptorInvoked() {
        return (String) context.getContextData().get("ClientInterceptorInvoked");
    }

    public int invoke(String id) {
        String interceptorInvocationCounterKey = id + "-COUNT";
        String clientInterceptor = wasClientInterceptorInvoked();
        context.getContextData().put("ClientInterceptorInvoked", clientInterceptor);
        return (Integer) context.getContextData().get(interceptorInvocationCounterKey);
    }

}
