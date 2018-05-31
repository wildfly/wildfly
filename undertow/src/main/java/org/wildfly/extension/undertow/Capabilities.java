/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
    public static final String CAPABILITY_SERVER = "org.wildfly.undertow.server";
    public static final String CAPABILITY_HOST = "org.wildfly.undertow.host";
    public static final String CAPABILITY_HOST_SSO = "org.wildfly.undertow.host.sso";
    public static final String CAPABILITY_LOCATION = "org.wildfly.undertow.host.location";
    public static final String CAPABILITY_ACCESS_LOG = "org.wildfly.undertow.host.access-log";
    public static final String CAPABILITY_HANDLER = "org.wildfly.extension.undertow.handler";
    public static final String CAPABILITY_MOD_CLUSTER_FILTER = "org.wildfly.undertow.mod_cluster-filter";
    public static final String CAPABILITY_SERVLET_CONTAINER = "org.wildfly.undertow.servlet-container";
    public static final String CAPABILITY_WEBSOCKET = "org.wildfly.undertow.servlet-container.websocket";
    public static final String CAPABILITY_HTTP_INVOKER = "org.wildfly.undertow.http-invoker";
    public static final String CAPABILITY_HTTP_INVOKER_HOST = "org.wildfly.undertow.http-invoker.host";
    public static final String CAPABILITY_APPLICATION_SECURITY_DOMAIN = "org.wildfly.undertow.application-security-domain";
    public static final String CAPABILITY_APPLICATION_SECURITY_DOMAIN_KNOWN_DEPLOYMENTS = "org.wildfly.undertow.application-security-domain.known-deployments";
    public static final String CAPABILITY_REVERSE_PROXY_HANDLER_HOST = "org.wildfly.undertow.reverse-proxy.host";
    public static final String CAPABILITY_BYTE_BUFFER_POOL = "org.wildfly.undertow.byte-buffer-pool";

    /*
    References to capabilities outside of the subsystem
     */

    public static final String REF_IO_WORKER = "org.wildfly.io.worker";
    public static final String REF_SECURITY_DOMAIN = "org.wildfly.security.security-domain";
    public static final String REF_SOCKET_BINDING = "org.wildfly.network.socket-binding";
    public static final String REF_SSL_CONTEXT = "org.wildfly.security.ssl-context";
    public static final String REF_HTTP_AUTHENTICATION_FACTORY = "org.wildfly.security.http-authentication-factory";
    public static final String REF_JACC_POLICY = "org.wildfly.security.jacc-policy";
    public static final String REF_OUTBOUND_SOCKET = "org.wildfly.network.outbound-socket-binding";
    public static final String REF_REQUEST_CONTROLLER = "org.wildfly.request-controller";
}
