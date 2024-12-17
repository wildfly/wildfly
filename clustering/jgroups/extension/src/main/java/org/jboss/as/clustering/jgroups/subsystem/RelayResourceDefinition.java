/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;
import org.jgroups.protocols.FORK;
import org.jgroups.protocols.relay.RELAY2;
import org.jgroups.protocols.relay.config.RelayConfig;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource definition for /subsystem=jgroups/stack=X/relay=RELAY
 *
 * @author Paul Ferraro
 */
public class RelayResourceDefinition extends AbstractProtocolResourceDefinition<RELAY2, RelayConfiguration> {

    static final PathElement PATH = pathElement(RelayConfiguration.PROTOCOL_NAME);
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("relay", name);
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RelayConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SITE("site", ModelType.STRING),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    RelayResourceDefinition() {
        super(new Parameters(PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), CAPABILITY, new SimpleResourceDescriptorConfigurator<>(Attribute.class), null);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new RemoteSiteResourceDefinition(this).register(registration);

        return registration;
    }

    @Override
    public Map.Entry<Function<ProtocolConfiguration<RELAY2>, RelayConfiguration>, Consumer<RequirementServiceBuilder<?>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String stackName = address.getParent().getLastElement().getValue();

        String siteName = Attribute.SITE.resolveModelAttribute(context, model).asString();

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> remoteSiteNames = resource.getChildrenNames(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey());
        List<ServiceDependency<RemoteSiteConfiguration>> remoteSites = new ArrayList<>(remoteSiteNames.size());
        for (String remoteSiteName : remoteSiteNames) {
            ServiceDependency<RemoteSiteConfiguration> remoteSite = ServiceDependency.on(RemoteSiteResourceDefinition.SERVICE_DESCRIPTOR, stackName, remoteSiteName);
            remoteSites.add(remoteSite);
        }

        return Map.entry(new Function<>() {
            @Override
            public RelayConfiguration apply(ProtocolConfiguration<RELAY2> configuration) {
                return new AbstractRelayConfiguration(configuration) {
                    @Override
                    public String getSiteName() {
                        return siteName;
                    }

                    @Override
                    public List<RemoteSiteConfiguration> getRemoteSites() {
                        return remoteSites.stream().map(Supplier::get).collect(Collectors.toUnmodifiableList());
                    }

                    @Override
                    public RELAY2 createProtocol(ProtocolStackConfiguration stackConfiguration) {
                        RELAY2 protocol = super.createProtocol(stackConfiguration);
                        List<RemoteSiteConfiguration> remoteSites = this.getRemoteSites();
                        List<String> sites = new ArrayList<>(remoteSites.size() + 1);
                        sites.add(siteName);
                        // Collect bridges, eliminating duplicates
                        Map<String, RelayConfig.BridgeConfig> bridges = new HashMap<>();
                        for (RemoteSiteConfiguration remoteSite: remoteSites) {
                            String siteName = remoteSite.getName();
                            sites.add(siteName);
                            String clusterName = remoteSite.getClusterName();
                            RelayConfig.BridgeConfig bridge = new RelayConfig.BridgeConfig(clusterName) {
                                @Override
                                public JChannel createChannel() throws Exception {
                                    JChannel channel = remoteSite.getChannelFactory().createChannel(siteName);
                                    // Don't use FORK in bridge stack
                                    channel.getProtocolStack().removeProtocol(FORK.class);
                                    return channel;
                                }
                            };
                            bridges.put(clusterName, bridge);
                        }
                        protocol.site(siteName);
                        for (String site: sites) {
                            RelayConfig.SiteConfig siteConfig = new RelayConfig.SiteConfig(site);
                            protocol.addSite(site, siteConfig);
                            if (site.equals(siteName)) {
                                for (RelayConfig.BridgeConfig bridge: bridges.values()) {
                                    siteConfig.addBridge(bridge);
                                }
                            }
                        }
                        return protocol;
                    }
                };
            }
        }, new Consumer<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                for (ServiceDependency<RemoteSiteConfiguration> remoteSite : remoteSites) {
                    remoteSite.accept(builder);
                }
            }
        });
    }

    abstract static class AbstractRelayConfiguration extends ProtocolConfigurationDecorator<RELAY2> implements RelayConfiguration {

        AbstractRelayConfiguration(ProtocolConfiguration<RELAY2> configuration) {
            super(configuration);
        }
    }
}
