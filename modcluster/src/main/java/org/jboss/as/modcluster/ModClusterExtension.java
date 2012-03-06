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
import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    public static final String SUBSYSTEM_NAME = "modcluster";

    static final PathElement sslConfigurationPath = PathElement.pathElement(CommonAttributes.SSL, CommonAttributes.CONFIGURATION);
    static final PathElement configurationPath = PathElement.pathElement(CommonAttributes.MOD_CLUSTER_CONFIG);

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {

        EnumSet<Flag> runtimeOnlyFlags = EnumSet.of(Flag.RUNTIME_ONLY);
        ROOT_LOGGER.debugf("Activating Mod_cluster Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, Namespace.CURRENT.getMajorVersion(), Namespace.CURRENT.getMinorVersion());
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(ModClusterSubsystemDescriptionProviders.SUBSYSTEM);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, ModClusterSubsystemAdd.INSTANCE, ModClusterSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, ModClusterSubsystemRemove.INSTANCE, ModClusterSubsystemRemove.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, ModClusterSubsystemDescribe.INSTANCE, ModClusterSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // The following ops only affect the runtime and not the configuration, so don't register them on the Host Controller
        if (context.isRuntimeOnlyRegistrationValid()) {
            // Proxy related commands.
            registration.registerOperationHandler("list-proxies", ModClusterListProxies.INSTANCE, ModClusterListProxies.INSTANCE, false, runtimeOnlyFlags);
            registration.registerOperationHandler("read-proxies-info", ModClusterGetProxyInfo.INSTANCE, ModClusterGetProxyInfo.INSTANCE, false, runtimeOnlyFlags);
            registration.registerOperationHandler("read-proxies-configuration", ModClusterGetProxyConfiguration.INSTANCE, ModClusterGetProxyConfiguration.INSTANCE, false, runtimeOnlyFlags);

            //These seem to be modifying the state so don't add the runtimeOnly stuff for now
            registration.registerOperationHandler("add-proxy", ModClusterAddProxy.INSTANCE, ModClusterAddProxy.INSTANCE, false);
            registration.registerOperationHandler("remove-proxy", ModClusterRemoveProxy.INSTANCE, ModClusterRemoveProxy.INSTANCE, false);

            // node related operations.
            registration.registerOperationHandler("refresh", ModClusterRefresh.INSTANCE, ModClusterRefresh.INSTANCE, false, runtimeOnlyFlags);
            registration.registerOperationHandler("reset", ModClusterReset.INSTANCE, ModClusterReset.INSTANCE, false, runtimeOnlyFlags);

            // node (all contexts) related operations.
            registration.registerOperationHandler("enable", ModClusterEnable.INSTANCE, ModClusterEnable.INSTANCE, false);
            registration.registerOperationHandler("disable", ModClusterDisable.INSTANCE, ModClusterDisable.INSTANCE, false);
            registration.registerOperationHandler("stop", ModClusterStop.INSTANCE, ModClusterStop.INSTANCE, false);

            // Context related operations.
            registration.registerOperationHandler("enable-context", ModClusterEnableContext.INSTANCE, ModClusterEnableContext.INSTANCE, false);
            registration.registerOperationHandler("disable-context", ModClusterDisableContext.INSTANCE, ModClusterDisableContext.INSTANCE, false);
            registration.registerOperationHandler("stop-context", ModClusterStopContext.INSTANCE, ModClusterStopContext.INSTANCE, false);
        }

        final ManagementResourceRegistration configuration = registration.registerSubModel(new ModClusterConfigResourceDefinition());

        configuration.registerSubModel(new ModClusterSSLResourceDefinition());

        subsystem.registerXMLElementWriter(new ModClusterSubsystemXMLWriter());
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), namespace.getXMLReader());
            }
        }
    }
}
