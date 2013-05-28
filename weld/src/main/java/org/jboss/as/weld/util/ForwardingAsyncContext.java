package org.jboss.as.weld.util;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Forwarding implementation of {@link AsyncContext}.
 *
 * @author Jozef Hartinger
 *
 */
public abstract class ForwardingAsyncContext implements AsyncContext {

    protected abstract AsyncContext delegate();

    @Override
    public ServletRequest getRequest() {
        return delegate().getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return delegate().getResponse();
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return delegate().hasOriginalRequestAndResponse();
    }

    @Override
    public void dispatch() {
        delegate().dispatch();
    }

    @Override
    public void dispatch(String path) {
        delegate().dispatch(path);
    }

    @Override
    public void dispatch(ServletContext context, String path) {
        delegate().dispatch(context, path);
    }

    @Override
    public void complete() {
        delegate().complete();
    }

    @Override
    public void start(Runnable run) {
        delegate().start(run);
    }

    @Override
    public void addListener(AsyncListener listener) {
        delegate().addListener(listener);
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        delegate().addListener(listener, servletRequest, servletResponse);
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz) throws ServletException {
        return delegate().createListener(clazz);
    }

    @Override
    public void setTimeout(long timeout) {
        delegate().setTimeout(timeout);
    }

    @Override
    public long getTimeout() {
        return delegate().getTimeout();
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ForwardingAsyncContext) {
            ForwardingAsyncContext that = (ForwardingAsyncContext) obj;
            return delegate().equals(that.delegate());
        }
        return delegate().equals(obj);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
