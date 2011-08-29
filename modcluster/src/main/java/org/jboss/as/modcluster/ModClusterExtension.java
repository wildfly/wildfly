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

package org.jboss.as.modcluster;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

import java.util.Locale;

import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.modcluster");

    public static final String SUBSYSTEM_NAME = "modcluster";
    public static final String NAMESPACE = "urn:jboss:domain:modcluster:1.0";

    final ModClusterSubsystemElementParser parser = new ModClusterSubsystemElementParser();

    private static final DescriptionProvider DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {

        log.debugf("Activating Mod_cluster Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(ModClusterSubsystemDescriptionProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, ModClusterSubsystemAdd.INSTANCE, ModClusterSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, ModClusterSubsystemDescribe.INSTANCE, ModClusterSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // Proxy related commands.
        registration.registerOperationHandler("list-proxies", ModClusterListProxies.INSTANCE, ModClusterListProxies.INSTANCE, false);
        registration.registerOperationHandler("read-proxies-info", ModClusterGetProxyInfo.INSTANCE, ModClusterGetProxyInfo.INSTANCE, false);
        registration.registerOperationHandler("read-proxies-configuration", ModClusterGetProxyConfiguration.INSTANCE, ModClusterGetProxyConfiguration.INSTANCE, false);
        registration.registerOperationHandler("add-proxy", ModClusterAddProxy.INSTANCE, ModClusterAddProxy.INSTANCE, false);
        registration.registerOperationHandler("remove-proxy", ModClusterRemoveProxy.INSTANCE, ModClusterRemoveProxy.INSTANCE, false);

        // node related operations.
        registration.registerOperationHandler("refresh", ModClusterRefresh.INSTANCE, ModClusterRefresh.INSTANCE, false);
        registration.registerOperationHandler("reset", ModClusterReset.INSTANCE, ModClusterReset.INSTANCE, false);

        // node (all contexts) related operations.
        registration.registerOperationHandler("enable", ModClusterEnable.INSTANCE, ModClusterEnable.INSTANCE, false);
        registration.registerOperationHandler("disable", ModClusterDisable.INSTANCE, ModClusterDisable.INSTANCE, false);
        registration.registerOperationHandler("stop", ModClusterStop.INSTANCE, ModClusterStop.INSTANCE, false);

        // Context related operations.
        registration.registerOperationHandler("enable-context", ModClusterEnableContext.INSTANCE, ModClusterEnableContext.INSTANCE, false);
        registration.registerOperationHandler("disable-context", ModClusterDisableContext.INSTANCE, ModClusterDisableContext.INSTANCE, false);
        registration.registerOperationHandler("stop-context", ModClusterStopContext.INSTANCE, ModClusterStopContext.INSTANCE, false);

        // Attributes. (standard)

        registration.registerReadWriteAttribute(CommonAttributes.ADVERTISE_SOCKET, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.PROXY_LIST, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.PROXY_URL, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.ADVERTISE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.ADVERTISE_SECURITY_KEY, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.EXCLUDED_CONTEXTS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.AUTO_ENABLE_CONTEXTS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.STOP_CONTEXT_TIMEOUT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.SOCKET_TIMEOUT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);

        registration.registerReadWriteAttribute(CommonAttributes.STICKY_SESSION, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.STICKY_SESSION_REMOVE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.STICKY_SESSION_FORCE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.WORKER_TIMEOUT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.MAX_ATTEMPTS, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.FLUSH_PACKETS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.FLUSH_WAIT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.PING, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.SMAX, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.TTL, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.NODE_TIMEOUT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.BALANCER, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.DOMAIN, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);

        // Special Attributes.
        registration.registerReadWriteAttribute(CommonAttributes.DYNAMIC_LOAD_PROVIDER, null, new WriteDynamicLoadProviderOperationHandler(), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.SIMPLE_LOAD_PROVIDER, null, new WriteSimpleLoadProviderOperationHandler(), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(CommonAttributes.SSL, null, new WriteSSLProviderOperationHandler(), Storage.CONFIGURATION);

        // Metric for the  dynamic-load-provider
        registration.registerOperationHandler("add-metric", ModClusterAddMetric.INSTANCE, ModClusterAddMetric.INSTANCE, false);
        registration.registerOperationHandler("add-custom-metric", ModClusterAddCustomMetric.INSTANCE, ModClusterAddCustomMetric.INSTANCE, false);
        registration.registerOperationHandler("remove-metric", ModClusterRemoveMetric.INSTANCE, ModClusterRemoveMetric.INSTANCE, false);
        registration.registerOperationHandler("remove-custom-metric", ModClusterRemoveCustomMetric.INSTANCE, ModClusterRemoveCustomMetric.INSTANCE, false);

        subsystem.registerXMLElementWriter(parser);
    }

    public static class WriteDynamicLoadProviderOperationHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode history = operation.get(CommonAttributes.HISTORY);
            ModelNode decay = operation.get(CommonAttributes.DECAY);

            // final ModelNode submodel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode submodel = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            final ModelNode currentValue = submodel.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).clone();
            if (!history.isDefined())
                history = currentValue.get(CommonAttributes.HISTORY);
            submodel.get(CommonAttributes.HISTORY).set(history);
            if (!decay.isDefined())
                decay = currentValue.get(CommonAttributes.DECAY);
            submodel.get(CommonAttributes.DECAY).set(decay);

            submodel.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).get(CommonAttributes.HISTORY).set(history);
            submodel.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER).get(CommonAttributes.DECAY).set(decay);

            context.completeStep();
        }

    }

    public static class WriteSimpleLoadProviderOperationHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode factor = operation.get(CommonAttributes.FACTOR);

            final ModelNode submodel = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);
            final ModelNode currentValue = submodel.get(CommonAttributes.SIMPLE_LOAD_PROVIDER).clone();
            if (!factor.isDefined())
                factor = currentValue.get(CommonAttributes.HISTORY);
            submodel.get(CommonAttributes.FACTOR).set(factor);

            submodel.get(CommonAttributes.SIMPLE_LOAD_PROVIDER).get(CommonAttributes.FACTOR).set(factor);

            context.completeStep();
        }

    }
    public static class WriteSSLProviderOperationHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode  currentValue = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS);

            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.PASSWORD);
            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.CERTIFICATE_KEY_FILE);
            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.CIPHER_SUITE);
            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.PROTOCOL);
            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.VERIFY_CLIENT);
            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.CA_CERTIFICATE_FILE);
            setSSLNodeAttribute(currentValue,  operation, CommonAttributes.CA_REVOCATION_URL);

            context.completeStep();
        }
        public void setSSLNodeAttribute(ModelNode currentnode, ModelNode operation, String attribute) {
            ModelNode node = operation.get(attribute);
            if (node.isDefined())
                currentnode.get(CommonAttributes.SSL).get(attribute).set(node);
        }

    }
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }
}
