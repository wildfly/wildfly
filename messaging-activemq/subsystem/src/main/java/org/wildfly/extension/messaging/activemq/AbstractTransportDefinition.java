/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * Abstract acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public abstract class AbstractTransportDefinition extends PersistentResourceDefinition {

    public static final String CONNECTOR_CAPABILITY_NAME = "org.wildfly.messaging.activemq.connector";
    public static final String ACCEPTOR_CAPABILITY_NAME = "org.wildfly.messaging.activemq.acceptor";
    private final boolean registerRuntimeOnly;
    private final AttributeDefinition[] attrs;
    protected final boolean isAcceptor;

    private static class TransportCapabilityNameMapper implements Function<PathAddress,String[]> {
        private static final TransportCapabilityNameMapper INSTANCE = new TransportCapabilityNameMapper();
        private TransportCapabilityNameMapper(){}
        @Override
        public String[] apply(PathAddress address) {
            String[] result = new String[2];
            PathAddress serverAddress = MessagingServices.getActiveMQServerPathAddress(address);
            if(serverAddress.size() > 0 ) {
                result[0] = serverAddress.getLastElement().getValue();
            } else {
                result[0] = "external";
            }
            result[1] = address.getLastElement().getValue();
            return result;
        }
    }

  public static class TransportCapabilityReferenceRecorder extends CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder {
      private final boolean external;
      public TransportCapabilityReferenceRecorder(String baseDependentName, String baseRequirementName, boolean external) {
          super(external ? DynamicNameMappers.SIMPLE : DynamicNameMappers.PARENT, baseDependentName, TransportCapabilityNameMapper.INSTANCE, baseRequirementName);
          this.external = external;
      }

      @Override
      public void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... attributeValues) {
          processCapabilityRequirement(context, attributeName, false, attributeValues);
      }

      @Override
      public void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... attributeValues) {
          processCapabilityRequirement(context, attributeName, true, attributeValues);
      }

      private void processCapabilityRequirement(OperationContext context, String attributeName, boolean remove, String... attributeValues) {
          String dependentName = getDependentName(context.getCurrentAddress());
          String requirement = getRequirementName(context.getCurrentAddress());
          for (String att : attributeValues) {
              String requirementName = RuntimeCapability.buildDynamicCapabilityName(requirement, att);
              if (remove) {
                  context.deregisterCapabilityRequirement(requirementName, dependentName, attributeName);
              } else {
                  context.registerAdditionalCapabilityRequirement(requirementName, dependentName, attributeName);
              }
          }
      }

      private String getDependentName(PathAddress address) {
          if (external) {
              return RuntimeCapability.buildDynamicCapabilityName(getBaseDependentName(), DynamicNameMappers.SIMPLE.apply(address));
          }
          return RuntimeCapability.buildDynamicCapabilityName(getBaseDependentName(), DynamicNameMappers.PARENT.apply(address));
      }

      private String getRequirementName(PathAddress address) {
          PathAddress serverAddress = MessagingServices.getActiveMQServerPathAddress(address);
          if (serverAddress.size() > 0) {
              return RuntimeCapability.buildDynamicCapabilityName(getBaseRequirementName(), serverAddress.getLastElement().getValue());
          }
          return getBaseRequirementName();
      }

      @Override
      public String getBaseRequirementName() {
          if (external) {
              return super.getBaseRequirementName() + ".external";
          }
          return super.getBaseRequirementName();
      }

        @Override
        public String[] getRequirementPatternSegments(String dynamicElement, PathAddress registrationAddress) {
            String[] dynamicElements;
            if (!external) {
                dynamicElements = new String[]{"$server"};
            } else {
                dynamicElements = new String[0];
            }
            if (dynamicElement != null && !dynamicElement.isEmpty()) {
                String[] result = new String[dynamicElements.length + 1];
                for (int i = 0; i < dynamicElements.length; i++) {
                    if (dynamicElements[i].charAt(0) == '$') {
                        result[i] = dynamicElements[i].substring(1);
                    } else {
                        result[i] = dynamicElements[i];
                    }
                }
                result[dynamicElements.length] = dynamicElement;
                return result;
            }
            return dynamicElements;
        }
    }

    protected AbstractTransportDefinition(final boolean isAcceptor, final String specificType, final boolean registerRuntimeOnly, AttributeDefinition... attrs) {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(specificType),
                new StandardResourceDescriptionResolver((isAcceptor ? CommonAttributes.ACCEPTOR : CommonAttributes.CONNECTOR),
                        MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                    @Override
                    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                        return bundle.getString(specificType);
                    }
                })
                .setCapabilities(RuntimeCapability.Builder.of(isAcceptor ? ACCEPTOR_CAPABILITY_NAME : CONNECTOR_CAPABILITY_NAME, true)
                        .setDynamicNameMapper(TransportCapabilityNameMapper.INSTANCE)
                        .build())
                .setAddHandler(new ActiveMQReloadRequiredHandlers.AddStepHandler(attrs))
                .setRemoveHandler(new ActiveMQReloadRequiredHandlers.RemoveStepHandler()));
        this.isAcceptor = isAcceptor;
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.attrs = attrs;
    }

    protected AbstractTransportDefinition(final boolean isAcceptor, final String specificType, final boolean registerRuntimeOnly, ModelVersion deprecatedSince, AttributeDefinition... attrs) {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(specificType),
                new StandardResourceDescriptionResolver((isAcceptor ? CommonAttributes.ACCEPTOR : CommonAttributes.CONNECTOR),
                        MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
                    @Override
                    public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                        return bundle.getString(specificType);
                    }
                })
                .setAddHandler(new ActiveMQReloadRequiredHandlers.AddStepHandler(attrs))
                .setRemoveHandler(new ActiveMQReloadRequiredHandlers.RemoveStepHandler())
                .setDeprecatedSince(deprecatedSince));
        this.isAcceptor = isAcceptor;
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.attrs = attrs;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(attrs);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        OperationStepHandler attributeHandler = new ReloadRequiredWriteAttributeHandler(attrs);
        for (AttributeDefinition attr : attrs) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, attributeHandler);
            }
        }

        if (isAcceptor) {
            AcceptorControlHandler.INSTANCE.registerAttributes(registry);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (isAcceptor && registerRuntimeOnly) {
            AcceptorControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
        }

        super.registerOperations(registry);
    }
}
