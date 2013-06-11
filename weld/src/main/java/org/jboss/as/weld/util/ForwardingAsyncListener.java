package org.jboss.as.weld.util;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

/**
 * Forwarding implementation of {@link AsyncListener}
 *
 * @author Jozef Hartinger
 *
 */
public abstract class ForwardingAsyncListener implements AsyncListener {

    protected abstract AsyncListener delegate();

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        delegate().onComplete(event);
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        delegate().onTimeout(event);
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        delegate().onError(event);
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        delegate().onStartAsync(event);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ForwardingAsyncListener) {
            ForwardingAsyncListener that = (ForwardingAsyncListener) obj;
            return delegate().equals(that.delegate());
        }
        return delegate().equals(obj);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
