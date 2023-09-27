/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.web.common;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.ServiceName;

/**
 * Utility class to mark a {@link DeploymentUnit} or {@code OperationContext} as requiring a virtual HttpServerAuthenticationMechanismFactory.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class VirtualHttpServerMechanismFactoryMarkerUtility {

    private static final AttachmentKey<Boolean> REQUIRED = AttachmentKey.create(Boolean.class);
    private static final ServiceName MECHANISM_FACTORY_SUFFIX = ServiceName.of("http-server-mechanism-factory", "virtual");

    public static void virtualMechanismFactoryRequired(final DeploymentUnit deploymentUnit) {
        DeploymentUnit rootUnit = toRoot(deploymentUnit);
        rootUnit.putAttachment(REQUIRED, Boolean.TRUE);
    }

    public static boolean isVirtualMechanismFactoryRequired(final DeploymentUnit deploymentUnit) {
        DeploymentUnit rootUnit = toRoot(deploymentUnit);
        Boolean required = rootUnit.getAttachment(REQUIRED);

        return required == null ? false : required.booleanValue();
    }

    public static ServiceName virtualMechanismFactoryName(final DeploymentUnit deploymentUnit) {
        DeploymentUnit rootUnit = toRoot(deploymentUnit);

        return rootUnit.getServiceName().append(MECHANISM_FACTORY_SUFFIX);
    }

    public static ServiceName virtualMechanismFactoryName(final OperationContext operationContext) {
        return ServiceName.of(operationContext.getCurrentAddressValue()).append(MECHANISM_FACTORY_SUFFIX);
    }

    private static DeploymentUnit toRoot(final DeploymentUnit deploymentUnit) {
        DeploymentUnit result = deploymentUnit;
        DeploymentUnit parent = result.getParent();
        while (parent != null) {
            result = parent;
            parent = result.getParent();
        }

        return result;
    }

}
