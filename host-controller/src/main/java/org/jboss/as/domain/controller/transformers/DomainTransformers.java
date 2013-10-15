/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.transformers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;

import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AddNameFromAddressResourceTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformerRegistry;
import org.jboss.as.controller.transform.TransformersSubRegistration;

/**
 * Global transformation rules for the domain, host and server-config model.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainTransformers {

    /** Dummy version for ignored subsystems. */
    static final ModelVersion IGNORED_SUBSYSTEMS = ModelVersion.create(-1);

    private static final PathElement JSF_EXTENSION = PathElement.pathElement(ModelDescriptionConstants.EXTENSION, "org.jboss.as.jsf");

    //AS 7.1.2.Final / EAP 6.0.0
    private static final ModelVersion VERSION_1_2 = ModelVersion.create(1, 2, 0);
    //AS 7.1.3.Final / EAP 6.0.1
    private static final ModelVersion VERSION_1_3 = ModelVersion.create(1, 3, 0);
    //AS 7.2.0.Final / EAP 6.1.0 / EAP 6.1.1
    private static final ModelVersion VERSION_1_4 = ModelVersion.create(1, 4, 0);
    // EAP 6.2.0
    private static final ModelVersion VERSION_1_5 = ModelVersion.create(1, 5, 0);
    //WF 8.0.0.Final
    private static final ModelVersion VERSION_2_0 = ModelVersion.create(2, 0, 0);

    /**
     * Initialize the domain registry.
     *
     * @param registry the domain registry
     */
    public static void initializeDomainRegistry(final TransformerRegistry registry) {

        initializeDomainRegistryEAP60(registry, VERSION_1_2);
        initializeDomainRegistryEAP60(registry, VERSION_1_3);
        initializeDomainRegistry14(registry);
        initializeDomainRegistry15(registry);
    }

    private static void initializeDomainRegistryEAP60(TransformerRegistry registry, ModelVersion modelVersion) {
        TransformersSubRegistration domain = registry.getDomainRegistration(modelVersion);

        ManagementTransformers.registerTransformersPreRBAC(domain);

        // Discard all operations to the newly introduced jsf extension
        domain.registerSubResource(JSF_EXTENSION, IGNORED_EXTENSIONS);

        JSFSubsystemTransformers.registerTransformers120(registry, domain);
        PathsTransformers.registerTransformers120(domain);
        DeploymentTransformers.registerTransformers120(domain);
        SystemPropertyTransformers.registerTransformers120(domain);
        SocketBindingGroupTransformers.registerTransformers120(domain);
        ServerGroupTransformers.registerTransformers120(domain);

        //Add the domain interface and path name. This is currently from a read attribute handler but in < 1.4.0 it existed in the model
        domain.registerSubResource(PathElement.pathElement(INTERFACE), AddNameFromAddressResourceTransformer.INSTANCE);
        domain.registerSubResource(PathElement.pathElement(PATH), AddNameFromAddressResourceTransformer.INSTANCE);
    }

    private static void initializeDomainRegistry14(TransformerRegistry registry) {
        TransformersSubRegistration domain = registry.getDomainRegistration(VERSION_1_4);

        ManagementTransformers.registerTransformersPreRBAC(domain);
    }

    private static void initializeDomainRegistry15(TransformerRegistry registry) {
        // currently no transformation needed
    }

    private static final ResourceTransformer IGNORED_EXTENSIONS = new IgnoreExtensionResourceTransformer();

    /**
     * Special resource transformer automatically ignoring all subsystems registered by an extension.
     */
    static class IgnoreExtensionResourceTransformer implements ResourceTransformer {

        @Override
        public void transformResource(final ResourceTransformationContext context, final PathAddress address, final Resource resource) throws OperationFailedException {
            // we just ignore this resource  - so don't add it: context.addTransformedResource(...)
            final PathElement element = address.getLastElement();

            final TransformationTarget target = context.getTarget();
            final ExtensionRegistry registry = target.getExtensionRegistry();

            final Map<String, SubsystemInformation> subsystems = registry.getAvailableSubsystems(element.getValue());
            if(subsystems != null) {
                for(final Map.Entry<String, SubsystemInformation> subsystem : subsystems.entrySet()) {
                    final String name = subsystem.getKey();
                    target.addSubsystemVersion(name, IGNORED_SUBSYSTEMS);
                }
            }
        }
    }
}
