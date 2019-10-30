package org.jboss.as.ee.component;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.invocation.InterceptorContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that represents an instance of a component client. It should only be referenced from client
 * post construct interceptors.
 *
 * This stores all the context data for the client, such as the SFSB session ID etc.
 *
 * Previously this was achieved using stateful interceptor chains. This information can only be set during view
 * construction for thread safety reasons. If mutable data is required then a mutable and thread safe structure should
 * be inserted into the map at construction time.
 *
 * The class is only used at component creation time, after that the information that is contains is attached to the
 * private data of the interceptor context.
 *
 * @author Stuart Douglas
 */
public class ComponentClientInstance implements Serializable {

    private final Map<Object, Object> contextInformation = new HashMap<Object, Object>();
    private volatile boolean constructionComplete = false;

    public Object getViewInstanceData(final Object key) {
        return contextInformation.get(key);
    }

    public void setViewInstanceData(final Object key, final Object data) {
        if(constructionComplete) {
            throw EeLogger.ROOT_LOGGER.instanceDataCanOnlyBeSetDuringConstruction();
        }
        contextInformation.put(key, data);
    }

    void prepareInterceptorContext(InterceptorContext interceptorContext){
        for(Map.Entry<Object, Object> entry : contextInformation.entrySet()) {
            interceptorContext.putPrivateData(entry.getKey(), entry.getValue());
        }
    }

    void constructionComplete() {
        constructionComplete = true;
    }

}
