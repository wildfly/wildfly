/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.undertow.filters.CustomFilterDefinition;
import org.wildfly.extension.undertow.filters.ErrorPageDefinition;
import org.wildfly.extension.undertow.filters.BasicAuthHandler;
import org.wildfly.extension.undertow.filters.ConnectionLimitHandler;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.extension.undertow.filters.GzipFilter;
import org.wildfly.extension.undertow.filters.ResponseHeaderFilter;
import org.wildfly.extension.undertow.handlers.FileHandler;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandler;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandlerHost;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowSubsystemParser_1_2 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final UndertowSubsystemParser_1_2 INSTANCE = new UndertowSubsystemParser_1_2();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(UndertowRootDefinition.INSTANCE)
                .addAttributes(UndertowRootDefinition.DEFAULT_VIRTUAL_HOST, UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER, UndertowRootDefinition.DEFAULT_SERVER, UndertowRootDefinition.INSTANCE_ID)
                .addAttribute(UndertowRootDefinition.STATISTICS_ENABLED)
                .addChild(
                        builder(BufferCacheDefinition.INSTANCE)
                                .addAttributes(BufferCacheDefinition.BUFFER_SIZE, BufferCacheDefinition.BUFFERS_PER_REGION, BufferCacheDefinition.MAX_REGIONS)
                )
                .addChild(builder(ServerDefinition.INSTANCE)
                        .addAttributes(ServerDefinition.DEFAULT_HOST, ServerDefinition.SERVLET_CONTAINER)
                        .addChild(
                                builder(AjpListenerResourceDefinition.INSTANCE)

                                        .addAttributes(AjpListenerResourceDefinition.SCHEME, AjpListenerResourceDefinition.BUFFER_POOL, AjpListenerResourceDefinition.ENABLED, AjpListenerResourceDefinition.SOCKET_BINDING, AjpListenerResourceDefinition.WORKER, ListenerResourceDefinition.REDIRECT_SOCKET)
                                        .addAttribute(ListenerResourceDefinition.RESOLVE_PEER_ADDRESS)
                                        .addAttributes(ListenerResourceDefinition.MAX_HEADER_SIZE, ListenerResourceDefinition.MAX_ENTITY_SIZE,
                                                ListenerResourceDefinition.BUFFER_PIPELINED_DATA, ListenerResourceDefinition.MAX_PARAMETERS, ListenerResourceDefinition.MAX_HEADERS, ListenerResourceDefinition.MAX_COOKIES,ListenerResourceDefinition.ALLOW_ENCODED_SLASH, ListenerResourceDefinition.DECODE_URL,
                                                ListenerResourceDefinition.URL_CHARSET, ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE, ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE, ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                                                ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE)
                                        .addAttributes(ListenerResourceDefinition.BACKLOG, ListenerResourceDefinition.RECEIVE_BUFFER, ListenerResourceDefinition.SEND_BUFFER, ListenerResourceDefinition.KEEP_ALIVE, ListenerResourceDefinition.READ_TIMEOUT, ListenerResourceDefinition.WRITE_TIMEOUT)
                        )
                        .addChild(
                                builder(HttpListenerResourceDefinition.INSTANCE)
                                        .addAttributes(HttpListenerResourceDefinition.BUFFER_POOL, HttpListenerResourceDefinition.CERTIFICATE_FORWARDING, HttpListenerResourceDefinition.ENABLED, HttpListenerResourceDefinition.SOCKET_BINDING, HttpListenerResourceDefinition.WORKER, ListenerResourceDefinition.REDIRECT_SOCKET, HttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING)
                                        .addAttribute(ListenerResourceDefinition.RESOLVE_PEER_ADDRESS)
                                        .addAttributes(ListenerResourceDefinition.MAX_HEADER_SIZE, ListenerResourceDefinition.MAX_ENTITY_SIZE,
                                                ListenerResourceDefinition.BUFFER_PIPELINED_DATA, ListenerResourceDefinition.MAX_PARAMETERS, ListenerResourceDefinition.MAX_HEADERS, ListenerResourceDefinition.MAX_COOKIES,ListenerResourceDefinition.ALLOW_ENCODED_SLASH, ListenerResourceDefinition.DECODE_URL,
                                                ListenerResourceDefinition.URL_CHARSET, ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE, ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE, ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                                                ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE)
                                        .addAttributes(ListenerResourceDefinition.BACKLOG, ListenerResourceDefinition.RECEIVE_BUFFER, ListenerResourceDefinition.SEND_BUFFER, ListenerResourceDefinition.KEEP_ALIVE, ListenerResourceDefinition.READ_TIMEOUT, ListenerResourceDefinition.WRITE_TIMEOUT)
                        ).addChild(
                                        builder(HttpsListenerResourceDefinition.INSTANCE)
                                                .addAttributes(AjpListenerResourceDefinition.SOCKET_BINDING, AjpListenerResourceDefinition.WORKER, AjpListenerResourceDefinition.BUFFER_POOL, AjpListenerResourceDefinition.ENABLED)
                                                .addAttribute(ListenerResourceDefinition.RESOLVE_PEER_ADDRESS)
                                                .addAttributes(HttpsListenerResourceDefinition.SECURITY_REALM, HttpsListenerResourceDefinition.VERIFY_CLIENT, HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES, HttpsListenerResourceDefinition.ENABLED_PROTOCOLS)
                                                .addAttributes(ListenerResourceDefinition.MAX_HEADER_SIZE, ListenerResourceDefinition.MAX_ENTITY_SIZE,
                                                        ListenerResourceDefinition.BUFFER_PIPELINED_DATA, ListenerResourceDefinition.MAX_PARAMETERS, ListenerResourceDefinition.MAX_HEADERS, ListenerResourceDefinition.MAX_COOKIES, ListenerResourceDefinition.ALLOW_ENCODED_SLASH, ListenerResourceDefinition.DECODE_URL,
                                                        ListenerResourceDefinition.URL_CHARSET, ListenerResourceDefinition.ALWAYS_SET_KEEP_ALIVE, ListenerResourceDefinition.MAX_BUFFERED_REQUEST_SIZE, ListenerResourceDefinition.RECORD_REQUEST_START_TIME,
                                                        ListenerResourceDefinition.ALLOW_EQUALS_IN_COOKIE_VALUE)
                                                .addAttributes(ListenerResourceDefinition.BACKLOG, ListenerResourceDefinition.RECEIVE_BUFFER, ListenerResourceDefinition.SEND_BUFFER, ListenerResourceDefinition.KEEP_ALIVE, ListenerResourceDefinition.READ_TIMEOUT, ListenerResourceDefinition.WRITE_TIMEOUT)
                                ).addChild(
                                        builder(HostDefinition.INSTANCE)
                                                .addAttributes(HostDefinition.ALIAS, HostDefinition.DEFAULT_WEB_MODULE)
                                                .addChild(
                                                        builder(LocationDefinition.INSTANCE)
                                                                .addAttributes(LocationDefinition.HANDLER)
                                                                .addChild(
                                                                        builder(FilterRefDefinition.INSTANCE)
                                                                                .addAttributes(FilterRefDefinition.PREDICATE, FilterRefDefinition.PRIORITY)
                                                                )
                                                ).addChild(
                                                builder(AccessLogDefinition.INSTANCE)
                                                        .addAttributes(AccessLogDefinition.PATTERN, AccessLogDefinition.DIRECTORY, AccessLogDefinition.PREFIX, AccessLogDefinition.SUFFIX, AccessLogDefinition.WORKER, AccessLogDefinition.ROTATE)
                                        ).addChild(
                                                builder(FilterRefDefinition.INSTANCE)
                                                        .addAttributes(FilterRefDefinition.PREDICATE, FilterRefDefinition.PRIORITY)
                                        ).addChild(
                                                builder(SingleSignOnDefinition.INSTANCE)
                                                        .addAttributes(SingleSignOnDefinition.DOMAIN, SingleSignOnDefinition.PATH, SingleSignOnDefinition.HTTP_ONLY, SingleSignOnDefinition.SECURE, SingleSignOnDefinition.COOKIE_NAME)
                                        )
                                )
                )
                .addChild(
                        builder(ServletContainerDefinition.INSTANCE)
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
                                        builder(JspDefinition.INSTANCE)
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
                                        builder(SessionCookieDefinition.INSTANCE)
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
                                        builder(PersistentSessionsDefinition.INSTANCE)
                                                .addAttributes(
                                                        PersistentSessionsDefinition.PATH,
                                                        PersistentSessionsDefinition.RELATIVE_TO
                                                )
                                )
                                .addChild(
                                        builder(WebsocketsDefinition.INSTANCE)
                                            .addAttributes(
                                                WebsocketsDefinition.WORKER,
                                                WebsocketsDefinition.BUFFER_POOL,
                                                WebsocketsDefinition.DISPATCH_TO_WORKER
                                            )
                                )
                )
                .addChild(
                        builder(HandlerDefinitions.INSTANCE)
                                .setXmlElementName(Constants.HANDLERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(FileHandler.INSTANCE)
                                                .addAttributes(
                                                        FileHandler.PATH,
                                                        FileHandler.CACHE_BUFFER_SIZE,
                                                        FileHandler.CACHE_BUFFERS,
                                                        FileHandler.DIRECTORY_LISTING)
                                )
                                .addChild(
                                        builder(ReverseProxyHandler.INSTANCE)
                                                .addAttributes(
                                                        ReverseProxyHandler.CONNECTIONS_PER_THREAD,
                                                        ReverseProxyHandler.SESSION_COOKIE_NAMES,
                                                        ReverseProxyHandler.PROBLEM_SERVER_RETRY,
                                                        ReverseProxyHandler.MAX_REQUEST_TIME)
                                                .addChild(builder(ReverseProxyHandlerHost.INSTANCE)
                                                        .setXmlElementName(Constants.HOST)
                                                        .addAttributes(ReverseProxyHandlerHost.INSTANCE_ID, ReverseProxyHandlerHost.PATH, ReverseProxyHandlerHost.SCHEME, ReverseProxyHandlerHost.OUTBOUND_SOCKET_BINDING))
                                )


                )
                .addChild(
                        builder(FilterDefinitions.INSTANCE)
                                .setXmlElementName(Constants.FILTERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(BasicAuthHandler.INSTANCE)
                                                .addAttributes(BasicAuthHandler.SECURITY_DOMAIN)
                                )
                                .addChild(
                                        builder(ConnectionLimitHandler.INSTANCE)
                                                .addAttributes(ConnectionLimitHandler.MAX_CONCURRENT_REQUESTS, ConnectionLimitHandler.QUEUE_SIZE)
                                ).addChild(
                                builder(ResponseHeaderFilter.INSTANCE)
                                        .addAttributes(ResponseHeaderFilter.NAME, ResponseHeaderFilter.VALUE)
                        ).addChild(
                                builder(GzipFilter.INSTANCE)
                        ).addChild(
                                builder(ErrorPageDefinition.INSTANCE)
                                        .addAttributes(ErrorPageDefinition.CODE, ErrorPageDefinition.PATH)
                        ).addChild(
                                builder(CustomFilterDefinition.INSTANCE)
                                        .addAttributes(CustomFilterDefinition.CLASS_NAME, CustomFilterDefinition.MODULE, CustomFilterDefinition.PARAMETERS)
                                        .setXmlElementName("filter")
                        )

                )
                 //here to make sure we always add filters & handlers path to mgmt model
                .setAdditionalOperationsGenerator(new PersistentResourceXMLDescription.AdditionalOperationsGenerator() {
                    @Override
                    public void additionalOperations(final PathAddress address, final ModelNode addOperation, final List<ModelNode> operations) {
                        operations.add(Util.createAddOperation(address.append(UndertowExtension.PATH_FILTERS)));
                        operations.add(Util.createAddOperation(address.append(UndertowExtension.PATH_HANDLERS)));
                    }
                })
                .build();
    }

    private UndertowSubsystemParser_1_2() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(UndertowRootDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());//this is bit of workaround for SPRD to work properly
        xmlDescription.persist(writer, model, Namespace.CURRENT.getUriString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}

