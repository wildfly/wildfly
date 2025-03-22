/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.LocalRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.NullRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.PrimaryOwnerRouteLocatorProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorProvider;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;

/**
 * Parser for both jboss-all.xml distributable-web namespace parsing its standalone deployment descriptor counterpart.
 * @author Paul Ferraro
 */
public class DistributableWebDeploymentXMLReader implements XMLElementReader<MutableDistributableWebDeploymentConfiguration> {

    private static final String SESSION_MANAGEMENT = "session-management";
    private static final String NAME = "name";
    private static final String HOTROD_SESSION_MANAGEMENT = "hotrod-session-management";
    private static final String INFINISPAN_SESSION_MANAGEMENT = "infinispan-session-management";
    private static final String REMOTE_CACHE_CONTAINER = "remote-cache-container";
    private static final String CACHE_CONFIGURATION = "cache-configuration";
    private static final String CACHE_CONTAINER = "cache-container";
    private static final String CACHE = "cache";
    private static final String GRANULARITY = "granularity";
    private static final String MARSHALLER = "marshaller";
    private static final String NO_AFFINITY = "no-affinity";
    private static final String LOCAL_AFFINITY = "local-affinity";
    private static final String PRIMARY_OWNER_AFFINITY = "primary-owner-affinity";
    private static final String RANKED_AFFINITY = "ranked-affinity";
    private static final String DELIMITER = "delimiter";
    private static final String MAX_ROUTES = "max-routes";
    private static final String IMMUTABLE_CLASS = "immutable-class";
    private static final String EXPIRATION_THREAD_POOL_SIZE = "expiration-thread-pool-size";

    private final DistributableWebDeploymentSchema schema;

    public DistributableWebDeploymentXMLReader(DistributableWebDeploymentSchema schema) {
        this.schema = schema;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, MutableDistributableWebDeploymentConfiguration configuration) throws XMLStreamException {
        ParseUtils.requireNoAttributes(reader);

        Set<String> names = new TreeSet<>();
        names.add(SESSION_MANAGEMENT);
        names.add(HOTROD_SESSION_MANAGEMENT);
        names.add(INFINISPAN_SESSION_MANAGEMENT);

        if (!reader.hasNext() || reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            throw ParseUtils.missingOneOf(reader, names);
        }

        switch (reader.getLocalName()) {
            case SESSION_MANAGEMENT: {
                this.readSessionManagement(reader, configuration);
                break;
            }
            case HOTROD_SESSION_MANAGEMENT: {
                MutableHotRodSessionManagementConfiguration config = new MutableHotRodSessionManagementConfiguration(configuration);
                RouteLocatorProvider provider = this.readHotRodSessionManagement(reader, config);
                configuration.setSessionManagementProvider(new HotRodSessionManagementProvider(config, config, provider));
                break;
            }
            case INFINISPAN_SESSION_MANAGEMENT: {
                MutableInfinispanSessionManagementConfiguration config = new MutableInfinispanSessionManagementConfiguration(configuration);
                RouteLocatorProvider provider = this.readInfinispanSessionManagement(reader, config, configuration);
                configuration.setSessionManagementProvider(new InfinispanSessionManagementProvider(config, config, provider));
                break;
            }
            default: {
                throw ParseUtils.unexpectedElement(reader, names);
            }
        }

        if (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            throw ParseUtils.unexpectedElement(reader);
        }
    }

    private void readSessionManagement(XMLExtendedStreamReader reader, MutableDistributableWebDeploymentConfiguration configuration) throws XMLStreamException {

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);

