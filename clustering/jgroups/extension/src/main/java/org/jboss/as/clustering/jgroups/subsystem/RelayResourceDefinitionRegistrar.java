/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
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
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.RemoteSiteConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a relay protocol, i.e. RELAY2.
 *
 * @author Paul Ferraro
 */
public class RelayResourceDefinitionRegistrar extends ProtocolConfigurationResourceDefinitionRegistrar<RELAY2, RelayConfiguration> {

    enum Protocol implements ResourceRegistration {
        RELAY2(RelayConfiguration.PROTOCOL_NAME);

        private final PathElement path;

        Protocol(String name) {
            this.path = StackResourceDefinitionRegistrar.Component.RELAY.pathElement(name);
        }

        @Override
        public PathElement getPathElement() {
            return this.path;
        }
    }

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RelayConfiguration.SERVICE_DESCRIPTOR).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();

    enum Attribute implements AttributeDefinitionProvider {
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
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    RelayResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler) {
        this(new Configurator() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return Protocol.RELAY2;
            }

            @Override
            public ResourceDescriptionResolver getResourceDescriptionResolver() {
                return JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(StackResourceDefinitionRegistrar.Component.RELAY.getPathElement(), StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement());
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }

            @Override
            public ResourceOperationRuntimeHandler getParentRuntimeHandler() {
                return parentRuntimeHandler;
            }
        });
    }

    private RelayResourceDefinitionRegistrar(Configurator configurator) {
        super(configurator);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).provideAttributes(EnumSet.allOf(Attribute.class));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new RemoteSiteResourceDefinitionRegistrar(ResourceOperationRuntimeHandler.configureService(this)).register(registration, context);

        return registration;
    }

    @Override
    public ServiceDependency<RelayConfiguration> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<ProtocolConfiguration<RELAY2>> protocol = this.resolver.resolve(context, model);
        String stackName = context.getCurrentAddress().getParent().getLastElement().getValue();
        String siteName = Attribute.SITE.resolveModelAttribute(context, model).asString();

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> remoteSiteNames = resource.getChildrenNames(JGroupsResourceRegistration.REMOTE_SITE.getPathElement().getKey());
        List<ServiceDependency<RemoteSiteConfiguration>> remoteSites = new ArrayList<>(remoteSiteNames.size());
        for (String remoteSiteName : remoteSiteNames) {
            remoteSites.add(ServiceDependency.on(RemoteSiteConfiguration.SERVICE_DESCRIPTOR, stackName, remoteSiteName));
        }
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                protocol.accept(builder);
                for (ServiceDependency<RemoteSiteConfiguration> remoteSite : remoteSites) {
                    remoteSite.accept(builder);
                }
            }

            @Override
            public RelayConfiguration get() {
                return new AbstractRelayConfiguration(protocol.get()) {
                    @Override
                    public RELAY2 createProtocol(ChannelFactoryConfiguration configuration) {
                        RELAY2 protocol = super.createProtocol(configuration);
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

                    @Override
                    public String getSiteName() {
                        return siteName;
                    }

                    @Override
                    public List<RemoteSiteConfiguration> getRemoteSites() {
                        return remoteSites.stream().map(Supplier::get).collect(Collectors.toUnmodifiableList());
                    }
                };
            }
        };
    }

    abstract static class AbstractRelayConfiguration extends ProtocolConfigurationDecorator<RELAY2> implements RelayConfiguration {

        AbstractRelayConfiguration(ProtocolConfiguration<RELAY2> configuration) {
            super(configuration);
        }
    }
}
