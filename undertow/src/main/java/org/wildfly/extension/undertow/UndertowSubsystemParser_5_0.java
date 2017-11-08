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

import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.operations.common.Util;
import org.wildfly.extension.undertow.filters.CustomFilterDefinition;
import org.wildfly.extension.undertow.filters.ErrorPageDefinition;
import org.wildfly.extension.undertow.filters.ExpressionFilterDefinition;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.extension.undertow.filters.GzipFilter;
import org.wildfly.extension.undertow.filters.ModClusterDefinition;
import org.wildfly.extension.undertow.filters.RequestLimitHandler;
import org.wildfly.extension.undertow.filters.ResponseHeaderFilter;
import org.wildfly.extension.undertow.filters.RewriteFilterDefinition;
import org.wildfly.extension.undertow.handlers.FileHandler;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandler;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandlerHost;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowSubsystemParser_5_0 extends PersistentResourceXMLParser {
    private final PersistentResourceXMLDescription xmlDescription;

    UndertowSubsystemParser_5_0() {
        xmlDescription = builder(UndertowRootDefinition.INSTANCE.getPathElement(), Namespace.UNDERTOW_5_0.getUriString())
                .addAttributes(
                        UndertowRootDefinition.DEFAULT_SERVER,
                        UndertowRootDefinition.DEFAULT_VIRTUAL_HOST,
                        UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER,
                        UndertowRootDefinition.INSTANCE_ID,
                        UndertowRootDefinition.DEFAULT_SECURITY_DOMAIN,
                        UndertowRootDefinition.STATISTICS_ENABLED)
                .addChild(
                        builder(BufferCacheDefinition.INSTANCE.getPathElement())
                                .addAttributes(BufferCacheDefinition.BUFFER_SIZE, BufferCacheDefinition.BUFFERS_PER_REGION, BufferCacheDefinition.MAX_REGIONS)
                )
                .addChild(builder(ServerDefinition.INSTANCE.getPathElement())
                                .addAttributes(ServerDefinition.DEFAULT_HOST, ServerDefinition.SERVLET_CONTAINER)
                                .addChild(
                                        listenerBuilder(AjpListenerResourceDefinition.INSTANCE)
                                                // xsd ajp-listener-type
                                                .addAttributes(AjpListenerResourceDefinition.SCHEME,
                                                        ListenerResourceDefinition.REDIRECT_SOCKET,
                                                        AjpListenerResourceDefinition.MAX_AJP_PACKET_SIZE)
                                )
                                .addChild(
                                        listenerBuilder(HttpListenerResourceDefinition.INSTANCE)
                                                // xsd http-listener-type
                                                .addAttributes(
                                                        HttpListenerResourceDefinition.CERTIFICATE_FORWARDING,
                                                        ListenerResourceDefinition.REDIRECT_SOCKET,
                                                        HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING,
                                                        HttpListenerResourceDefinition.ENABLE_HTTP2,
                                                        HttpListenerResourceDefinition.HTTP2_ENABLE_PUSH,
                                                        HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE,
                                                        HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE,
                                                        HttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS,
                                                        HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE,
                                                        HttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE,
                                                        HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11)
                                ).addChild(
                                        listenerBuilder(HttpsListenerResourceDefinition.INSTANCE)
                                                // xsd https-listener-type
                                                .setMarshallDefaultValues(true)
                                                .addAttributes(
                                                        HttpsListenerResourceDefinition.SSL_CONTEXT,
                                                        HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING,
                                                        HttpListenerResourceDefinition.CERTIFICATE_FORWARDING,
                                                        HttpsListenerResourceDefinition.SECURITY_REALM,
                                                        HttpsListenerResourceDefinition.VERIFY_CLIENT,
                                                        HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES,
                                                        HttpsListenerResourceDefinition.ENABLED_PROTOCOLS,
                                                        HttpsListenerResourceDefinition.ENABLE_HTTP2,
                                                        HttpsListenerResourceDefinition.ENABLE_SPDY,
                                                        HttpsListenerResourceDefinition.SSL_SESSION_CACHE_SIZE,
                                                        HttpsListenerResourceDefinition.SSL_SESSION_TIMEOUT,
                                                        HttpListenerResourceDefinition.HTTP2_ENABLE_PUSH,
                                                        HttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE,
                                                        HttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE,
                                                        HttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS,
                                                        HttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE,
                                                        HttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE,
                                                        HttpListenerResourceDefinition.REQUIRE_HOST_HTTP11)
                                ).addChild(
                                        builder(HostDefinition.INSTANCE.getPathElement())
                                                .addAttributes(HostDefinition.ALIAS, HostDefinition.DEFAULT_WEB_MODULE, HostDefinition.DEFAULT_RESPONSE_CODE, HostDefinition.DISABLE_CONSOLE_REDIRECT)
                                                .addChild(
                                                        builder(LocationDefinition.INSTANCE.getPathElement())
                                                                .addAttributes(LocationDefinition.HANDLER)
                                                                .addChild(filterRefBuilder())
                                                ).addChild(
                                                builder(AccessLogDefinition.INSTANCE.getPathElement())
                                                        .addAttributes(
                                                                AccessLogDefinition.PATTERN,
                                                                AccessLogDefinition.WORKER,
                                                                AccessLogDefinition.DIRECTORY,
                                                                AccessLogDefinition.RELATIVE_TO,
                                                                AccessLogDefinition.PREFIX,
                                                                AccessLogDefinition.SUFFIX,
                                                                AccessLogDefinition.ROTATE,
                                                                AccessLogDefinition.USE_SERVER_LOG,
                                                                AccessLogDefinition.EXTENDED,
                                                                AccessLogDefinition.PREDICATE)
                                        ).addChild(filterRefBuilder())
                                                .addChild(
                                                    builder(UndertowExtension.PATH_SSO)
                                                        .addAttribute(SingleSignOnDefinition.Attribute.DOMAIN.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.PATH.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.HTTP_ONLY.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.SECURE.getDefinition())
                                                        .addAttribute(SingleSignOnDefinition.Attribute.COOKIE_NAME.getDefinition())
                                        ).addChild(builder(HttpInvokerDefinition.INSTANCE.getPathElement())
                                            .addAttributes(HttpInvokerDefinition.PATH, HttpInvokerDefinition.HTTP_AUTHENTICATION_FACTORY, HttpInvokerDefinition.SECURITY_REALM))
                                        )
                )
                .addChild(
                        builder(ServletContainerDefinition.INSTANCE.getPathElement())
                                .addAttribute(ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS)
                                .addAttribute(ServletContainerDefinition.DEFAULT_BUFFER_CACHE)
                                .addAttribute(ServletContainerDefinition.STACK_TRACE_ON_ERROR)
                                .addAttribute(ServletContainerDefinition.DEFAULT_ENCODING)
                                .addAttribute(ServletContainerDefinition.USE_LISTENER_ENCODING)
                                .addAttribute(ServletContainerDefinition.IGNORE_FLUSH)
                                .addAttribute(ServletContainerDefinition.EAGER_FILTER_INIT)
                                .addAttribute(ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT)
                                .addAttribute(ServletContainerDefinition.DISABLE_CACHING_FOR_SECURED_PAGES)
                                .addAttribute(ServletContainerDefinition.DIRECTORY_LISTING)
                                .addAttribute(ServletContainerDefinition.PROACTIVE_AUTHENTICATION)
                                .addAttribute(ServletContainerDefinition.SESSION_ID_LENGTH)
                                .addAttribute(ServletContainerDefinition.MAX_SESSIONS)
                                .addAttribute(ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE)
                                .addAttribute(ServletContainerDefinition.DISABLE_SESSION_ID_REUSE)
                                .addAttribute(ServletContainerDefinition.FILE_CACHE_METADATA_SIZE)
                                .addAttribute(ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE)
                                .addAttribute(ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)
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
                                                        JspDefinition.DISPLAY_SOURCE_FRAGMENT,
                                                        JspDefinition.OPTIMIZE_SCRIPTLETS)
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
                                                .setMarshallDefaultValues(true)
                                                .addAttributes(
                                                        WebsocketsDefinition.WORKER,
                                                        WebsocketsDefinition.BUFFER_POOL,
                                                        WebsocketsDefinition.DISPATCH_TO_WORKER,
                                                        WebsocketsDefinition.PER_MESSAGE_DEFLATE,
                                                        WebsocketsDefinition.DEFLATER_LEVEL
                                                )
                                )
                                .addChild(builder(MimeMappingDefinition.INSTANCE.getPathElement())
                                        .setXmlWrapperElement("mime-mappings")
                                        .addAttributes(
                                                MimeMappingDefinition.VALUE
                                        ))
                                .addChild(builder(WelcomeFileDefinition.INSTANCE.getPathElement()).setXmlWrapperElement("welcome-files"))
                                .addChild(builder(CrawlerSessionManagementDefinition.INSTANCE.getPathElement())
                                        .addAttributes(CrawlerSessionManagementDefinition.USER_AGENTS, CrawlerSessionManagementDefinition.SESSION_TIMEOUT))
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
                                                        FileHandler.DIRECTORY_LISTING,
                                                        FileHandler.FOLLOW_SYMLINK,
                                                        FileHandler.SAFE_SYMLINK_PATHS,
                                                        FileHandler.CASE_SENSITIVE
                                                )
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
                                                        ReverseProxyHandler.CONNECTION_IDLE_TIMEOUT,
                                                        ReverseProxyHandler.MAX_RETRIES)
                                                .addChild(builder(ReverseProxyHandlerHost.INSTANCE.getPathElement())
                                                        .setXmlElementName(Constants.HOST)
                                                        .addAttributes(
                                                                ReverseProxyHandlerHost.OUTBOUND_SOCKET_BINDING,
                                                                ReverseProxyHandlerHost.SCHEME,
                                                                ReverseProxyHandlerHost.PATH,
                                                                ReverseProxyHandlerHost.INSTANCE_ID,
                                                                ReverseProxyHandlerHost.SSL_CONTEXT,
                                                                ReverseProxyHandlerHost.SECURITY_REALM,
                                                                ReverseProxyHandlerHost.ENABLE_HTTP2))
                                )


                )
                .addChild(
                        builder(FilterDefinitions.INSTANCE.getPathElement())
                                .setXmlElementName(Constants.FILTERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(RequestLimitHandler.INSTANCE.getPathElement())
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
                                builder(ModClusterDefinition.INSTANCE.getPathElement())
                                .addAttributes(ModClusterDefinition.MANAGEMENT_SOCKET_BINDING,
                                        ModClusterDefinition.ADVERTISE_SOCKET_BINDING,
                                        ModClusterDefinition.SECURITY_KEY,
                                        ModClusterDefinition.ADVERTISE_PROTOCOL,
                                        ModClusterDefinition.ADVERTISE_PATH,
                                        ModClusterDefinition.ADVERTISE_FREQUENCY,
                                        ModClusterDefinition.FAILOVER_STRATEGY,
                                        ModClusterDefinition.HEALTH_CHECK_INTERVAL,
                                        ModClusterDefinition.BROKEN_NODE_TIMEOUT,
                                        ModClusterDefinition.WORKER,
                                        ModClusterDefinition.MAX_REQUEST_TIME,
                                        ModClusterDefinition.MANAGEMENT_ACCESS_PREDICATE,
                                        ModClusterDefinition.CONNECTIONS_PER_THREAD,
                                        ModClusterDefinition.CACHED_CONNECTIONS_PER_THREAD,
                                        ModClusterDefinition.CONNECTION_IDLE_TIMEOUT,
                                        ModClusterDefinition.REQUEST_QUEUE_SIZE,
                                        ModClusterDefinition.SSL_CONTEXT,
                                        ModClusterDefinition.SECURITY_REALM,
                                        ModClusterDefinition.USE_ALIAS,
                                        ModClusterDefinition.ENABLE_HTTP2,
                                        ModClusterDefinition.MAX_AJP_PACKET_SIZE,
                                        ModClusterDefinition.HTTP2_ENABLE_PUSH,
                                        ModClusterDefinition.HTTP2_HEADER_TABLE_SIZE,
                                        ModClusterDefinition.HTTP2_INITIAL_WINDOW_SIZE,
                                        ModClusterDefinition.HTTP2_MAX_CONCURRENT_STREAMS,
                                        ModClusterDefinition.HTTP2_MAX_FRAME_SIZE,
                                        ModClusterDefinition.HTTP2_MAX_HEADER_LIST_SIZE,
                                        ModClusterDefinition.MAX_RETRIES)
                        ).addChild(
                                builder(CustomFilterDefinition.INSTANCE.getPathElement())
                                        .addAttributes(CustomFilterDefinition.CLASS_NAME, CustomFilterDefinition.MODULE, CustomFilterDefinition.PARAMETERS)
                                        .setXmlElementName("filter")
                        ).addChild(
                                builder(ExpressionFilterDefinition.INSTANCE.getPathElement())
                                        .addAttributes(ExpressionFilterDefinition.EXPRESSION, ExpressionFilterDefinition.MODULE)
                        ).addChild(
                                builder(RewriteFilterDefinition.INSTANCE.getPathElement())
                                        .addAttributes(RewriteFilterDefinition.TARGET, RewriteFilterDefinition.REDIRECT)
                        )

                )
                .addChild(
                        builder(ApplicationSecurityDomainDefinition.INSTANCE.getPathElement())
                            .setXmlWrapperElement(Constants.APPLICATION_SECURITY_DOMAINS)
                            .addAttributes(ApplicationSecurityDomainDefinition.HTTP_AUTHENTICATION_FACTORY, ApplicationSecurityDomainDefinition.OVERRIDE_DEPLOYMENT_CONFIG, ApplicationSecurityDomainDefinition.ENABLE_JACC)
                            .addChild(builder(UndertowExtension.PATH_SSO)
                                    .addAttribute(SingleSignOnDefinition.Attribute.DOMAIN.getDefinition())
                                    .addAttribute(SingleSignOnDefinition.Attribute.PATH.getDefinition())
                                    .addAttribute(SingleSignOnDefinition.Attribute.HTTP_ONLY.getDefinition())
                                    .addAttribute(SingleSignOnDefinition.Attribute.SECURE.getDefinition())
                                    .addAttribute(SingleSignOnDefinition.Attribute.COOKIE_NAME.getDefinition())
                                    .addAttribute(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.KEY_STORE.getDefinition())
                                    .addAttribute(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.KEY_ALIAS.getDefinition())
                                    .addAttribute(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.SSL_CONTEXT.getDefinition())
                                    .addAttribute(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.CREDENTIAL.getDefinition(), AttributeParser.OBJECT_PARSER, AttributeMarshaller.ATTRIBUTE_OBJECT)
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

    /** Registers attributes common across listener types */
    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder listenerBuilder(PersistentResourceDefinition resource) {
        return builder(resource.getPathElement())
                // xsd socket-optionsType
                .addAttributes(
                        ListenerResourceDefinition.RECEIVE_BUFFER,
                        ListenerResourceDefinition.SEND_BUFFER,
                        ListenerResourceDefinition.BACKLOG,
                        ListenerResourceDefinition.KEEP_ALIVE,
                        ListenerResourceDefinition.READ_TIMEOUT,
                        ListenerResourceDefinition.WRITE_TIMEOUT,
                        ListenerResourceDefinition.MAX_CONNECTIONS)
                // xsd listener-type
                .addAttributes(
                        ListenerResourceDefinition.SOCKET_BINDING,
                        ListenerResourceDefinition.WORKER,
                        ListenerResourceDefinition.BUFFER_POOL,
                        ListenerResourceDefinition.ENABLED,
                        ListenerResourceDefinition.RESOLVE_PEER_ADDRESS,
                        ListenerResourceDefinition.MAX_ENTITY_SIZE,
                        ListenerResourceDefinition.BUFFER_PIPELINED_DATA,
                        ListenerResourceDefinition.MAX_HEADER_SIZE,
                        ListenerResourceDefinition.MAX_PARAMETERS,
                        ListenerResourceDefinition.MAX_HEADERS,
                        ListenerResourceDefinition.MAX_COOKIES,
                        ListenerResourceDefinition.ALLOW_ENCODED_SLASH,
                        ListenerResourceDefinition.DECODE_URL,
                        ListenerResourceDefinition.URL_CHARSET,
                        ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE,
                        ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE,
                        ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                        ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE,
                        ListenerResourceDefinition.NO_REQUEST_TIMEOUT,
                        ListenerResourceDefinition.REQUEST_PARSE_TIMEOUT,
                        ListenerResourceDefinition.DISALLOWED_METHODS,
                        ListenerResourceDefinition.SECURE,
                        ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION);
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder filterRefBuilder() {
        return builder(FilterRefDefinition.INSTANCE.getPathElement())
                .addAttributes(FilterRefDefinition.PREDICATE, FilterRefDefinition.PRIORITY);
    }
}
