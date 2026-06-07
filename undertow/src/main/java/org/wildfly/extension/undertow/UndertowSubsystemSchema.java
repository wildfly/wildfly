/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.xml.NamedResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.ResourceXMLAll;
import org.jboss.as.controller.persistence.xml.ResourceXMLChoice;
import org.jboss.as.controller.persistence.xml.ResourceXMLElementLocalName;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.ResourceXMLSequence;
import org.jboss.as.controller.persistence.xml.SingletonResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.controller.xml.XMLCardinality;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;
import org.wildfly.extension.undertow.filters.AffinityResourceDefinition;
import org.wildfly.extension.undertow.filters.CustomFilterDefinition;
import org.wildfly.extension.undertow.filters.ErrorPageDefinition;
import org.wildfly.extension.undertow.filters.ExpressionFilterDefinition;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.extension.undertow.filters.GzipFilterDefinition;
import org.wildfly.extension.undertow.filters.ModClusterDefinition;
import org.wildfly.extension.undertow.filters.NoAffinityResourceDefinition;
import org.wildfly.extension.undertow.filters.RankedAffinityResourceDefinition;
import org.wildfly.extension.undertow.filters.RequestLimitHandlerDefinition;
import org.wildfly.extension.undertow.filters.ResponseHeaderFilterDefinition;
import org.wildfly.extension.undertow.filters.RewriteFilterDefinition;
import org.wildfly.extension.undertow.filters.SingleAffinityResourceDefinition;
import org.wildfly.extension.undertow.handlers.FileHandlerDefinition;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandlerDefinition;
import org.wildfly.extension.undertow.handlers.ReverseProxyHandlerHostDefinition;

/**
 * Enumerates the supported Undertow subsystem schemas.
 * @author Paul Ferraro
 */
public enum UndertowSubsystemSchema implements SubsystemResourceXMLSchema<UndertowSubsystemSchema> {
/*  Unsupported, for documentation purposes only
    VERSION_1_0(1, 0),  // WildFly 8.0
    VERSION_1_1(1, 1),  // WildFly 8.1
    VERSION_1_2(1, 2),  // WildFly 8.2
    VERSION_2_0(2),     // WildFly 9
    VERSION_3_0(3, 0),  // WildFly 10.0
 */
    VERSION_3_1(3, 1),  // WildFly 10.1
    VERSION_4_0(4),     // WildFly 11
    VERSION_5_0(5),     // WildFly 12
    VERSION_6_0(6),     // WildFly 13
    VERSION_7_0(7),     // WildFly 14
    VERSION_8_0(8),     // WildFly 15-16
    VERSION_9_0(9),     // WildFly 17
    VERSION_10_0(10),   // WildFly 18-19
    VERSION_11_0(11),   // WildFly 20-22    N.B. There were no parser changes between 10.0 and 11.0 !!
    VERSION_12_0(12),   // WildFly 23-26.1, EAP 7.4
    VERSION_13_0(13),   // WildFly 27       N.B. There were no schema changes between 12.0 and 13.0!
    VERSION_14_0(14),   // WildFly 28-39
    VERSION_14_0_PREVIEW(14, 0, Stability.PREVIEW),   // WildFly 33-35
    VERSION_14_0_COMMUNITY(14, 0, Stability.COMMUNITY),   // WildFly 36-40
    VERSION_15_0(15),    // WildFly 40
    VERSION_16_0(16),    // WildFly 41-present
    ;

    static final Set<UndertowSubsystemSchema> CURRENT = EnumSet.of(VERSION_16_0);

