/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.as.version.Stability;
import org.jboss.staxmapper.IntVersion;
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
public enum UndertowSubsystemSchema implements PersistentSubsystemSchema<UndertowSubsystemSchema> {
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
    VERSION_14_0(14),   // WildFly 28-present
    VERSION_14_0_PREVIEW(14, 0, Stability.PREVIEW),   // WildFly 33-35
    VERSION_14_0_COMMUNITY(14, 0, Stability.COMMUNITY),   // WildFly 36-present
    VERSION_15_0(15)    // WildFly 40-present
    ;

    static final Set<UndertowSubsystemSchema> CURRENT = EnumSet.of(VERSION_15_0, VERSION_14_0_COMMUNITY);
    private final VersionedNamespace<IntVersion, UndertowSubsystemSchema> namespace;
    private final PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);

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
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(UndertowRootDefinition.PATH_ELEMENT);

        if (this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            builder.addChild(this.factory.builder(ByteBufferPoolDefinition.PATH_ELEMENT).addAttributes(ByteBufferPoolDefinition.ATTRIBUTES.stream()).build());
        }
        builder.addChild(this.factory.builder(BufferCacheDefinition.PATH_ELEMENT).addAttributes(BufferCacheDefinition.ATTRIBUTES.stream()).build());
        builder.addChild(this.factory.builder(ServerDefinition.PATH_ELEMENT).addAttributes(ServerDefinition.ATTRIBUTES.stream())
            .addChild(this.ajpListener())
            .addChild(this.httpListener())
            .addChild(this.httpsListener())
            .addChild(this.host())
            .build()
        );
        builder.addChild(this.servletContainer());
        builder.addChild(this.handlers());
        builder.addChild(this.factory.builder(FilterDefinitions.PATH_ELEMENT).setXmlElementName(Constants.FILTERS).setNoAddOperation(true)
            .addChild(this.factory.builder(RequestLimitHandlerDefinition.PATH_ELEMENT).addAttributes(RequestLimitHandlerDefinition.ATTRIBUTES.stream()).build())
            .addChild(this.factory.builder(ResponseHeaderFilterDefinition.PATH_ELEMENT).addAttributes(ResponseHeaderFilterDefinition.ATTRIBUTES.stream()).build())
            .addChild(this.factory.builder(GzipFilterDefinition.PATH_ELEMENT).build())
            .addChild(this.factory.builder(ErrorPageDefinition.PATH_ELEMENT).addAttributes(ErrorPageDefinition.ATTRIBUTES.stream()).build())
            .addChild(this.modCluster())
            .addChild(this.factory.builder(CustomFilterDefinition.PATH_ELEMENT).addAttributes(CustomFilterDefinition.ATTRIBUTES.stream()).setXmlElementName("filter").build())
            .addChild(this.factory.builder(ExpressionFilterDefinition.PATH_ELEMENT).addAttributes(ExpressionFilterDefinition.ATTRIBUTES.stream()).build())
            .addChild(this.factory.builder(RewriteFilterDefinition.PATH_ELEMENT).addAttributes(RewriteFilterDefinition.ATTRIBUTES.stream()).build())
            .build()
        );
        if (this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addChild(this.applicationSecurityDomain());
        }
        //here to make sure we always add filters & handlers path to mgmt model
        builder.setAdditionalOperationsGenerator((address, addOperation, operations) -> {
            operations.add(Util.createAddOperation(address.append(FilterDefinitions.PATH_ELEMENT)));
            operations.add(Util.createAddOperation(address.append(HandlerDefinitions.PATH_ELEMENT)));
        });

        Stream<AttributeDefinition> attributes = UndertowRootDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_12_0)) {
            attributes = attributes.filter(Predicate.isEqual(UndertowRootDefinition.OBFUSCATE_SESSION_ROUTE).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription ajpListener() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(AjpListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = AjpListenerResourceDefinition.ATTRIBUTES.stream();
        if (!this.since(VERSION_14_0_COMMUNITY)) {
            attributes = attributes.filter(Predicate.isEqual(AjpListenerResourceDefinition.ALLOWED_REQUEST_ATTRIBUTES_PATTERN).negate());
        }
        Stream.concat(this.listenerAttributes(), attributes).forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription httpListener() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(HttpListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = HttpListenerResourceDefinition.ATTRIBUTES.stream();
        // Reproduce attribute order of the previous parser implementation
        Stream.of(this.listenerAttributes(), attributes, this.httpListenerAttributes()).flatMap(Function.identity()).forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription httpsListener() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(HttpsListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = HttpsListenerResourceDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.isEqual(HttpsListenerResourceDefinition.SSL_CONTEXT).negate());
        }
        Stream<AttributeDefinition> httpListenerAttributes = this.httpListenerAttributes();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            httpListenerAttributes = httpListenerAttributes.filter(Predicate.not(Set.of(AbstractHttpListenerResourceDefinition.CERTIFICATE_FORWARDING, AbstractHttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING)::contains));
        }
        // Reproduce attribute order of the previous parser implementation
        Stream.of(this.listenerAttributes(), attributes, httpListenerAttributes).flatMap(Function.identity()).forEach(builder::addAttribute);
        return builder.build();
    }

    private Stream<AttributeDefinition> httpListenerAttributes() {
        Stream<AttributeDefinition> attributes = AbstractHttpListenerResourceDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.isEqual(AbstractHttpListenerResourceDefinition.REQUIRE_HOST_HTTP11).negate());
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL).negate());
        }
        return attributes;
    }

    private Stream<AttributeDefinition> listenerAttributes() {
        Stream<AttributeDefinition> attributes = ListenerResourceDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.isEqual(ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION).negate());
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(ListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL).negate());
        }
        return attributes;
    }

    private PersistentResourceXMLDescription host() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(HostDefinition.PATH_ELEMENT);

        builder.addChild(this.factory.builder(LocationDefinition.PATH_ELEMENT).addAttributes(LocationDefinition.ATTRIBUTES.stream())
            .addChild(this.filterRef())
            .build()
        );
        builder.addChild(this.factory.builder(AccessLogDefinition.PATH_ELEMENT).addAttributes(AccessLogDefinition.ATTRIBUTES.stream()).build());
        if (this.since(UndertowSubsystemSchema.VERSION_9_0)) {
            builder.addChild(this.factory.builder(ConsoleAccessLogDefinition.PATH_ELEMENT).addAttributes(ConsoleAccessLogDefinition.ATTRIBUTES.stream()).build());
        }
        builder.addChild(this.filterRef());
        builder.addChild(this.factory.builder(SingleSignOnDefinition.PATH_ELEMENT).addAttributes(EnumSet.allOf(SingleSignOnDefinition.Attribute.class).stream().map(Supplier::get)).build());
        if (this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addChild(this.factory.builder(HttpInvokerDefinition.PATH_ELEMENT).addAttributes(HttpInvokerDefinition.ATTRIBUTES.stream()).build());
        }

        Stream<AttributeDefinition> attributes = HostDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(HostDefinition.QUEUE_REQUESTS_ON_START).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription filterRef() {
        return this.factory.builder(FilterRefDefinition.PATH_ELEMENT).addAttributes(FilterRefDefinition.ATTRIBUTES.stream()).build();
    }

    private PersistentResourceXMLDescription servletContainer() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(ServletContainerDefinition.PATH_ELEMENT);

        builder.addChild(this.factory.builder(JspDefinition.PATH_ELEMENT).addAttributes(JspDefinition.ATTRIBUTES.stream()).setXmlElementName(Constants.JSP_CONFIG).build());
        if (this.since(UndertowSubsystemSchema.VERSION_14_0)) {
            builder.addChild(this.factory.builder(AffinityCookieDefinition.PATH_ELEMENT).addAttributes(AffinityCookieDefinition.ATTRIBUTES.stream()).build());
        }
        builder.addChild(this.factory.builder(SessionCookieDefinition.PATH_ELEMENT).addAttributes(SessionCookieDefinition.ATTRIBUTES.stream()).build());
        builder.addChild(this.factory.builder(PersistentSessionsDefinition.PATH_ELEMENT).addAttributes(PersistentSessionsDefinition.ATTRIBUTES.stream()).build());
        builder.addChild(this.websockets());
        builder.addChild(PersistentResourceXMLDescription.builder(MimeMappingDefinition.PATH_ELEMENT).addAttributes(MimeMappingDefinition.ATTRIBUTES.stream()).setXmlWrapperElement("mime-mappings").build());
        builder.addChild(PersistentResourceXMLDescription.builder(WelcomeFileDefinition.PATH_ELEMENT).setXmlWrapperElement("welcome-files").build());
        builder.addChild(this.factory.builder(CrawlerSessionManagementDefinition.PATH_ELEMENT).addAttributes(CrawlerSessionManagementDefinition.ATTRIBUTES.stream()).build());

        Stream<AttributeDefinition> attributes = ServletContainerDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE, ServletContainerDefinition.DISABLE_SESSION_ID_REUSE)::contains));
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_5_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE, ServletContainerDefinition.FILE_CACHE_METADATA_SIZE, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)::contains));
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(ServletContainerDefinition.DEFAULT_COOKIE_VERSION).negate());
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_10_0)) {
            attributes = attributes.filter(Predicate.isEqual(ServletContainerDefinition.PRESERVE_PATH_ON_FORWARD).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription websockets() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(WebsocketsDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = WebsocketsDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(WebsocketsDefinition.PER_MESSAGE_DEFLATE, WebsocketsDefinition.DEFLATER_LEVEL)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription handlers() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(HandlerDefinitions.PATH_ELEMENT).setXmlElementName(Constants.HANDLERS).setNoAddOperation(true);

        builder.addChild(this.factory.builder(FileHandlerDefinition.PATH_ELEMENT).addAttributes(FileHandlerDefinition.ATTRIBUTES.stream()).build());

        Stream<AttributeDefinition> reverseProxyHandlerAttributes = ReverseProxyHandlerDefinition.ATTRIBUTES.stream();
        if (!this.since(VERSION_15_0) && !this.since(VERSION_14_0_COMMUNITY)) {
            reverseProxyHandlerAttributes = reverseProxyHandlerAttributes.filter(Predicate.not(Set.of(ReverseProxyHandlerDefinition.REUSE_X_FORWARDED_HEADER, ReverseProxyHandlerDefinition.REWRITE_HOST_HEADER)::contains));
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            reverseProxyHandlerAttributes = reverseProxyHandlerAttributes.filter(Predicate.isEqual(ReverseProxyHandlerDefinition.MAX_RETRIES).negate());
        }
        Stream<AttributeDefinition> reverseProxyHandlerHostAttributes = ReverseProxyHandlerHostDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            reverseProxyHandlerHostAttributes = reverseProxyHandlerHostAttributes.filter(Predicate.not(Set.of(ReverseProxyHandlerHostDefinition.SSL_CONTEXT, ReverseProxyHandlerHostDefinition.ENABLE_HTTP2)::contains));
        }
        builder.addChild(this.factory.builder(ReverseProxyHandlerDefinition.PATH_ELEMENT).addAttributes(reverseProxyHandlerAttributes)
            .addChild(this.factory.builder(ReverseProxyHandlerHostDefinition.PATH_ELEMENT).addAttributes(ReverseProxyHandlerHostDefinition.ATTRIBUTES.stream()).setXmlElementName(Constants.HOST).build())
            .build()
        );
        return builder.build();
    }

    private PersistentResourceXMLDescription modCluster() {
        PersistentResourceXMLDescription.Builder builder = this.factory.builder(ModClusterDefinition.PATH_ELEMENT);

        if (this.since(UndertowSubsystemSchema.VERSION_10_0)) {
            builder.addChild(this.factory.builder(NoAffinityResourceDefinition.PATH).setXmlElementName(Constants.NO_AFFINITY).build());
            builder.addChild(this.factory.builder(SingleAffinityResourceDefinition.PATH).setXmlElementName(Constants.SINGLE_AFFINITY).build());
            builder.addChild(this.factory.builder(RankedAffinityResourceDefinition.PATH).addAttributes(Attribute.stream(RankedAffinityResourceDefinition.Attribute.class)).setXmlElementName(Constants.RANKED_AFFINITY).build());
        }

        Stream<AttributeDefinition> attributes = ModClusterDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ModClusterDefinition.FAILOVER_STRATEGY, ModClusterDefinition.SSL_CONTEXT, ModClusterDefinition.MAX_RETRIES)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private PersistentResourceXMLDescription applicationSecurityDomain() {
        PersistentResourceXMLDescription.Builder builder = PersistentResourceXMLDescription.builder(ApplicationSecurityDomainDefinition.PATH_ELEMENT).setXmlWrapperElement(Constants.APPLICATION_SECURITY_DOMAINS);

        Stream<AttributeDefinition> ssoAttributes = Stream.concat(EnumSet.allOf(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.class).stream(), EnumSet.allOf(SingleSignOnDefinition.Attribute.class).stream()).map(Supplier::get);
        builder.addChild(this.factory.builder(SingleSignOnDefinition.PATH_ELEMENT).addAttributes(ssoAttributes).build());

        Stream<AttributeDefinition> attributes = ApplicationSecurityDomainDefinition.ATTRIBUTES.stream();
        if (!this.since(UndertowSubsystemSchema.VERSION_7_0)) {
            attributes = attributes.filter(Predicate.isEqual(ApplicationSecurityDomainDefinition.SECURITY_DOMAIN).negate());
        }
        if (!this.since(UndertowSubsystemSchema.VERSION_8_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ApplicationSecurityDomainDefinition.ENABLE_JASPI, ApplicationSecurityDomainDefinition.INTEGRATED_JASPI)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }
}
