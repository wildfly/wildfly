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

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
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
public enum UndertowPersistentResourceXMLDescriptionFactory implements Function<UndertowSchema, PersistentResourceXMLDescription> {
    INSTANCE;

    @Override
    public PersistentResourceXMLDescription apply(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(UndertowRootDefinition.PATH_ELEMENT, schema.getUri());

        if (schema.since(UndertowSchema.VERSION_6_0)) {
            builder.addChild(builder(ByteBufferPoolDefinition.PATH_ELEMENT, ByteBufferPoolDefinition.ATTRIBUTES.stream()));
        }
        builder.addChild(builder(BufferCacheDefinition.PATH_ELEMENT, BufferCacheDefinition.ATTRIBUTES.stream()));
        builder.addChild(builder(ServerDefinition.PATH_ELEMENT, ServerDefinition.ATTRIBUTES.stream())
            .addChild(ajpListenerBuilder(schema))
            .addChild(httpListenerBuilder(schema))
            .addChild(httpsListenerBuilder(schema))
            .addChild(hostBuilder(schema))
        );
        builder.addChild(servletContainerBuilder(schema));
        builder.addChild(builder(HandlerDefinitions.PATH_ELEMENT).setXmlElementName(Constants.HANDLERS).setNoAddOperation(true)
            .addChild(builder(FileHandlerDefinition.PATH_ELEMENT, FileHandlerDefinition.ATTRIBUTES.stream()))
            .addChild(builder(ReverseProxyHandlerDefinition.PATH_ELEMENT, ReverseProxyHandlerDefinition.ATTRIBUTES.stream())
                .addChild(builder(ReverseProxyHandlerHostDefinition.PATH_ELEMENT, ReverseProxyHandlerHostDefinition.ATTRIBUTES.stream()).setXmlElementName(Constants.HOST))
            )
        );
        builder.addChild(PersistentResourceXMLDescription.builder(FilterDefinitions.PATH_ELEMENT).setXmlElementName(Constants.FILTERS).setNoAddOperation(true)
            .addChild(builder(RequestLimitHandlerDefinition.PATH_ELEMENT, RequestLimitHandlerDefinition.ATTRIBUTES.stream()))
            .addChild(builder(ResponseHeaderFilterDefinition.PATH_ELEMENT, ResponseHeaderFilterDefinition.ATTRIBUTES.stream()))
            .addChild(builder(GzipFilterDefinition.PATH_ELEMENT))
            .addChild(builder(ErrorPageDefinition.PATH_ELEMENT, ErrorPageDefinition.ATTRIBUTES.stream()))
            .addChild(modClusterBuilder(schema))
            .addChild(builder(CustomFilterDefinition.PATH_ELEMENT, CustomFilterDefinition.ATTRIBUTES.stream()).setXmlElementName("filter"))
            .addChild(builder(ExpressionFilterDefinition.PATH_ELEMENT, ExpressionFilterDefinition.ATTRIBUTES.stream()))
            .addChild(builder(RewriteFilterDefinition.PATH_ELEMENT, RewriteFilterDefinition.ATTRIBUTES.stream()))
        );
        builder.addChild(applicationSecurityDomainBuilder(schema));
        //here to make sure we always add filters & handlers path to mgmt model
        builder.setAdditionalOperationsGenerator((address, addOperation, operations) -> {
            operations.add(Util.createAddOperation(address.append(FilterDefinitions.PATH_ELEMENT)));
            operations.add(Util.createAddOperation(address.append(HandlerDefinitions.PATH_ELEMENT)));
        });

        Stream<AttributeDefinition> attributes = UndertowRootDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSchema.VERSION_12_0)) {
            attributes = attributes.filter(Predicate.isEqual(UndertowRootDefinition.OBFUSCATE_SESSION_ROUTE).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder.build();
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder ajpListenerBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(AjpListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = AjpListenerResourceDefinition.ATTRIBUTES.stream();
        Stream.concat(listenerAttributes(schema), attributes).forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder httpListenerBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HttpListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = HttpListenerResourceDefinition.ATTRIBUTES.stream();
        // Reproduce attribute order of the previous parser implementation
        Stream.of(listenerAttributes(schema), attributes, httpListenerAttributes(schema)).flatMap(Function.identity()).forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder httpsListenerBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HttpsListenerResourceDefinition.PATH_ELEMENT);
        Stream<AttributeDefinition> attributes = HttpsListenerResourceDefinition.ATTRIBUTES.stream();
        // Reproduce attribute order of the previous parser implementation
        Stream.of(listenerAttributes(schema), attributes, httpListenerAttributes(schema)).flatMap(Function.identity()).forEach(builder::addAttribute);
        return builder;
    }

    private static Stream<AttributeDefinition> httpListenerAttributes(UndertowSchema schema) {
        Stream<AttributeDefinition> attributes = AbstractHttpListenerResourceDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(AbstractHttpListenerResourceDefinition.PROXY_PROTOCOL).negate());
        }
        return attributes;
    }

    private static Stream<AttributeDefinition> listenerAttributes(UndertowSchema schema) {
        Stream<AttributeDefinition> attributes = ListenerResourceDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(ListenerResourceDefinition.ALLOW_UNESCAPED_CHARACTERS_IN_URL).negate());
        }
        return attributes;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder hostBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(HostDefinition.PATH_ELEMENT);

        builder.addChild(builder(LocationDefinition.PATH_ELEMENT, LocationDefinition.ATTRIBUTES.stream())
            .addChild(filterRefBuilder())
        );
        builder.addChild(builder(AccessLogDefinition.PATH_ELEMENT, AccessLogDefinition.ATTRIBUTES.stream()));
        if (schema.since(UndertowSchema.VERSION_9_0)) {
            builder.addChild(builder(ConsoleAccessLogDefinition.PATH_ELEMENT, ConsoleAccessLogDefinition.ATTRIBUTES.stream()));
        }
        builder.addChild(filterRefBuilder());
        builder.addChild(builder(SingleSignOnDefinition.PATH_ELEMENT, Attribute.stream(SingleSignOnDefinition.Attribute.class)));
        builder.addChild(builder(HttpInvokerDefinition.PATH_ELEMENT, HttpInvokerDefinition.ATTRIBUTES.stream()));

        Stream<AttributeDefinition> attributes = HostDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(HostDefinition.QUEUE_REQUESTS_ON_START).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder filterRefBuilder() {
        return builder(FilterRefDefinition.PATH_ELEMENT, FilterRefDefinition.ATTRIBUTES.stream());
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder servletContainerBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(ServletContainerDefinition.PATH_ELEMENT);

        builder.addChild(builder(JspDefinition.PATH_ELEMENT, JspDefinition.ATTRIBUTES.stream()).setXmlElementName(Constants.JSP_CONFIG));
        builder.addChild(builder(SessionCookieDefinition.PATH_ELEMENT, SessionCookieDefinition.ATTRIBUTES.stream()));
        builder.addChild(builder(PersistentSessionsDefinition.PATH_ELEMENT, PersistentSessionsDefinition.ATTRIBUTES.stream()));
        builder.addChild(builder(WebsocketsDefinition.PATH_ELEMENT, WebsocketsDefinition.ATTRIBUTES.stream()));
        builder.addChild(builder(MimeMappingDefinition.PATH_ELEMENT, MimeMappingDefinition.ATTRIBUTES.stream()).setXmlWrapperElement("mime-mappings"));
        builder.addChild(builder(WelcomeFileDefinition.PATH_ELEMENT).setXmlWrapperElement("welcome-files"));
        builder.addChild(builder(CrawlerSessionManagementDefinition.PATH_ELEMENT, CrawlerSessionManagementDefinition.ATTRIBUTES.stream()));

        Stream<AttributeDefinition> attributes = ServletContainerDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSchema.VERSION_5_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE, ServletContainerDefinition.FILE_CACHE_METADATA_SIZE, ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE)::contains));
        }
        if (!schema.since(UndertowSchema.VERSION_6_0)) {
            attributes = attributes.filter(Predicate.isEqual(ServletContainerDefinition.DEFAULT_COOKIE_VERSION).negate());
        }
        if (!schema.since(UndertowSchema.VERSION_10_0)) {
            attributes = attributes.filter(Predicate.isEqual(ServletContainerDefinition.PRESERVE_PATH_ON_FORWARD).negate());
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder modClusterBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(ModClusterDefinition.PATH_ELEMENT);

        if (schema.since(UndertowSchema.VERSION_10_0)) {
            builder.addChild(builder(NoAffinityResourceDefinition.PATH).setXmlElementName(Constants.NO_AFFINITY));
            builder.addChild(builder(SingleAffinityResourceDefinition.PATH).setXmlElementName(Constants.SINGLE_AFFINITY));
            builder.addChild(builder(RankedAffinityResourceDefinition.PATH, Attribute.stream(RankedAffinityResourceDefinition.Attribute.class)).setXmlElementName(Constants.RANKED_AFFINITY));
        }

        Stream<AttributeDefinition> attributes = ModClusterDefinition.ATTRIBUTES.stream();
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    private static PersistentResourceXMLDescription.PersistentResourceXMLBuilder applicationSecurityDomainBuilder(UndertowSchema schema) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = builder(ApplicationSecurityDomainDefinition.PATH_ELEMENT).setXmlWrapperElement(Constants.APPLICATION_SECURITY_DOMAINS);

        Stream<AttributeDefinition> ssoAttributes = Stream.concat(Attribute.stream(ApplicationSecurityDomainSingleSignOnDefinition.Attribute.class), Attribute.stream(SingleSignOnDefinition.Attribute.class));
        builder.addChild(builder(SingleSignOnDefinition.PATH_ELEMENT, ssoAttributes));

        Stream<AttributeDefinition> attributes = ApplicationSecurityDomainDefinition.ATTRIBUTES.stream();
        if (!schema.since(UndertowSchema.VERSION_7_0)) {
            attributes = attributes.filter(Predicate.isEqual(ApplicationSecurityDomainDefinition.SECURITY_DOMAIN).negate());
        }
        if (!schema.since(UndertowSchema.VERSION_8_0)) {
            attributes = attributes.filter(Predicate.not(Set.of(ApplicationSecurityDomainDefinition.ENABLE_JASPI, ApplicationSecurityDomainDefinition.INTEGRATED_JASPI)::contains));
        }
        attributes.forEach(builder::addAttribute);
        return builder;
    }

    // TODO Drop methods below once WFCORE-6218 is available
    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder(PathElement path, String namespaceUri) {
        return PersistentResourceXMLDescription.builder(path, namespaceUri);
    }

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder(PathElement path) {
        return builder(path, Stream.empty());
    }

    static PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder(PathElement path, Stream<? extends AttributeDefinition> attributes) {
        PersistentResourceXMLDescription.PersistentResourceXMLBuilder builder = PersistentResourceXMLDescription.builder(path);
        attributes.forEach(builder::addAttribute);
        return builder;
    }
}
