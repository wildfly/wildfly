/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.operations.common.Util;
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
 * Factory for creating the {@link org.jboss.as.controller.PersistentResourceXMLDescription} for a given schema.
 * @author Paul Ferraro
 */
public enum UndertowPersistentResourceXMLDescriptionFactory implements Function<UndertowSubsystemSchema, PersistentResourceXMLDescription> {
    INSTANCE;

    @Override
    public PersistentResourceXMLDescription apply(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(UndertowRootDefinition.PATH_ELEMENT, schema.getNamespace());

        if (schema.since(UndertowSubsystemSchema.VERSION_6_0)) {
            builder.addChild(builder(ByteBufferPoolDefinition.PATH_ELEMENT).addAttributes(ByteBufferPoolDefinition.ATTRIBUTES.stream()));
        }
        builder.addChild(builder(BufferCacheDefinition.PATH_ELEMENT).addAttributes(BufferCacheDefinition.ATTRIBUTES.stream()));
        builder.addChild(builder(ServerDefinition.PATH_ELEMENT).addAttributes(ServerDefinition.ATTRIBUTES.stream())
            .addChild(ajpListenerBuilder(schema))
            .addChild(httpListenerBuilder(schema))
            .addChild(httpsListenerBuilder(schema))
            .addChild(hostBuilder(schema))
        );
        builder.addChild(servletContainerBuilder(schema));
        builder.addChild(handlersBuilder(schema));
        builder.addChild(PersistentResourceXMLDescription.builder(FilterDefinitions.PATH_ELEMENT).setXmlElementName(Constants.FILTERS).setNoAddOperation(true)
            .addChild(builder(RequestLimitHandlerDefinition.PATH_ELEMENT).addAttributes(RequestLimitHandlerDefinition.ATTRIBUTES.stream()))
            .addChild(builder(ResponseHeaderFilterDefinition.PATH_ELEMENT).addAttributes(ResponseHeaderFilterDefinition.ATTRIBUTES.stream()))
            .addChild(builder(GzipFilterDefinition.PATH_ELEMENT))
            .addChild(builder(ErrorPageDefinition.PATH_ELEMENT).addAttributes(ErrorPageDefinition.ATTRIBUTES.stream()))
            .addChild(modClusterBuilder(schema))
            .addChild(builder(CustomFilterDefinition.PATH_ELEMENT).addAttributes(CustomFilterDefinition.ATTRIBUTES.stream()).setXmlElementName("filter"))
            .addChild(builder(ExpressionFilterDefinition.PATH_ELEMENT).addAttributes(ExpressionFilterDefinition.ATTRIBUTES.stream()))
            .addChild(builder(RewriteFilterDefinition.PATH_ELEMENT).addAttributes(RewriteFilterDefinition.ATTRIBUTES.stream()))
        );
        if (schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addChild(applicationSecurityDomainBuilder(schema));
        }
        //here to make sure we always add filters & handlers path to mgmt model
        builder.setAdditionalOperationsGenerator((address, addOperation, operations) -> {
            operations.add(Util.createAddOperation(address.append(FilterDefinitions.PATH_ELEMENT)));
            operations.add(Util.createAddOperation(address.append(HandlerDefinitions.PATH_ELEMENT)));
        });

        Stream<AttributeDefinition> attributes = UndertowRootDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_12_0)) {
            attributes = attributes.filter(Predicate.isEqual(UndertowRootDefinition.OBFUSCATE_SESSION_ROUTE).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder ajpListenerBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(AjpListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = AjpListenerResourceDefinition.ATTRIBUTES.stream();
        Stream.concat(listenerAttributes(schema), attributes).forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder httpListenerBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HttpListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = HttpListenerResourceDefinition.ATTRIBUTES.stream();
        // Reproduce attribute order of the previous parser implementation
        Stream.of(listenerAttributes(schema), attributes, httpListenerAttributes(schema)).flatMap(Function.identity()).forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder httpsListenerBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HttpsListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = HttpsListenerResourceDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.isEqual(HttpsListenerResourceDefinition.SSL_CONTEXT).negate());
        }
        Stream<AttributeDefinition> httpListenerAttributes = httpListenerAttributes(schema);
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            httpListenerAttributes = httpListenerAttributes.filter(Predicate.not(Set.of(AbstractHttpListenerResourceDefinition.CERTIFICATE_FORWARDING, AbstractHttpListenerResourceDefinition.PROXY_ADDRESS_FORWARDING)::contains));
        }
        // Reproduce attribute order of the previous parser implementation
        Stream.of(listenerAttributes(schema), attributes, httpListenerAttributes).flatMap(Function.identity()).forEach(builder::addAttribute);
        return builder;
    }

    private static Stream<AttributeDefinition> httpListenerAttributes(UndertowSubsystemSchema schema) {
        Stream<AttributeDefinition> attributes = AbstractHttpListenerResourceDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.isEqual(AbstractHttpListenerResourceDefinition.REQUIRE_HOST_HTTP11).negate());
        }
        if (!schema.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL).negate());
        }
        return attributes;
    }

    private static Stream<AttributeDefinition> listenerAttributes(UndertowSubsystemSchema schema) {
        Stream<AttributeDefinition> attributes = ListenerResourceDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.isEqual(ListenerResourceDefinition.RFC6265_COOKIE_VALIDATION).negate());
        }
        if (!schema.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(ListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL).negate());
        }
        return attributes;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder hostBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HostDefinition.PATH_ELEMENT);

        builder.addChild(builder(LocationDefinition.PATH_ELEMENT).addAttributes(LocationDefinition.ATTRIBUTES.stream())
            .addChild(filterRefBuilder())
        );
        builder.addChild(builder(AccessLogDefinition.PATH_ELEMENT).addAttributes(AccessLogDefinition.ATTRIBUTES.stream()));
        if (schema.since(UndertowSubsystemSchema.VERSION_9_0)) {
            builder.addChild(builder(ConsoleAccessLogDefinition.PATH_ELEMENT).addAttributes(ConsoleAccessLogDefinition.ATTRIBUTES.stream()));
        }
        builder.addChild(filterRefBuilder());
        builder.addChild(builder(SingleSignOnDefinition.PATH_ELEMENT).addAttributes(Attribute.stream(SingleSignOnDefinition.Attribute.class)));
        if (schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            builder.addChild(builder(HttpInvokerDefinition.PATH_ELEMENT).addAttributes(HttpInvokerDefinition.ATTRIBUTES.stream()));
        }

        Stream<AttributeDefinition> attributes = HostDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(HostDefinition.QUEUE_REQUESTS_ON_START).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder filterRefBuilder() {
        return builder(FilterRefDefinition.PATH_ELEMENT).addAttributes(FilterRefDefinition.ATTRIBUTES.stream());
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder servletContainerBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(ServletContainerDefinition.PATH_ELEMENT);

        builder.addChild(builder(JspDefinition.PATH_ELEMENT).addAttributes(JspDefinition.ATTRIBUTES.stream()).setXmlElementName(Constants.JSP_CONFIG));
        if (schema.since(UndertowSubsystemSchema.VERSION_14_0)) {
            builder.addChild(builder(AffinityCookieDefinition.PATH_ELEMENT).addAttributes(AffinityCookieDefinition.ATTRIBUTES.stream()));
        }
        builder.addChild(builder(SessionCookieDefinition.PATH_ELEMENT).addAttributes(SessionCookieDefinition.ATTRIBUTES.stream()));
        builder.addChild(builder(PersistentSessionsDefinition.PATH_ELEMENT).addAttributes(PersistentSessionsDefinition.ATTRIBUTES.stream()));
        builder.addChild(websocketsBuilder(schema));
        builder.addChild(builder(MimeMappingDefinition.PATH_ELEMENT).addAttributes(MimeMappingDefinition.ATTRIBUTES.stream()).setXmlWrapperElement("mime-mappings"));
        builder.addChild(builder(WelcomeFileDefinition.PATH_ELEMENT).setXmlWrapperElement("welcome-files"));
        builder.addChild(builder(CrawlerSessionManagementDefinition.PATH_ELEMENT).addAttributes(CrawlerSessionManagementDefinition.ATTRIBUTES.stream()));

        Stream<AttributeDefinition> attributes = ServletContainerDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE, ServletContainerDefinition.DISABLE_SESSION_ID_REUSE)::contains));
        }
        if (!schema.since(UndertowSubsystemSchema.VERSION_5_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE, ServletContainerDefinition.FILE_CACHE_METADATA_SIZE, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)::contains));
        }
        if (!schema.since(UndertowSubsystemSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(ServletContainerDefinition.DEFAULT_COOKIE_VERSION).negate());
        }
        if (!schema.since(UndertowSubsystemSchema.VERSION_10_0)) {
            attributes = attributes.filter(Predicate.isEqual(ServletContainerDefinition.PRESERVE_PATH_ON_FORWARD).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder websocketsBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(WebsocketsDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = WebsocketsDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(WebsocketsDefinition.PER_MESSAGE_DEFLATE, WebsocketsDefinition.DEFLATER_LEVEL)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder handlersBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HandlerDefinitions.PATH_ELEMENT).setXmlElementName(Constants.HANDLERS).setNoAddOperation(true);

        builder.addChild(builder(FileHandlerDefinition.PATH_ELEMENT).addAttributes(FileHandlerDefinition.ATTRIBUTES.stream()));

        Stream<AttributeDefinition> reverseProxyHandlerAttributes = ReverseProxyHandlerDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            reverseProxyHandlerAttributes = reverseProxyHandlerAttributes.filter(Predicate.isEqual(ReverseProxyHandlerDefinition.MAX_RETRIES).negate());
        }
        Stream<AttributeDefinition> reverseProxyHandlerHostAttributes = ReverseProxyHandlerHostDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            reverseProxyHandlerHostAttributes = reverseProxyHandlerHostAttributes.filter(Predicate.not(Set.of(ReverseProxyHandlerHostDefinition.SSL_CONTEXT, ReverseProxyHandlerHostDefinition.ENABLE_HTTP2)::contains));
        }
        builder.addChild(builder(ReverseProxyHandlerDefinition.PATH_ELEMENT).addAttributes(reverseProxyHandlerAttributes)
            .addChild(builder(ReverseProxyHandlerHostDefinition.PATH_ELEMENT).addAttributes(ReverseProxyHandlerHostDefinition.ATTRIBUTES.stream()).setXmlElementName(Constants.HOST))
        );
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder modClusterBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(ModClusterDefinition.PATH_ELEMENT);

        if (schema.since(UndertowSubsystemSchema.VERSION_10_0)) {
            builder.addChild(builder(NoAffinityResourceDefinition.PATH).setXmlElementName(Constants.NO_AFFINITY));
            builder.addChild(builder(SingleAffinityResourceDefinition.PATH).setXmlElementName(Constants.SINGLE_AFFINITY));
            builder.addChild(builder(RankedAffinityResourceDefinition.PATH).addAttributes(Attribute.stream(RankedAffinityResourceDefinition.Attribute.class)).setXmlElementName(Constants.RANKED_AFFINITY));
        }

        Stream<AttributeDefinition> attributes = ModClusterDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_4_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ModClusterDefinition.FAILOVER_STRATEGY, ModClusterDefinition.SSL_CONTEXT, ModClusterDefinition.MAX_RETRIES)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder applicationSecurityDomainBuilder(UndertowSubsystemSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(ApplicationSecurityDomainDefinition.PATH_ELEMENT).setXmlWrapperElement(Constants.APPLICATION_SECURITY_DOMAINS);

        Stream<AttributeDefinition> ssoAttributes = Stream.concat(Attribute.stream(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.class), Attribute.stream(SingleSignOnDefinition.Attribute.class));
        builder.addChild(builder(SingleSignOnDefinition.PATH_ELEMENT).addAttributes(ssoAttributes));

        Stream<AttributeDefinition> attributes = ApplicationSecurityDomainDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSubsystemSchema.VERSION_7_0)) {
            attributes = attributes.filter(Predicate.isEqual(ApplicationSecurityDomainDefinition.SECURITY_DOMAIN).negate());
        }
        if (!schema.since(UndertowSubsystemSchema.VERSION_8_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ApplicationSecurityDomainDefinition.ENABLE_JASPI, ApplicationSecurityDomainDefinition.INTEGRATED_JASPI)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }
}
