/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.LocalTransportProvider;
import org.jboss.as.ejb3.remote.RemotingProfileService;
import org.jboss.as.remoting.AbstractOutboundConnectionService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.RemotingOptions;
import org.wildfly.discovery.AttributeValue;
import org.wildfly.discovery.ServiceURL;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class RemotingProfileAdd extends AbstractAddStepHandler {

    static final RemotingProfileAdd INSTANCE = new RemotingProfileAdd();

    private RemotingProfileAdd() {
        super(RemotingProfileResourceDefinition.ATTRIBUTES.values());
    }

    @Override
    protected void performRuntime(final OperationContext context,final ModelNode operation,final ModelNode model)
            throws OperationFailedException {
        context.addStep((context1, operation1) -> {
            // Install another RUNTIME handler to actually install the services. This will run after the
            // RUNTIME handler for any child resources. Doing this will ensure that child resource handlers don't
            // see the installed services and can just ignore doing any RUNTIME stage work
            context1.addStep(ServiceInstallStepHandler.INSTANCE, OperationContext.Stage.RUNTIME);
        }, OperationContext.Stage.RUNTIME);
    }

    protected void installServices(final OperationContext context, final PathAddress address, final ModelNode profileNode) throws OperationFailedException {
        try {
            final String profileName = address.getLastElement().getValue();
            final ServiceName profileServiceName = RemotingProfileService.BASE_SERVICE_NAME.append(profileName);
            final ModelNode staticEjbDiscoery = StaticEJBDiscoveryDefinition.INSTANCE.resolveModelAttribute(context, profileNode);
            List<StaticEJBDiscoveryDefinition.StaticEjbDiscovery> discoveryList = StaticEJBDiscoveryDefinition.createStaticEjbList(context, staticEjbDiscoery);

            final List<ServiceURL> urls = new ArrayList<>();

            for (StaticEJBDiscoveryDefinition.StaticEjbDiscovery resource : discoveryList) {
                ServiceURL.Builder builder = new ServiceURL.Builder();
                builder.setAbstractType("ejb")
                        .setAbstractTypeAuthority("jboss")
                        .setUri(new URI(resource.getUrl()));
                String distinctName = resource.getDistinct() == null ? "" : resource.getDistinct();
                String appName = resource.getApp() == null ? "" : resource.getApp();
                String moduleName = resource.getModule();

                if (distinctName.isEmpty()) {
                    if (appName.isEmpty()) {
                        builder.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE, AttributeValue.fromString(moduleName));
                    } else {
                        builder.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE, AttributeValue.fromString(appName + "/" + moduleName));
                    }
                } else {
                    if (appName.isEmpty()) {
                        builder.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE_DISTINCT, AttributeValue.fromString(moduleName + "/" + distinctName));
                    } else {
                        builder.addAttribute(EJBClientContext.FILTER_ATTR_EJB_MODULE_DISTINCT, AttributeValue.fromString(appName + "/" + moduleName + "/" + distinctName));
                    }
                }
                urls.add(builder.create());
            }
            final Map<String, RemotingProfileService.ConnectionSpec> map = new HashMap<>();
            final RemotingProfileService profileService = new RemotingProfileService(urls, map);
            // populating the map after the fact is cheating, but it works thanks to the MSC start service "fence"
            final ServiceBuilder<RemotingProfileService> builder = context.getServiceTarget().addService(profileServiceName, profileService);
            if (profileNode.hasDefined(EJB3SubsystemModel.REMOTING_EJB_RECEIVER)) {
                for (final Property receiverProperty : profileNode.get(EJB3SubsystemModel.REMOTING_EJB_RECEIVER)
                        .asPropertyList()) {
                    final ModelNode receiverNode = receiverProperty.getValue();

                    final String connectionRef = RemotingEjbReceiverDefinition.OUTBOUND_CONNECTION_REF.resolveModelAttribute(context,
                            receiverNode).asString();
                    final long timeout = RemotingEjbReceiverDefinition.CONNECT_TIMEOUT.resolveModelAttribute(context,
                            receiverNode).asLong();

                    final ServiceName connectionDependencyService = AbstractOutboundConnectionService.OUTBOUND_CONNECTION_BASE_SERVICE_NAME
                            .append(connectionRef);
                    final InjectedValue<AbstractOutboundConnectionService> connectionInjector = new InjectedValue<AbstractOutboundConnectionService>();
                    builder.addDependency(connectionDependencyService, AbstractOutboundConnectionService.class, connectionInjector);

                    final ModelNode channelCreationOptionsNode = receiverNode.get(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS);
                    OptionMap channelCreationOptions = createChannelOptionMap(context, channelCreationOptionsNode);

                    map.put(connectionRef, new RemotingProfileService.ConnectionSpec(
                        connectionRef,
                        connectionInjector,
                        channelCreationOptions,
                        timeout
                    ));
                }
            }

            final boolean isLocalReceiverExcluded = RemotingProfileResourceDefinition.EXCLUDE_LOCAL_RECEIVER
                    .resolveModelAttribute(context, profileNode).asBoolean();
            // if the local receiver is enabled for this context, then add a dependency on the appropriate LocalEjbReceiver
            // service
            if (!isLocalReceiverExcluded) {
                final ModelNode passByValueNode = RemotingProfileResourceDefinition.LOCAL_RECEIVER_PASS_BY_VALUE
                        .resolveModelAttribute(context, profileNode);
                if (passByValueNode.isDefined()) {
                    final ServiceName localTransportProviderServiceName = passByValueNode.asBoolean() == true ? LocalTransportProvider.BY_VALUE_SERVICE_NAME
                            : LocalTransportProvider.BY_REFERENCE_SERVICE_NAME;
                    builder.addDependency(localTransportProviderServiceName, EJBTransportProvider.class, profileService.getLocalTransportProviderInjector());
                } else {
                    // setup a dependency on the default local ejb receiver service configured at the subsystem level
                    builder.addDependency(LocalTransportProvider.DEFAULT_LOCAL_TRANSPORT_PROVIDER_SERVICE_NAME, EJBTransportProvider.class, profileService.getLocalTransportProviderInjector());
                }
            }

            builder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        } catch (IllegalArgumentException | URISyntaxException e) {
            throw new OperationFailedException(e.getLocalizedMessage());
        }
    }

    private OptionMap createChannelOptionMap(final OperationContext context, final ModelNode channelCreationOptionsNode)
            throws OperationFailedException {
        final OptionMap optionMap;
        if (channelCreationOptionsNode.isDefined()) {
            final OptionMap.Builder optionMapBuilder = OptionMap.builder();
            final ClassLoader loader = this.getClass().getClassLoader();
            for (final Property optionProperty : channelCreationOptionsNode.asPropertyList()) {
                final String name = optionProperty.getName();
                final ModelNode propValueModel = optionProperty.getValue();
                final String type = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_TYPE
                        .resolveModelAttribute(context, propValueModel).asString();
                final String optionClassName = this.getClassNameForChannelOptionType(type);
                final String fullyQualifiedOptionName = optionClassName + "." + name;
                final Option option = Option.fromString(fullyQualifiedOptionName, loader);
                final String value = RemoteConnectorChannelCreationOptionResource.CHANNEL_CREATION_OPTION_VALUE
                        .resolveModelAttribute(context, propValueModel).asString();
                optionMapBuilder.set(option, option.parseValue(value, loader));
            }
            optionMap = optionMapBuilder.getMap();
        } else {
            optionMap = OptionMap.EMPTY;
        }
        return optionMap;
    }

    private String getClassNameForChannelOptionType(final String optionType) {
        if ("remoting".equals(optionType)) {
            return RemotingOptions.class.getName();
        }
        if ("xnio".equals(optionType)) {
            return Options.class.getName();
        }
        throw EjbLogger.ROOT_LOGGER.unknownChannelCreationOptionType(optionType);
    }

    private static class ServiceInstallStepHandler implements OperationStepHandler {

        private static final ServiceInstallStepHandler INSTANCE = new ServiceInstallStepHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = Resource.Tools.readModel(resource);
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            RemotingProfileAdd.INSTANCE.installServices(context, address, model);
        }
    }

}