            switch (name) {
                case NAME: {
                    configuration.setSessionManagementName(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        this.readImmutability(reader, configuration);
    }

    @SuppressWarnings("static-method")
    private void readSessionManagementAttribute(XMLExtendedStreamReader reader, int index, MutableSessionManagementConfiguration configuration) throws XMLStreamException {
        String value = reader.getAttributeValue(index);

        switch (reader.getAttributeLocalName(index)) {
            case GRANULARITY: {
                try {
                    configuration.setSessionGranularity(value);
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                break;
            }
            case MARSHALLER: {
                try {
                    configuration.setMarshallerFactory(value);
                } catch (IllegalArgumentException e) {
                    throw ParseUtils.invalidAttributeValue(reader, index);
                }
                break;
            }
            default: {
                throw ParseUtils.unexpectedAttribute(reader, index);
            }
        }
    }

    private RouteLocatorProvider readInfinispanSessionManagement(XMLExtendedStreamReader reader, MutableInfinispanSessionManagementConfiguration configuration, Consumer<String> accumulator) throws XMLStreamException {

        Set<String> required = new TreeSet<>(Arrays.asList(CACHE_CONTAINER, GRANULARITY));

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            required.remove(name);

            switch (name) {
                case CACHE_CONTAINER: {
                    configuration.setContainerName(value);
                    break;
                }
                case CACHE: {
                    configuration.setCacheName(value);
                    break;
                }
                default: {
                    this.readSessionManagementAttribute(reader, i, configuration);
                }
            }
        }

        if (!required.isEmpty()) {
            ParseUtils.requireAttributes(reader, required.toArray(new String[required.size()]));
        }

        RouteLocatorProvider affinityFactory = this.readInfinispanAffinity(reader);

        this.readImmutability(reader, accumulator);

        return affinityFactory;
    }

    private RouteLocatorProvider readInfinispanAffinity(XMLExtendedStreamReader reader) throws XMLStreamException {
        if (!reader.hasNext() || reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            throw ParseUtils.missingRequiredElement(reader, new TreeSet<>(Arrays.asList(NO_AFFINITY, LOCAL_AFFINITY, PRIMARY_OWNER_AFFINITY)));
        }
        switch (reader.getLocalName()) {
            case PRIMARY_OWNER_AFFINITY: {
                ParseUtils.requireNoContent(reader);
                return new PrimaryOwnerRouteLocatorProvider();
            }
            case RANKED_AFFINITY: {
                if (this.schema.since(DistributableWebDeploymentSchema.VERSION_2_0)) {
                    MutableRankedRoutingConfiguration config = new MutableRankedRoutingConfiguration();
                    for (int i = 0; i < reader.getAttributeCount(); ++i) {
                        String value = reader.getAttributeValue(i);

                        switch (reader.getAttributeLocalName(i)) {
                            case DELIMITER: {
                                config.setDelimiter(value);
                                break;
                            }
                            case MAX_ROUTES: {
                                config.setMaxMembers(Integer.parseInt(value));
                                break;
                            }
                            default: {
                                throw ParseUtils.unexpectedAttribute(reader, i);
                            }
                        }
                    }
                    ParseUtils.requireNoContent(reader);
                    return new RankedRouteLocatorProvider(config);
                }
            }
            default: {
                return this.readAffinity(reader);
            }
        }
    }

    private RouteLocatorProvider readHotRodSessionManagement(XMLExtendedStreamReader reader, MutableHotRodSessionManagementConfiguration configuration) throws XMLStreamException {

        Set<String> required = new TreeSet<>(Arrays.asList(REMOTE_CACHE_CONTAINER, GRANULARITY));

        for (int i = 0; i < reader.getAttributeCount(); ++i) {
            String localName = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            required.remove(localName);

            switch (localName) {
                case REMOTE_CACHE_CONTAINER: {
                    configuration.setContainerName(value);
                    break;
                }
                case CACHE_CONFIGURATION: {
                    configuration.setConfigurationName(value);
                    break;
                }
                case EXPIRATION_THREAD_POOL_SIZE: {
                    if (this.schema.since(DistributableWebDeploymentSchema.VERSION_4_0)) {
                        break;
                    }
                }
                default: {
                    this.readSessionManagementAttribute(reader, i, configuration);
                }
            }
        }

        if (!required.isEmpty()) {
            ParseUtils.requireAttributes(reader, required.toArray(new String[required.size()]));
        }

        if (!reader.hasNext() || reader.nextTag() == XMLStreamConstants.END_ELEMENT) {
            throw ParseUtils.missingRequiredElement(reader, new TreeSet<>(Arrays.asList(NO_AFFINITY, LOCAL_AFFINITY)));
        }

        return this.readAffinity(reader);
    }

    @SuppressWarnings("static-method")
    private RouteLocatorProvider readAffinity(XMLExtendedStreamReader reader) throws XMLStreamException {

        switch (reader.getLocalName()) {
            case NO_AFFINITY: {
                ParseUtils.requireNoAttributes(reader);
                ParseUtils.requireNoContent(reader);
                return new NullRouteLocatorProvider();
            }
            case LOCAL_AFFINITY: {
                ParseUtils.requireNoAttributes(reader);
                ParseUtils.requireNoContent(reader);
                return new LocalRouteLocatorProvider();
            }
            default: {
                throw ParseUtils.unexpectedElement(reader);
            }
        }
    }

    @SuppressWarnings("static-method")
    private void readImmutability(XMLExtendedStreamReader reader, Consumer<String> accumulator) throws XMLStreamException {
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (reader.getLocalName()) {
                case IMMUTABLE_CLASS: {
                    ParseUtils.requireNoAttributes(reader);
                    accumulator.accept(reader.getElementText());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }
    }
}
