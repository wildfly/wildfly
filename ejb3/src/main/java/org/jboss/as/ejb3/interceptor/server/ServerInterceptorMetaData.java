package org.jboss.as.ejb3.interceptor.server;

public class ServerInterceptorMetaData {
    private final String module;
    private final String clazz;

    public ServerInterceptorMetaData(final String module, final String clazz){
        this.module = module;
        this.clazz = clazz;
    }

    public String getModule() {
        return module;
    }

    public String getClazz() {
        return clazz;
    }
}