    private final VersionedNamespace<IntVersion, UndertowSubsystemSchema> namespace;
    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);

    UndertowSubsystemSchema(int major) {
        this(new IntVersion(major));
    }

    UndertowSubsystemSchema(int major, int minor) {
        this(new IntVersion(major, minor));
    }

    UndertowSubsystemSchema(IntVersion version) {
        this(version, Stability.DEFAULT);
    }

    UndertowSubsystemSchema(final int major, final int minor, final Stability stability) {
        this(new IntVersion(major, minor), stability);
    }

    UndertowSubsystemSchema(final IntVersion version, final Stability stability) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(UndertowExtension.SUBSYSTEM_NAME, stability, version);
    }

    @Override
    public VersionedNamespace<IntVersion, UndertowSubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        SubsystemResourceRegistrationXMLElement.Builder builder = this.factory.subsystemElement(UndertowRootDefinition.REGISTRATION)
                .addAttributes(List.of(
                        UndertowRootDefinition.DEFAULT_VIRTUAL_HOST,
                        UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER,
                        UndertowRootDefinition.DEFAULT_SERVER,
                        UndertowRootDefinition.INSTANCE_ID,
                        UndertowRootDefinition.STATISTICS_ENABLED,
                        UndertowRootDefinition.DEFAULT_SECURITY_DOMAIN));
        if (this.since(VERSION_12_0)) {
            builder.addAttribute(UndertowRootDefinition.OBFUSCATE_SESSION_ROUTE);
        }

        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence();
        if (this.since(VERSION_6_0)) {
            contentBuilder.addElement(this.getBufferPoolElement());
        }
        contentBuilder.addElement(this.getBufferCacheElement());
        contentBuilder.addElement(this.getServerElement());
        contentBuilder.addElement(this.getServletContainerElement());
        contentBuilder.addElement(this.getHandlersElement());
        contentBuilder.addElement(this.getFiltersElement());
        if (this.since(VERSION_4_0)) {
            contentBuilder.addElement(this.factory.element(this.factory.resolve(Constants.APPLICATION_SECURITY_DOMAINS))
                    .withCardinality(XMLCardinality.Single.OPTIONAL)
                    .withContent(this.factory.sequence().addElement(this.getApplicationSecurityDomainElement()).build())
                    .build());
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private ResourceRegistrationXMLElement getBufferPoolElement() {
        return this.factory.namedElement(ByteBufferPoolDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ByteBufferPoolDefinition.BUFFER_SIZE,
                        ByteBufferPoolDefinition.MAX_POOL_SIZE,
                        ByteBufferPoolDefinition.DIRECT,
                        ByteBufferPoolDefinition.THREAD_LOCAL_CACHE_SIZE,
                        ByteBufferPoolDefinition.LEAK_DETECTION_PERCENT))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getBufferCacheElement() {
        return this.factory.namedElement(BufferCacheDefinition.REGISTRATION)
                .addAttributes(List.of(
                        BufferCacheDefinition.BUFFER_SIZE,
                        BufferCacheDefinition.BUFFERS_PER_REGION,
                        BufferCacheDefinition.MAX_REGIONS))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getServerElement() {
        return this.factory.namedElement(ServerDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ServerDefinition.DEFAULT_HOST,
                        ServerDefinition.SERVLET_CONTAINER))
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .withContent(this.factory.sequence()
                        .addElement(this.getAjpListenerElement())
                        .addElement(this.getHttpListenerElement())
                        .addElement(this.getHttpsListenerElement())
                        .addElement(this.getHostElement())
                        .build())
                .build();
    }

    private ResourceRegistrationXMLElement getAjpListenerElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.listenerElementBuilder(AjpListenerResourceDefinition.REGISTRATION);
        // Reproduce attribute order of the previous parser implementation
        builder.addAttributes(List.of(
                AjpListenerResourceDefinition.SCHEME,
                ListenerResourceDefinition.REDIRECT_SOCKET,
                AjpListenerResourceDefinition.MAX_AJP_PACKET_SIZE));
        if (this.since(VERSION_15_0) || this.since(VERSION_14_0_COMMUNITY)) {
            builder.addAttribute(AjpListenerResourceDefinition.ALLOWED_REQUEST_ATTRIBUTES_PATTERN);
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement getHttpListenerElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.listenerElementBuilder(HttpListenerResourceDefinition.REGISTRATION);
        // Reproduce attribute order of the previous parser implementation
        builder.addAttributes(List.of(
                ListenerResourceDefinition.REDIRECT_SOCKET,
                AbstractHttpListenerResourceDefinition.ENABLE_HTTP2,
                AbstractHttpListenerResourceDefinition.HTTP2_ENABLE_PUSH,
                AbstractHttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE,
                AbstractHttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE,
                AbstractHttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS,
                AbstractHttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE,
                AbstractHttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE,
                AbstractHttpListenerResourceDefinition.CERTIFICATE_FORWARDING,
                AbstractHttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING));
        if (this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addAttribute(AbstractHttpListenerResourceDefinition.REQUIRE_HOST_HTTP11);
        }
        if (this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            builder.addAttribute(AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL);
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement getHttpsListenerElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.listenerElementBuilder(HttpsListenerResourceDefinition.REGISTRATION);
        // Reproduce attribute order of the previous parser implementation
        if (this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addAttribute(HttpsListenerResourceDefinition.SSL_CONTEXT);
        }
        builder.addAttributes(List.of(
                HttpsListenerResourceDefinition.SECURITY_REALM,
                HttpsListenerResourceDefinition.VERIFY_CLIENT,
                HttpsListenerResourceDefinition.ENABLED_CIPHER_SUITES,
                HttpsListenerResourceDefinition.ENABLED_PROTOCOLS,
                HttpsListenerResourceDefinition.ENABLE_SPDY,
                HttpsListenerResourceDefinition.SSL_SESSION_CACHE_SIZE,
                HttpsListenerResourceDefinition.SSL_SESSION_TIMEOUT,
                AbstractHttpListenerResourceDefinition.ENABLE_HTTP2,
                AbstractHttpListenerResourceDefinition.HTTP2_ENABLE_PUSH,
                AbstractHttpListenerResourceDefinition.HTTP2_HEADER_TABLE_SIZE,
                AbstractHttpListenerResourceDefinition.HTTP2_INITIAL_WINDOW_SIZE,
                AbstractHttpListenerResourceDefinition.HTTP2_MAX_CONCURRENT_STREAMS,
                AbstractHttpListenerResourceDefinition.HTTP2_MAX_HEADER_LIST_SIZE,
                AbstractHttpListenerResourceDefinition.HTTP2_MAX_FRAME_SIZE));
        if (this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addAttributes(List.of(
                    AbstractHttpListenerResourceDefinition.CERTIFICATE_FORWARDING,
                    AbstractHttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING,
                    AbstractHttpListenerResourceDefinition.REQUIRE_HOST_HTTP11));
        }
        if (this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            builder.addAttribute(AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL);
        }
        return builder.build();
    }

    private NamedResourceRegistrationXMLElement.Builder listenerElementBuilder(ResourceRegistration registration) {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(registration);
        // Reproduce attribute order of the previous parser implementation
        builder.addAttributes(List.of(
                ListenerResourceDefinition.SOCKET_BINDING,
                ListenerResourceDefinition.WORKER,
                ListenerResourceDefinition.BUFFER_POOL,
                ListenerResourceDefinition.ENABLED,
                ListenerResourceDefinition.RESOLVE_PEER_ADDRESS,
                ListenerResourceDefinition.DISALLOWED_METHODS,
                ListenerResourceDefinition.SECURE,
                ListenerResourceDefinition.MAX_HEADER_SIZE,
                ListenerResourceDefinition.MAX_ENTITY_SIZE,
                ListenerResourceDefinition.BUFFER_PIPELINED_DATA,
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
                ListenerResourceDefinition.REQUEST_PARSE_TIMEOUT));
        if (this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addAttribute(ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION);
        }
        if (this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            builder.addAttribute(ListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL);
        }
        builder.addAttributes(List.of(
                ListenerResourceDefinition.BACKLOG,
                ListenerResourceDefinition.RECEIVE_BUFFER,
                ListenerResourceDefinition.SEND_BUFFER,
                ListenerResourceDefinition.KEEP_ALIVE,
                ListenerResourceDefinition.READ_TIMEOUT,
                ListenerResourceDefinition.WRITE_TIMEOUT,
                ListenerResourceDefinition.MAX_CONNECTIONS));
        return builder.withCardinality(XMLCardinality.Unbounded.OPTIONAL);
    }

    private ResourceRegistrationXMLElement getHostElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(HostDefinition.REGISTRATION)
                .addAttributes(List.of(
                        HostDefinition.ALIAS,
                        HostDefinition.DEFAULT_WEB_MODULE,
                        HostDefinition.DEFAULT_RESPONSE_CODE,
                        HostDefinition.DISABLE_CONSOLE_REDIRECT))
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                ;

        if (this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            builder.addAttribute(HostDefinition.QUEUE_REQUESTS_ON_START);
        }

        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence();
        contentBuilder.addElement(this.getLocationElement());
        contentBuilder.addElement(this.getAccessLogElement());
        if (this.since(VERSION_9_0)) {
            contentBuilder.addElement(this.getConsoleAccessLogElement());
        }
        contentBuilder.addElement(this.getFilterRefElement());
        contentBuilder.addElement(this.getSingleSignOnElement());
        if (this.since(VERSION_4_0)) {
            contentBuilder.addElement(this.getHttpInvokerElement());
        }
        return builder.withContent(contentBuilder.build()).build();
    }

    private ResourceRegistrationXMLElement getLocationElement() {
        return this.factory.namedElement(LocationDefinition.REGISTRATION)
                .addAttribute(LocationDefinition.HANDLER)
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .withContent(this.factory.sequence()
                        .addElement(this.getFilterRefElement())
                        .build())
                .build();
    }

    private ResourceRegistrationXMLElement getAccessLogElement() {
        return this.factory.singletonElement(AccessLogDefinition.REGISTRATION)
                .addAttributes(List.of(
                        AccessLogDefinition.WORKER,
                        AccessLogDefinition.PATTERN,
                        AccessLogDefinition.PREFIX,
                        AccessLogDefinition.SUFFIX,
                        AccessLogDefinition.ROTATE,
                        AccessLogDefinition.DIRECTORY,
                        AccessLogDefinition.USE_SERVER_LOG,
                        AccessLogDefinition.RELATIVE_TO,
                        AccessLogDefinition.EXTENDED,
                        AccessLogDefinition.PREDICATE
                ))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getConsoleAccessLogElement() {
        return this.factory.singletonElement(ConsoleAccessLogDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ExchangeAttributeDefinitions.ATTRIBUTES,
                        ConsoleAccessLogDefinition.INCLUDE_HOST_NAME,
                        AccessLogDefinition.WORKER,
                        AccessLogDefinition.PREDICATE,
                        ConsoleAccessLogDefinition.METADATA))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .withContent(this.factory.sequence()
                        .addElement(ExchangeAttributeDefinitions.ATTRIBUTES)
                        .addElement(ConsoleAccessLogDefinition.METADATA)
                        .build())
                .build();
    }

    private ResourceRegistrationXMLElement getFilterRefElement() {
        return this.factory.namedElement(FilterRefDefinition.REGISTRATION)
                .addAttributes(List.of(
                        FilterRefDefinition.PREDICATE,
                        FilterRefDefinition.PRIORITY))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getSingleSignOnElement() {
        return this.factory.singletonElement(SingleSignOnDefinition.REGISTRATION)
                .addAttributes(List.of(
                        SingleSignOnDefinition.Attribute.DOMAIN.get(),
                        SingleSignOnDefinition.Attribute.PATH.get(),
                        SingleSignOnDefinition.Attribute.SECURE.get(),
                        SingleSignOnDefinition.Attribute.HTTP_ONLY.get(),
                        SingleSignOnDefinition.Attribute.COOKIE_NAME.get()))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getHttpInvokerElement() {
        return this.factory.singletonElement(HttpInvokerDefinition.REGISTRATION)
                .addAttributes(List.of(
                        HttpInvokerDefinition.PATH,
                        HttpInvokerDefinition.HTTP_AUTHENTICATION_FACTORY,
                        HttpInvokerDefinition.SECURITY_REALM))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getServletContainerElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(ServletContainerDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS,
                        ServletContainerDefinition.DEFAULT_BUFFER_CACHE,
                        ServletContainerDefinition.STACK_TRACE_ON_ERROR,
                        ServletContainerDefinition.DEFAULT_ENCODING,
                        ServletContainerDefinition.USE_LISTENER_ENCODING,
                        ServletContainerDefinition.IGNORE_FLUSH,
                        ServletContainerDefinition.EAGER_FILTER_INIT,
                        ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT,
                        ServletContainerDefinition.DISABLE_CACHING_FOR_SECURED_PAGES,
                        ServletContainerDefinition.DIRECTORY_LISTING,
                        ServletContainerDefinition.PROACTIVE_AUTHENTICATION,
                        ServletContainerDefinition.SESSION_ID_LENGTH,
                        ServletContainerDefinition.MAX_SESSIONS))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL);

        if (this.since(VERSION_4_0)) {
            builder.addAttributes(List.of(ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE, ServletContainerDefinition.DISABLE_SESSION_ID_REUSE));
        }
        if (this.since(VERSION_5_0)) {
            builder.addAttributes(List.of(ServletContainerDefinition.FILE_CACHE_METADATA_SIZE, ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE));
        }
        if (this.since(VERSION_6_0)) {
            builder.addAttribute(ServletContainerDefinition.DEFAULT_COOKIE_VERSION);
        }
        if (this.since(VERSION_10_0)) {
            builder.addAttribute(ServletContainerDefinition.PRESERVE_PATH_ON_FORWARD);
        }
        if (this.since(VERSION_14_0)) {
            builder.addAttribute(ServletContainerDefinition.ORPHAN_SESSION_ALLOWED);
        }

        ResourceXMLAll.Builder contentBuilder = this.factory.all();
        contentBuilder.addElement(this.getJSPElement());
        if (this.since(VERSION_14_0)) {
            contentBuilder.addElement(this.getAffinityCookieElement());
        }
        contentBuilder.addElement(this.getSessionCookieElement());
        contentBuilder.addElement(this.getPersistentSessionsElement());
        contentBuilder.addElement(this.getWebSocketsElement());
        contentBuilder.addElement(this.factory.element(this.resolve("mime-mappings"))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .withContent(this.factory.sequence()
                        .addElement(this.getMimeMappingElement())
                        .build())
                .build());
        contentBuilder.addElement(this.factory.element(this.resolve("welcome-files"))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .withContent(this.factory.sequence()
                        .addElement(this.getWelcomeFileElement())
                        .build())
                .build());
        contentBuilder.addElement(this.getCrawlerSessionManagementElement());
        return builder.withContent(contentBuilder.build()).build();
    }

    private ResourceRegistrationXMLElement getJSPElement() {
        return this.factory.singletonElement(JspDefinition.REGISTRATION)
                .withElementLocalName(Constants.JSP_CONFIG)
                .addAttributes(List.of(
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
                        JspDefinition.OPTIMIZE_SCRIPTLETS))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getPersistentSessionsElement() {
        return this.factory.singletonElement(PersistentSessionsDefinition.REGISTRATION)
                .addAttributes(List.of(
                        PersistentSessionsDefinition.PATH,
                        PersistentSessionsDefinition.RELATIVE_TO))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getAffinityCookieElement() {
        return this.factory.singletonElement(AffinityCookieDefinition.REGISTRATION)
                .addAttributes(List.of(
                        AbstractCookieDefinition.Attribute.REQUIRED_NAME.getDefinition(),
                        AbstractCookieDefinition.Attribute.DOMAIN.getDefinition(),
                        AbstractCookieDefinition.Attribute.HTTP_ONLY.getDefinition(),
                        AbstractCookieDefinition.Attribute.SECURE.getDefinition(),
                        AbstractCookieDefinition.Attribute.MAX_AGE.getDefinition()))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getSessionCookieElement() {
        return this.factory.singletonElement(SessionCookieDefinition.REGISTRATION)
                .addAttributes(List.of(
                        AbstractCookieDefinition.Attribute.OPTIONAL_NAME.getDefinition(),
                        AbstractCookieDefinition.Attribute.DOMAIN.getDefinition(),
                        AbstractCookieDefinition.Attribute.COMMENT.getDefinition(),
                        AbstractCookieDefinition.Attribute.HTTP_ONLY.getDefinition(),
                        AbstractCookieDefinition.Attribute.SECURE.getDefinition(),
                        AbstractCookieDefinition.Attribute.MAX_AGE.getDefinition()))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getWebSocketsElement() {
        SingletonResourceRegistrationXMLElement.Builder builder = this.factory.singletonElement(WebsocketsDefinition.REGISTRATION)
                .addAttributes(List.of(WebsocketsDefinition.BUFFER_POOL, WebsocketsDefinition.WORKER, WebsocketsDefinition.DISPATCH_TO_WORKER))
                .withCardinality(XMLCardinality.Single.OPTIONAL);

        if (this.since(VERSION_4_0)) {
            builder.addAttributes(List.of(WebsocketsDefinition.PER_MESSAGE_DEFLATE, WebsocketsDefinition.DEFLATER_LEVEL));
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement getMimeMappingElement() {
        return this.factory.namedElement(MimeMappingDefinition.REGISTRATION)
                .addAttribute(MimeMappingDefinition.VALUE)
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .build();
    }

    private ResourceRegistrationXMLElement getWelcomeFileElement() {
        return this.factory.namedElement(WelcomeFileDefinition.REGISTRATION)
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                .build();
    }

    private ResourceRegistrationXMLElement getCrawlerSessionManagementElement() {
        return this.factory.singletonElement(CrawlerSessionManagementDefinition.REGISTRATION)
                .addAttributes(List.of(
                        CrawlerSessionManagementDefinition.USER_AGENTS,
                        CrawlerSessionManagementDefinition.SESSION_TIMEOUT))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getHandlersElement() {
        ResourceXMLSequence.Builder contentBuilder = this.factory.sequence()
                .addElement(this.getFileHandlerElement())
                .addElement(this.getReverseProxyElement())
                ;
        return this.factory.singletonElement(HandlerDefinitions.REGISTRATION)
                .implyIfAbsent()
                .withElementLocalName(Constants.HANDLERS)
                .withContent(contentBuilder.build())
                .build();
    }

    private ResourceRegistrationXMLElement getFileHandlerElement() {
        return this.factory.namedElement(FileHandlerDefinition.REGISTRATION)
                .addAttributes(List.of(
                        FileHandlerDefinition.PATH,
                        FileHandlerDefinition.CACHE_BUFFER_SIZE,
                        FileHandlerDefinition.CACHE_BUFFERS,
                        FileHandlerDefinition.DIRECTORY_LISTING,
                        FileHandlerDefinition.FOLLOW_SYMLINK,
                        FileHandlerDefinition.CASE_SENSITIVE,
                        FileHandlerDefinition.SAFE_SYMLINK_PATHS))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                .build();
    }

    private ResourceRegistrationXMLElement getReverseProxyElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(ReverseProxyHandlerDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ReverseProxyHandlerDefinition.CONNECTIONS_PER_THREAD,
                        ReverseProxyHandlerDefinition.SESSION_COOKIE_NAMES,
                        ReverseProxyHandlerDefinition.PROBLEM_SERVER_RETRY,
                        ReverseProxyHandlerDefinition.REQUEST_QUEUE_SIZE,
                        ReverseProxyHandlerDefinition.MAX_REQUEST_TIME,
                        ReverseProxyHandlerDefinition.CACHED_CONNECTIONS_PER_THREAD,
                        ReverseProxyHandlerDefinition.CONNECTION_IDLE_TIMEOUT))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL);

        if (this.since(VERSION_4_0)) {
            builder.addAttribute(ReverseProxyHandlerDefinition.MAX_RETRIES);
        }
        if (this.since(VERSION_14_0_COMMUNITY) || this.since(VERSION_15_0)) {
            builder.addAttributes(List.of(ReverseProxyHandlerDefinition.REUSE_X_FORWARDED_HEADER, ReverseProxyHandlerDefinition.REWRITE_HOST_HEADER));
        }
        ResourceXMLSequence content = this.factory.sequence()
                .addElement(this.getReverseProxyHostElement())
                .build();
        return builder.withContent(content).build();
    }

    private ResourceRegistrationXMLElement getReverseProxyHostElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(ReverseProxyHandlerHostDefinition.REGISTRATION)
                .addAttributes(List.of(ReverseProxyHandlerHostDefinition.OUTBOUND_SOCKET_BINDING, ReverseProxyHandlerHostDefinition.SCHEME, ReverseProxyHandlerHostDefinition.INSTANCE_ID, ReverseProxyHandlerHostDefinition.PATH, ReverseProxyHandlerHostDefinition.SECURITY_REALM))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL);

        if (this.since(VERSION_4_0)) {
            builder.addAttributes(List.of(ReverseProxyHandlerHostDefinition.SSL_CONTEXT, ReverseProxyHandlerHostDefinition.ENABLE_HTTP2));
        }
        return builder.build();
    }

    private ResourceRegistrationXMLElement getFiltersElement() {
        ResourceXMLChoice.Builder contentBuilder = this.factory.choice()
                .addElement(this.getRequestLimitElement())
                .addElement(this.getResponseHeaderElement())
                .addElement(this.getGzipElement())
                .addElement(this.getErrorPage())
                .addElement(this.getModClusterElement())
                .addElement(this.getCustomFilterElement())
                .addElement(this.getExpressionFilterElement())
                .addElement(this.getRewriteElement())
                .withCardinality(XMLCardinality.Unbounded.REQUIRED)
                ;

        return this.factory.singletonElement(FilterDefinitions.REGISTRATION)
                .implyIfAbsent()
                .withElementLocalName(Constants.FILTERS)
                .withContent(contentBuilder.build())
                .build();
    }

    private ResourceRegistrationXMLElement getRequestLimitElement() {
        return this.factory.namedElement(RequestLimitHandlerDefinition.REGISTRATION)
                .addAttributes(List.of(
                        RequestLimitHandlerDefinition.MAX_CONCURRENT_REQUESTS,
                        RequestLimitHandlerDefinition.QUEUE_SIZE))
                .build();
    }

    private ResourceRegistrationXMLElement getResponseHeaderElement() {
        return this.factory.namedElement(ResponseHeaderFilterDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ResponseHeaderFilterDefinition.NAME,
                        ResponseHeaderFilterDefinition.VALUE))
                .build();
    }

    private ResourceRegistrationXMLElement getGzipElement() {
        return this.factory.namedElement(GzipFilterDefinition.REGISTRATION)
                .build();
    }

    private ResourceRegistrationXMLElement getErrorPage() {
        return this.factory.namedElement(ErrorPageDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ErrorPageDefinition.CODE,
                        ErrorPageDefinition.PATH))
                .build();
    }

    private ResourceRegistrationXMLElement getModClusterElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(ModClusterDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ModClusterDefinition.MANAGEMENT_SOCKET_BINDING,
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
                        ModClusterDefinition.REQUEST_QUEUE_SIZE,
                        ModClusterDefinition.SECURITY_REALM,
                        ModClusterDefinition.USE_ALIAS,
                        ModClusterDefinition.ENABLE_HTTP2,
                        ModClusterDefinition.MAX_AJP_PACKET_SIZE,
                        ModClusterDefinition.HTTP2_MAX_HEADER_LIST_SIZE,
                        ModClusterDefinition.HTTP2_MAX_FRAME_SIZE,
                        ModClusterDefinition.HTTP2_MAX_CONCURRENT_STREAMS,
                        ModClusterDefinition.HTTP2_INITIAL_WINDOW_SIZE,
                        ModClusterDefinition.HTTP2_HEADER_TABLE_SIZE,
                        ModClusterDefinition.HTTP2_ENABLE_PUSH));

        if (this.since(VERSION_4_0)) {
            builder.addAttributes(List.of(ModClusterDefinition.FAILOVER_STRATEGY, ModClusterDefinition.SSL_CONTEXT, ModClusterDefinition.MAX_RETRIES));
        }
        if (this.since(VERSION_10_0)) {
            builder.withContent(this.factory.singletonElementChoice()
                    .addElement(this.affinityElementBuilder(NoAffinityResourceDefinition.REGISTRATION).withElementLocalName("no-affinity").build())
                    .addElement(this.affinityElementBuilder(SingleAffinityResourceDefinition.REGISTRATION).build())
                    .addElement(this.affinityElementBuilder(RankedAffinityResourceDefinition.REGISTRATION).addAttribute(RankedAffinityResourceDefinition.Attribute.DELIMITER.getDefinition()).build())
                    .implyIfEmpty(SingleAffinityResourceDefinition.REGISTRATION, AffinityResourceDefinition.WILDCARD_PATH)
                    .build());
        }
        return builder.build();
    }

    private SingletonResourceRegistrationXMLElement.Builder affinityElementBuilder(ResourceRegistration registration) {
        return this.factory.singletonElement(registration)
                .withElementLocalName(ResourceXMLElementLocalName.VALUE_KEY)
                .withOperationKey(AffinityResourceDefinition.WILDCARD_PATH)
                ;
    }

    private ResourceRegistrationXMLElement getCustomFilterElement() {
        return this.factory.namedElement(CustomFilterDefinition.REGISTRATION)
                .withElementLocalName(Constants.FILTER)
                .addAttributes(List.of(
                        CustomFilterDefinition.CLASS_NAME,
                        CustomFilterDefinition.MODULE))
                .withContent(this.factory.sequence()
                        .addElement(CustomFilterDefinition.PARAMETERS)
                        .build())
                .build();
    }

    private ResourceRegistrationXMLElement getExpressionFilterElement() {
        return this.factory.namedElement(ExpressionFilterDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ExpressionFilterDefinition.EXPRESSION,
                        ExpressionFilterDefinition.MODULE))
                .build();
    }

    private ResourceRegistrationXMLElement getRewriteElement() {
        return this.factory.namedElement(RewriteFilterDefinition.REGISTRATION)
                .addAttributes(List.of(
                        RewriteFilterDefinition.TARGET,
                        RewriteFilterDefinition.REDIRECT))
                .build();
    }

    private ResourceRegistrationXMLElement getApplicationSecurityDomainElement() {
        NamedResourceRegistrationXMLElement.Builder builder = this.factory.namedElement(ApplicationSecurityDomainDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ApplicationSecurityDomainDefinition.HTTP_AUTHENTICATION_FACTORY,
                        ApplicationSecurityDomainDefinition.OVERRIDE_DEPLOYMENT_CONFIG,
                        ApplicationSecurityDomainDefinition.ENABLE_JACC))
                .withCardinality(XMLCardinality.Unbounded.OPTIONAL)
                ;

        if (this.since(VERSION_7_0)) {
            builder.addAttribute(ApplicationSecurityDomainDefinition.SECURITY_DOMAIN);
        }
        if (this.since(VERSION_8_0)) {
            builder.addAttributes(List.of(ApplicationSecurityDomainDefinition.ENABLE_JASPI, ApplicationSecurityDomainDefinition.INTEGRATED_JASPI));
        }

        ResourceXMLSequence content = this.factory.sequence()
                .addElement(this.getApplicationSecurityDomainSingleSignOnElement())
                .build();
        return builder.withContent(content).build();
    }

    private ResourceRegistrationXMLElement getApplicationSecurityDomainSingleSignOnElement() {
        return this.factory.singletonElement(SingleSignOnDefinition.REGISTRATION)
                .addAttributes(List.of(
                        ApplicationSecurityDomainSingleSignOnDefinition.Attribute.KEY_ALIAS.get(),
                        ApplicationSecurityDomainSingleSignOnDefinition.Attribute.KEY_STORE.get(),
                        ApplicationSecurityDomainSingleSignOnDefinition.Attribute.SSL_CONTEXT.get(),
                        SingleSignOnDefinition.Attribute.DOMAIN.get(),
                        SingleSignOnDefinition.Attribute.PATH.get(),
                        SingleSignOnDefinition.Attribute.SECURE.get(),
                        SingleSignOnDefinition.Attribute.HTTP_ONLY.get(),
                        SingleSignOnDefinition.Attribute.COOKIE_NAME.get()))
                .withCardinality(XMLCardinality.Single.OPTIONAL)
                .withContent(this.factory.sequence()
                        .addElement(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.CREDENTIAL.get())
                        .build())
                .build();
    }
}
