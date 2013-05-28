package org.jboss.as.weld.webtier;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jboss.as.weld.util.ForwardingAsyncContext;
import org.jboss.as.weld.util.ForwardingAsyncListener;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.servlet.HttpContextLifecycle;

/**
 * There is a class of scenarios where an {@link AsyncListener} is invoked outside of the servlet context (outside of
 * ServletRequestListener#requestInitialized(javax.servlet.ServletRequestEvent) and
 * ServletRequestListener#requestDestroyed(javax.servlet.ServletRequestEvent) calls). This is due to a design decision in
 * Undertow. This filter takes care of starting and stopping CDI contexts around AsyncListener notifications which happen
 * outside of the servlet context.
 *
 * @author Jozef Hartinger
 *
 */
public class AsyncListenerContextActivationFilter implements Filter {

    @Inject
    private BeanManagerImpl manager;
    private HttpContextLifecycle lifecycle;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.lifecycle = new HttpContextLifecycle(manager);
    }

    @Override
    public void destroy() {
    }

    private void preNotify(AsyncEvent event, NotificationAction action) {
        HttpServletRequest request = (HttpServletRequest) event.getSuppliedRequest();
        lifecycle.requestInitialized(request, request.getServletContext());
    }

    private void postNotify(AsyncEvent event, NotificationAction action) {
        try {
            lifecycle.requestDestroyed((HttpServletRequest) event.getSuppliedRequest());
        } finally {
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        chain.doFilter(wrapRequest(request), response);
    }

    private ServletRequest wrapRequest(ServletRequest request) {
        if (request instanceof HttpServletRequest) {
            final HttpServletRequest delegate = (HttpServletRequest) request;
            return new HttpServletRequestWrapper(delegate);
        }
        return request;
    }

    private class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper {

        public HttpServletRequestWrapper(HttpServletRequest delegate) {
            super(delegate);
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return wrapAsyncContext(super.startAsync());
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IllegalStateException {
            return wrapAsyncContext(super.startAsync(servletRequest, servletResponse));
        }

        @Override
        public AsyncContext getAsyncContext() {
            return wrapAsyncContext(super.getAsyncContext());
        }

        private AsyncContext wrapAsyncContext(AsyncContext delegate) {
            return new AsyncContextWrapper(delegate);
        }
    }

    private class AsyncContextWrapper extends ForwardingAsyncContext {

        private final AsyncContext delegate;

        public AsyncContextWrapper(AsyncContext delegate) {
            this.delegate = delegate;
        }

        @Override
        protected AsyncContext delegate() {
            return delegate;
        }

        @Override
        public void addListener(AsyncListener listener) {
            super.addListener(wrapAsyncListener(listener));
        }

        @Override
        public void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
            super.addListener(wrapAsyncListener(listener), servletRequest, servletResponse);
        }

        private AsyncListener wrapAsyncListener(AsyncListener delegate) {
            return new AsyncListenerWrapper(delegate);
        }
    }

    private class AsyncListenerWrapper extends ForwardingAsyncListener {

        private final AsyncListener delegate;

        public AsyncListenerWrapper(AsyncListener delegate) {
            this.delegate = delegate;
        }

        @Override
        protected AsyncListener delegate() {
            return delegate;
        }

        protected void onEvent(AsyncEvent event, NotificationAction action) throws IOException {
            final boolean contextsActive = lifecycle.getRequestContext().isActive();

            try {
                if (!contextsActive) {
                    preNotify(event, action);
                }
                action.notify(delegate, event);
            } finally {
                if (!contextsActive) {
                    postNotify(event, action);
                }
            }
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            onEvent(event, EventType.ON_COMPLETE);
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            onEvent(event, EventType.ON_TIMEOUT);
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            onEvent(event, EventType.ON_ERROR);
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            onEvent(event, EventType.ON_START_ASYNC);
        }
    }

    private interface NotificationAction {
        void notify(AsyncListener listener, AsyncEvent event) throws IOException;
    }

    private static enum EventType implements NotificationAction {
        ON_COMPLETE {
            @Override
            public void notify(AsyncListener listener, AsyncEvent event) throws IOException {
                listener.onComplete(event);
            }
        },
        ON_TIMEOUT {
            @Override
            public void notify(AsyncListener listener, AsyncEvent event) throws IOException {
                listener.onTimeout(event);
            }
        },
        ON_ERROR {
            @Override
            public void notify(AsyncListener listener, AsyncEvent event) throws IOException {
                listener.onError(event);
            }
        },
        ON_START_ASYNC {
            @Override
            public void notify(AsyncListener listener, AsyncEvent event) throws IOException {
                listener.onStartAsync(event);
            }
        };
    }
}
