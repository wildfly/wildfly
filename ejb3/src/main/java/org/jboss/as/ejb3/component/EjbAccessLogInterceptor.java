/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.subsystem.AccessLogResourceDefinition.AttributeVocabulary;
import org.jboss.as.ejb3.subsystem.AccessLogService;
import org.jboss.ejb.client.SessionID;
import org.jboss.ejb.server.Request;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.ImmediateInterceptorFactory;
import org.wildfly.event.logger.EventLogger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;

/**
 * View interceptor that records one EJB access-log event per invocation.
 *
 * <p>Registered at {@link org.jboss.as.ee.component.interceptors.InterceptorOrder.View#ACCESS_LOG_INTERCEPTOR}
 * (0x280) — after {@code SECURITY_ROLES} (0x270), before
 * {@code EJB_SECURITY_AUTHORIZATION_INTERCEPTOR} (0x300). This position sees the established
 * security identity and captures authorization denials as exception outcomes.
 *
 * <p>At invocation time the interceptor reads the {@link AccessLogHolder} injected into the
 * {@link EJBComponent}.  If the service is not running (access-log resource not present),
 * the interceptor returns immediately with no overhead beyond one volatile read.
 *
 * <p>Local (in-VM) invocations are suppressed when {@code include-local=false} (the default).
 *
 * <p>MDB message-delivery invocations are always suppressed: the {@link InvocationType} private
 * datum is {@link InvocationType#MESSAGE_DELIVERY} for those calls, and access logging records
 * client access, not internal message delivery.
 *
 * <p>The field map is assembled on the invocation thread and handed to {@link EventLogger#log},
 * which queues it; the formatter and writer run on the XNIO I/O worker thread.
 */
public final class EjbAccessLogInterceptor implements Interceptor {

    public static final InterceptorFactory FACTORY = new ImmediateInterceptorFactory(new EjbAccessLogInterceptor());

    private EjbAccessLogInterceptor() {
    }

    @Override
    public Object processInvocation(final InterceptorContext context) throws Exception {
        final EJBComponent ejbComponent = (EJBComponent) context.getPrivateData(Component.class);
        final AccessLogService service = ejbComponent.getAccessLogHolder().get();
        if (service == null) {
            // Access log not configured — fast path: one private-data lookup, one cast,
            // one holder-field read, one volatile read.
            return context.proceed();
        }

        final EventLogger logger = service.getEventLogger();
        if (logger == null) {
            return context.proceed();
        }

        // Suppress MDB message-delivery invocations — those are not client access.
        final InvocationType invocationType = context.getPrivateData(InvocationType.class);
        if (invocationType == InvocationType.MESSAGE_DELIVERY) {
            return context.proceed();
        }

        // Suppress local invocations unless include-local is enabled.
        final Request request = context.getPrivateData(Request.class);
        final boolean isLocal = (request == null);
        if (isLocal && !service.isIncludeLocal()) {
            return context.proceed();
        }

        final long startNanos = System.nanoTime();
        String outcomeValue = "success";
        String exceptionClass = null;
        try {
            return context.proceed();
        } catch (final Throwable t) {
            outcomeValue = "exception";
            exceptionClass = t.getClass().getName();
            throw t;
        } finally {
            final long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            try {
                emit(context, ejbComponent, service, logger, request, invocationType,
                        outcomeValue, exceptionClass, durationMillis);
            } catch (final Throwable t) {
                // Never let logging failures affect the invocation, but count and report the failure.
                service.recordEmitFailure(t);
            }
        }
    }

    private static void emit(
            final InterceptorContext context,
            final EJBComponent ejbComponent,
            final AccessLogService service,
            final EventLogger logger,
            final Request request,
            final InvocationType invocationType,
            final String outcomeValue,
            final String exceptionClass,
            final long durationMillis) {

        final Set<AttributeVocabulary> enabled = service.getEnabledAttributes();
        // Pre-size for the common all-attributes case.
        final Map<String, Object> data = new LinkedHashMap<>(32);

        // --- Bean / view identity ---
        // ComponentView is present on business-view invocations; absent on timeout (timer) views.
        final ComponentView componentView = context.getPrivateData(ComponentView.class);
        EJBComponent ejb = ejbComponent;

        if (ejb != null) {
            if (enabled.contains(AttributeVocabulary.APP)) {
                final String app = ejb.getApplicationName();
                if (app != null && !app.isEmpty()) data.put("app", app);
            }
            if (enabled.contains(AttributeVocabulary.MODULE)) {
                final String module = ejb.getModuleName();
                if (module != null && !module.isEmpty()) data.put("module", module);
            }
            if (enabled.contains(AttributeVocabulary.BEAN)) {
                data.put("bean", ejb.getComponentName());
            }
            if (enabled.contains(AttributeVocabulary.BEAN_CLASS)) {
                data.put("beanClass", ejb.getComponentClass().getName());
            }
        }

        if (componentView != null && enabled.contains(AttributeVocabulary.VIEW)) {
            data.put("view", componentView.getViewClass().getName());
        }

        if (enabled.contains(AttributeVocabulary.METHOD)) {
            final Method method = context.getMethod();
            if (method != null) {
                final StringBuilder sb = new StringBuilder(method.getName()).append('(');
                final Class<?>[] params = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(params[i].getSimpleName());
                }
                sb.append(')');
                data.put("method", sb.toString());
            }
        }

