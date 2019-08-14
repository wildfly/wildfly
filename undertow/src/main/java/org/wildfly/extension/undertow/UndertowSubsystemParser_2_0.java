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

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.operations.common.Util;
import org.wildfly.extension.undertow.filters.BasicAuthHandler;
import org.wildfly.extension.undertow.filters.CustomFilterDefinition;
import org.wildfly.extension.undertow.filters.ErrorPageDefinition;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.extension.undertow.filters.GzipFilter;
import org.wildfly.extension.undertow.filters.ModClusterDefinition;
import org.wildfly.extension.undertow.filters.RequestLimitHandler;
import org.wildfly.extension.undertow.filters.ResponseHeaderFilter;
import org.wildfly.extension.undertow.handlers.FileHandler;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandler;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandlerHost;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowSubsystemParser_2_0 extends PersistentResourceXMLParser {
    private final PersistentResourceXMLDescription xmlDescription;

    UndertowSubsystemParser_2_0() {
        xmlDescription = builder(UndertowRootDefinition.INSTANCE.getPathElement(), Namespace.UNDERTOW_2_0.getUriString())
                .addAttributes(UndertowRootDefinition.DEFAULT_VIRTUAL_HOST, UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER, UndertowRootDefinition.DEFAULT_SERVER, UndertowRootDefinition.INSTANCE_ID)
                .addAttribute(UndertowRootDefinition.STATISTICS_ENABLED)
                .addChild(
                        builder(BufferCacheDefinition.INSTANCE.getPathElement())
                                .addAttributes(BufferCacheDefinition.BUFFER_SIZE, BufferCacheDefinition.BUFFERS_PER_REGION, BufferCacheDefinition.MAX_REGIONS)
                )
                .addChild(builder(ServerDefinition.INSTANCE.getPathElement())
                        .addAttributes(ServerDefinition.DEFAULT_HOST, ServerDefinition.SERVLET_CONTAINER)
                        .addChild(
                                builder(AjpListenerResourceDefinition.INSTANCE.getPathElement())

                                        .addAttributes(AjpListenerResourceDefinition.SCHEME, AjpListenerResourceDefinition.BUFFER_POOL, AjpListenerResourceDefinition.ENABLED, AjpListenerResourceDefinition.SOCKET_BINDING, AjpListenerResourceDefinition.WORKER, ListenerResourceDefinition.REDIRECT_SOCKET)
                                        .addAttribute(ListenerResourceDefinition.RESOLVE_PEER_ADDRESS)
                                        .addAttributes(ListenerResourceDefinition.MAX_HEADER_SIZE, ListenerResourceDefinition.MAX_ENTITY_SIZE,
                                                ListenerResourceDefinition.BUFFER_PIPELINED_DATA, ListenerResourceDefinition.MAX_PARAMETERS, ListenerResourceDefinition.MAX_HEADERS, ListenerResourceDefinition.MAX_COOKIES,ListenerResourceDefinition.ALLOW_ENCODED_SLASH, ListenerResourceDefinition.DECODE_URL,
                                                ListenerResourceDefinition.URL_CHARSET, ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE, ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE, ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                                                ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE, ListenerResourceDefinition.NO_REQUEST_TIMEOUT, ListenerResourceDefinition.REQUEST_PARSE_TIMEOUT)
                                        .addAttributes(ListenerResourceDefinition.BACKLOG, ListenerResourceDefinition.RECEIVE_BUFFER, ListenerResourceDefinition.SEND_BUFFER, ListenerResourceDefinition.KEEP_ALIVE, ListenerResourceDefinition.READ_TIMEOUT, ListenerResourceDefinition.WRITE_TIMEOUT)
                        )
                        .addChild(
                                builder(HttpListenerResourceDefinition.INSTANCE.getPathElement())
                                        .addAttributes(HttpListenerResourceDefinition.BUFFER_POOL, HttpListenerResourceDefinition.CERTIFICATE_FORWARDING, HttpListenerResourceDefinition.ENABLED, HttpListenerResourceDefinition.SOCKET_BINDING, HttpListenerResourceDefinition.WORKER, ListenerResourceDefinition.REDIRECT_SOCKET, HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING, HttpListenerResourceDefinition.ENABLE_HTTP2)
                                        .addAttribute(ListenerResourceDefinition.RESOLVE_PEER_ADDRESS)
                                        .addAttributes(ListenerResourceDefinition.MAX_HEADER_SIZE, ListenerResourceDefinition.MAX_ENTITY_SIZE,
                                                ListenerResourceDefinition.BUFFER_PIPELINED_DATA, ListenerResourceDefinition.MAX_PARAMETERS, ListenerResourceDefinition.MAX_HEADERS, ListenerResourceDefinition.MAX_COOKIES,ListenerResourceDefinition.ALLOW_ENCODED_SLASH, ListenerResourceDefinition.DECODE_URL,
                                                ListenerResourceDefinition.URL_CHARSET, ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE, ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE, ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                                                ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE, ListenerResourceDefinition.NO_REQUEST_TIMEOUT, ListenerResourceDefinition.REQUEST_PARSE_TIMEOUT)
                                        .addAttributes(ListenerResourceDefinition.BACKLOG, ListenerResourceDefinition.RECEIVE_BUFFER, ListenerResourceDefinition.SEND_BUFFER, ListenerResourceDefinition.KEEP_ALIVE, ListenerResourceDefinition.READ_TIMEOUT, ListenerResourceDefinition.WRITE_TIMEOUT)
                        ).addChild(
                                        builder(HttpsListenerResourceDefinition.INSTANCE.getPathElement())
                                                .addAttributes(AjpListenerResourceDefinition.SOCKET_BINDING, AjpListenerResourceDefinition.WORKER, AjpListenerResourceDefinition.BUFFER_POOL, AjpListenerResourceDefinition.ENABLED)
                                                .addAttribute(ListenerResourceDefinition.RESOLVE_PEER_ADDRESS)
                                                .addAttributes(HttpsListenerResourceDefinition.SECURITY_REALM, HttpsListenerResourceDefinition.VERIFY_CLIENT, HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES, HttpsListenerResourceDefinition.ENABLED_PROTOCOLS, HttpsListenerResourceDefinition.ENABLE_HTTP2, HttpsListenerResourceDefinition.ENABLE_SPDY)
                                                .addAttributes(ListenerResourceDefinition.MAX_HEADER_SIZE, ListenerResourceDefinition.MAX_ENTITY_SIZE,
                                                        ListenerResourceDefinition.BUFFER_PIPELINED_DATA, ListenerResourceDefinition.MAX_PARAMETERS, ListenerResourceDefinition.MAX_HEADERS, ListenerResourceDefinition.MAX_COOKIES, ListenerResourceDefinition.ALLOW_ENCODED_SLASH, ListenerResourceDefinition.DECODE_URL,
                                                        ListenerResourceDefinition.URL_CHARSET, ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE, ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE, ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                                                        ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE, ListenerResourceDefinition.NO_REQUEST_TIMEOUT, ListenerResourceDefinition.REQUEST_PARSE_TIMEOUT)
                                                .addAttributes(ListenerResourceDefinition.BACKLOG, ListenerResourceDefinition.RECEIVE_BUFFER, ListenerResourceDefinition.SEND_BUFFER, ListenerResourceDefinition.KEEP_ALIVE, ListenerResourceDefinition.READ_TIMEOUT, ListenerResourceDefinition.WRITE_TIMEOUT)
                                ).addChild(
                                        builder(HostDefinition.INSTANCE.getPathElement())
                                                .addAttributes(HostDefinition.ALIAS, HostDefinition.DEFAULT_WEB_MODULE)
                                                .addChild(
                                                        builder(LocationDefinition.INSTANCE.getPathElement())
                                                                .addAttributes(LocationDefinition.HANDLER)
                                                                .addChild(
                                                                        builder(FilterRefDefinition.INSTANCE.getPathElement())
                                                                                .addAttributes(FilterRefDefinition.PREDICATE, FilterRefDefinition.PRIORITY)
                                                                )
                                                ).addChild(
                                                builder(AccessLogDefinition.INSTANCE.getPathElement())
                                                        .addAttributes(AccessLogDefinition.PATTERN, AccessLogDefinition.DIRECTORY, AccessLogDefinition.RELATIVE_TO, AccessLogDefinition.PREFIX, AccessLogDefinition.SUFFIX, AccessLogDefinition.WORKER, AccessLogDefinition.ROTATE, AccessLogDefinition.USE_SERVER_LOG)
                                        ).addChild(
                                                builder(FilterRefDefinition.INSTANCE.getPathElement())
                                                        .addAttributes(FilterRefDefinition.PREDICATE, FilterRefDefinition.PRIORITY)
                                        ).addChild(
                                                builder(UndertowExtension.PATH_SSO)
                                                        .addAttribute(SingleSignOnDefinition.Attribute.DOMAIN.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.PATH.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.HTTP_ONLY.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.SECURE.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.COOKIE_NAME.getDefinition())
                                        )
                                )
                )
                .addChild(
                        builder(ServletContainerDefinition.INSTANCE.getPathElement())
                                .addAttribute(ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS)
                                .addAttribute(ServletContainerDefinition.DEFAULT_BUFFER_CACHE)
                                .addAttribute(ServletContainerDefinition.STACK_TRACE_ON_ERROR)
                                .addAttribute(ServletContainerDefinition.USE_LISTENER_ENCODING)
                                .addAttribute(ServletContainerDefinition.DEFAULT_ENCODING)
                                .addAttribute(ServletContainerDefinition.IGNORE_FLUSH)
                                .addAttribute(ServletContainerDefinition.EAGER_FILTER_INIT)
                                .addAttribute(ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT)
                                .addAttribute(ServletContainerDefinition.DISABLE_CACHING_FOR_SECURED_PAGES)
                                .addChild(
                                        builder(JspDefinition.INSTANCE.getPathElement())
                                                .setXmlElementName(Constants.JSP_CONFIG)
                                                .addAttributes(
                                                        JspDefinition.DISABLED,
                                                        JspDefinition.DEVELOPMENT,
                                                        JspDefinition.KEEP_GENERATED,
                                                        JspDefinition.TRIM_SPACES,
                                                        JspDefinition.TAG_POOLING,
                                                        JspDefinition.MAPPED_FILE,
                                                        JspDefinition.CHECK_INTERVAL,
                                                        JspDefinition.MODIFICATION_TEST_INTERVAL,
                                                        JspDefinition.RECOMPILE_ON_FAIL,
                                                        JspDefinition.SMAP,
                                                        JspDefinition.DUMP_SMAP,
                                                        JspDefinition.GENERATE_STRINGS_AS_CHAR_ARRAYS,
                                                        JspDefinition.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE,
                                                        JspDefinition.SCRATCH_DIR,
                                                        JspDefinition.SOURCE_VM,
                                                        JspDefinition.TARGET_VM,
                                                        JspDefinition.JAVA_ENCODING,
                                                        JspDefinition.X_POWERED_BY,
                                                        JspDefinition.DISPLAY_SOURCE_FRAGMENT)
                                )
                                .addChild(
                                        builder(SessionCookieDefinition.INSTANCE.getPathElement())
                                                .addAttributes(
                                                        SessionCookieDefinition.NAME,
                                                        SessionCookieDefinition.DOMAIN,
                                                        SessionCookieDefinition.COMMENT,
                                                        SessionCookieDefinition.HTTP_ONLY,
                                                        SessionCookieDefinition.SECURE,
                                                        SessionCookieDefinition.MAX_AGE
                                                )
                                )
                                .addChild(
                                        builder(PersistentSessionsDefinition.INSTANCE.getPathElement())
                                                .addAttributes(
                                                        PersistentSessionsDefinition.PATH,
                                                        PersistentSessionsDefinition.RELATIVE_TO
                                                )
                                )
                                .addChild(
                                        builder(WebsocketsDefinition.INSTANCE.getPathElement())
                                                .addAttributes(
                                                        WebsocketsDefinition.WORKER,
                                                        WebsocketsDefinition.BUFFER_POOL,
                                                        WebsocketsDefinition.DISPATCH_TO_WORKER
                                                )
                                )
                )
                .addChild(
                        builder(HandlerDefinitions.INSTANCE.getPathElement())
                                .setXmlElementName(Constants.HANDLERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(FileHandler.INSTANCE.getPathElement())
                                                .addAttributes(
                                                        FileHandler.PATH,
                                                        FileHandler.CACHE_BUFFER_SIZE,
                                                        FileHandler.CACHE_BUFFERS,
                                                        FileHandler.DIRECTORY_LISTING)
                                )
                                .addChild(
                                        builder(ReverseProxyHandler.INSTANCE.getPathElement())
                                                .addAttributes(
                                                        ReverseProxyHandler.CONNECTIONS_PER_THREAD,
                                                        ReverseProxyHandler.SESSION_COOKIE_NAMES,
                                                        ReverseProxyHandler.PROBLEM_SERVER_RETRY,
                                                        ReverseProxyHandler.MAX_REQUEST_TIME,
                                                        ReverseProxyHandler.REQUEST_QUEUE_SIZE,
                                                        ReverseProxyHandler.CACHED_CONNECTIONS_PER_THREAD,
                                                        ReverseProxyHandler.CONNECTION_IDLE_TIMEOUT)
                                                .addChild(builder(ReverseProxyHandlerHost.INSTANCE.getPathElement())
                                                        .setXmlElementName(Constants.HOST)
                                                        .addAttributes(ReverseProxyHandlerHost.INSTANCE_ID, ReverseProxyHandlerHost.PATH, ReverseProxyHandlerHost.SCHEME, ReverseProxyHandlerHost.OUTBOUND_SOCKET_BINDING))
                                )


                )
                .addChild(
                        builder(FilterDefinitions.INSTANCE.getPathElement())
                                .setXmlElementName(Constants.FILTERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(BasicAuthHandler.INSTANCE.getPathElement())
                                                .addAttributes(BasicAuthHandler.SECURITY_DOMAIN)
                                        .setNoAddOperation(true)
                                )
                                .addChild(
                                        builder(RequestLimitHandler.INSTANCE.getPathElement())
                                                .setXmlElementName("connection-limit")
                                                .addAttributes(RequestLimitHandler.MAX_CONCURRENT_REQUESTS, RequestLimitHandler.QUEUE_SIZE)
                                ).addChild(
                                builder(ResponseHeaderFilter.INSTANCE.getPathElement())
                                        .addAttributes(ResponseHeaderFilter.NAME, ResponseHeaderFilter.VALUE)
                        ).addChild(
                                builder(GzipFilter.INSTANCE.getPathElement())
                        ).addChild(
                                builder(ErrorPageDefinition.INSTANCE.getPathElement())
                                        .addAttributes(ErrorPageDefinition.CODE, ErrorPageDefinition.PATH)
                        ).addChild(
                                builder(ModClusterDefinition.PATH)
                                .addAttributes(ModClusterDefinition.MANAGEMENT_SOCKET_BINDING,
                                        ModClusterDefinition.ADVERTISE_SOCKET_BINDING,
                                        ModClusterDefinition.SECURITY_KEY,
                                        ModClusterDefinition.ADVERTISE_PROTOCOL,
                                        ModClusterDefinition.ADVERTISE_PATH,
                                        ModClusterDefinition.ADVERTISE_FREQUENCY,
                                        ModClusterDefinition.HEALTH_CHECK_INTERVAL,
                                        ModClusterDefinition.BROKEN_NODE_TIMEOUT,
                                        ModClusterDefinition.WORKER,
                                        ModClusterDefinition.MAX_REQUEST_TIME,
                                        ModClusterDefinition.MANAGEMENT_ACCESS_PREDICATE,
                                        ModClusterDefinition.CONNECTIONS_PER_THREAD,
                                        ModClusterDefinition.CACHED_CONNECTIONS_PER_THREAD,
                                        ModClusterDefinition.CONNECTION_IDLE_TIMEOUT,
                                        ModClusterDefinition.REQUEST_QUEUE_SIZE)
                        ).addChild(
                                builder(CustomFilterDefinition.INSTANCE.getPathElement())
                                        .addAttributes(CustomFilterDefinition.CLASS_NAME, CustomFilterDefinition.MODULE, CustomFilterDefinition.PARAMETERS)
                                        .setXmlElementName("filter")
                        )

                )
                 //here to make sure we always add filters & handlers path to mgmt model
                .setAdditionalOperationsGenerator((address, addOperation, operations) -> {
                    operations.add(Util.createAddOperation(address.append(UndertowExtension.PATH_FILTERS)));
                    operations.add(Util.createAddOperation(address.append(UndertowExtension.PATH_HANDLERS)));
                })
                .build();
    }


    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}

