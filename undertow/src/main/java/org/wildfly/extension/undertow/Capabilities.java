/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public final class Capabilities {
    /*
    Capabilities in this subsystem
     */
    public static final String CAPABILITY_UNDERTOW = "org.wildfly.undertow";
    public static final String CAPABILITY_LISTENER = "org.wildfly.undertow.listener";
    @Deprecated(forRemoval = true) public static final String CAPABILITY_SERVER = Server.SERVICE_DESCRIPTOR.getName();
    public static final String CAPABILITY_SERVER_LISTENER = "org.wildfly.undertow.server.listener";
    @Deprecated(forRemoval = true) public static final String CAPABILITY_HOST = Host.SERVICE_DESCRIPTOR.getName();
    public static final String CAPABILITY_HOST_SSO = "org.wildfly.undertow.host.sso";
    public static final String CAPABILITY_LOCATION = "org.wildfly.undertow.host.location";
    public static final String CAPABILITY_ACCESS_LOG = "org.wildfly.undertow.host.access-log";
    public static final String CAPABILITY_CONSOLE_ACCESS_LOG = "org.wildfly.undertow.host.console-access-log";
    public static final String CAPABILITY_HANDLER = "org.wildfly.extension.undertow.handler";
    public static final String CAPABILITY_FILTER = "org.wildfly.extension.undertow.filter";
    public static final String CAPABILITY_HOST_FILTER_REF = "org.wildfly.extension.undertow.host.filter-ref";
    public static final String CAPABILITY_LOCATION_FILTER_REF = "org.wildfly.extension.undertow.location.filter-ref";
    public static final String CAPABILITY_SERVLET_CONTAINER = "org.wildfly.undertow.servlet-container";
    public static final String CAPABILITY_WEBSOCKET = "org.wildfly.undertow.servlet-container.websocket";
    public static final String CAPABILITY_HTTP_INVOKER = "org.wildfly.undertow.http-invoker";
    public static final String CAPABILITY_HTTP_INVOKER_HOST = "org.wildfly.undertow.http-invoker.host";
    public static final String CAPABILITY_HTTP_UPGRADE_REGISTRY = "org.wildfly.undertow.listener.http-upgrade-registry";
    public static final String CAPABILITY_APPLICATION_SECURITY_DOMAIN = "org.wildfly.undertow.application-security-domain";
    public static final String CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS = "org.wildfly.undertow.application-security-domain.known-deployments";
    public static final String CAPABILITY_REVERSE_PROXY_HANDLER_HOST = "org.wildfly.undertow.reverse-proxy.host";
    public static final String CAPABILITY_BYTE_BUFFER_POOL = "org.wildfly.undertow.byte-buffer-pool";

    /*
    References to capabilities outside of the subsystem
     */

    public static final String REF_IO_WORKER = "org.wildfly.io.worker";
    public static final String REF_SECURITY_DOMAIN = "org.wildfly.security.security-domain";
    public static final String REF_SSL_CONTEXT = "org.wildfly.security.ssl-context";
    public static final String REF_HTTP_AUTHENTICATION_FACTORY = "org.wildfly.security.http-authentication-factory";
    public static final String REF_HTTP_LISTENER_REGISTRY = "org.wildfly.remoting.http-listener-registry";
    public static final String REF_REQUEST_CONTROLLER = "org.wildfly.request-controller";
    public static final String REF_SUSPEND_CONTROLLER = "org.wildfly.server.suspend-controller";
}