        // --- User ---
        if (enabled.contains(AttributeVocabulary.USER)) {
            final SecurityDomain domain = SecurityDomain.getCurrent();
            if (domain != null) {
                final SecurityIdentity identity = domain.getCurrentSecurityIdentity();
                if (identity != null && !identity.isAnonymous()) {
                    data.put("user", identity.getPrincipal().getName());
                }
            }
        }

        // --- Network fields (remote only) ---
        if (request != null) {
            if (enabled.contains(AttributeVocabulary.REMOTE_ADDRESS)) {
                final SocketAddress peer = request.getPeerAddress();
                if (peer instanceof InetSocketAddress) {
                    data.put("remoteAddress", ((InetSocketAddress) peer).getAddress().getHostAddress());
                    if (enabled.contains(AttributeVocabulary.REMOTE_PORT)) {
                        data.put("remotePort", ((InetSocketAddress) peer).getPort());
                    }
                }
            } else if (enabled.contains(AttributeVocabulary.REMOTE_PORT)) {
                final SocketAddress peer = request.getPeerAddress();
                if (peer instanceof InetSocketAddress) {
                    data.put("remotePort", ((InetSocketAddress) peer).getPort());
                }
            }
            if (enabled.contains(AttributeVocabulary.LOCAL_ADDRESS)) {
                final SocketAddress local = request.getLocalAddress();
                if (local instanceof InetSocketAddress) {
                    data.put("localAddress", ((InetSocketAddress) local).getAddress().getHostAddress());
                    if (enabled.contains(AttributeVocabulary.LOCAL_PORT)) {
                        data.put("localPort", ((InetSocketAddress) local).getPort());
                    }
                }
            } else if (enabled.contains(AttributeVocabulary.LOCAL_PORT)) {
                final SocketAddress local = request.getLocalAddress();
                if (local instanceof InetSocketAddress) {
                    data.put("localPort", ((InetSocketAddress) local).getPort());
                }
            }
            if (enabled.contains(AttributeVocabulary.PROTOCOL)) {
                final String protocol = request.getProtocol();
                if (protocol != null) data.put("protocol", protocol);
            }
            if (enabled.contains(AttributeVocabulary.TRANSPORT)) {
                data.put("transport", deriveTransport(request.getProtocol()));
            }
        } else if (enabled.contains(AttributeVocabulary.TRANSPORT)) {
            // No Request — in-VM (local) invocation.
            data.put("transport", "local");
        }

        // --- Invocation type ---
        if (invocationType != null && enabled.contains(AttributeVocabulary.INVOCATION_TYPE)) {
            data.put("invocationType", invocationType.name());
        }

        // --- Session (SFSB only) ---
        if (enabled.contains(AttributeVocabulary.SESSION_ID)) {
            final SessionID sessionId = context.getPrivateData(SessionID.class);
            if (sessionId != null) {
                data.put("sessionId", sessionId.toString());
            }
        }

        // --- Outcome ---
        if (enabled.contains(AttributeVocabulary.OUTCOME)) {
            data.put("outcome", outcomeValue);
            if (exceptionClass != null && enabled.contains(AttributeVocabulary.EXCEPTION)) {
                data.put("exception", exceptionClass);
            }
        }

        // --- Duration ---
        if (enabled.contains(AttributeVocabulary.DURATION)) {
            data.put("duration", durationMillis);
        }

        // --- Thread name ---
        if (enabled.contains(AttributeVocabulary.THREAD_NAME)) {
            data.put("threadName", Thread.currentThread().getName());
        }

        // --- Node name (added as top-level key, not formatter metadata) ---
        if (service.isIncludeNodeName() && enabled.contains(AttributeVocabulary.NODE_NAME)) {
            final String nodeName = org.wildfly.security.manager.WildFlySecurityManager
                    .getPropertyPrivileged(org.jboss.as.server.ServerEnvironment.NODE_NAME, null);
            if (nodeName != null) {
                data.put("nodeName", nodeName);
            }
        }

        logger.log(data);
    }

    /**
     * Maps {@code Request.getProtocol()} to the stable {@code transport} vocabulary.
     *
     * <p>The mapping is explicit and exhaustive over the known values:
     * <ul>
     *   <li>{@code null} or any value starting with {@code "HTTP/"} — the wildfly-http-client
     *       transport reports the wire version here (e.g. {@code "HTTP/1.1"}, {@code "HTTP/2.0"}).
     *       All such values map to {@code "http"}.
     *   <li>{@code "remoting"} — plain JBoss Remoting → {@code "remoting"}.
     *   <li>{@code "http-remoting"} — Remoting tunnelled over an HTTP upgrade → {@code "http-remoting"}.
     *   <li>{@code "iiop"} — reserved for the IIOP capture point (E7); unreachable in this release.
     *   <li>Any other value — unrecognised; yields {@code "unknown"} rather than silently omitting
     *       the field, so that the record remains queryable even when the protocol is unexpected.
     * </ul>
     *
     * @param protocol the value returned by {@link org.jboss.ejb.server.Request#getProtocol()};
     *                 may be {@code null}
     * @return a stable, non-null transport label
     */
    static String deriveTransport(final String protocol) {
        if (protocol == null) {
            return "unknown";
        }
        if (protocol.startsWith("HTTP/")) {
            return "http";
        }
        switch (protocol) {
            case "remoting":      return "remoting";
            case "http-remoting": return "http-remoting";
            case "iiop":          return "iiop";
            default:              return "unknown";
        }
    }
}
