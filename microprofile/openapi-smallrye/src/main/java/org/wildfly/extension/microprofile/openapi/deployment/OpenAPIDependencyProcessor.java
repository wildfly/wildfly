/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.openapi.deployment;

import java.util.EnumSet;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 * Adds MicroProfile OpenAPI dependencies to deployment.
 *
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIDependencyProcessor implements DeploymentUnitProcessor {

    static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    @Override
    public void deploy(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();

        // If this is a sub-deployment, we can afford to be more discerning of the type of web application while still passing the TCK.
        boolean enabled = DeploymentTypeMarker.isType(DeploymentType.WAR, unit) && ((unit.getParent() == null) || isJaxrsDeployment(unit));
        unit.putAttachment(ATTACHMENT_KEY, enabled);

        if (enabled) {
            ModuleSpecification specification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
            ModuleLoader loader = Module.getBootModuleLoader();

            specification.addSystemDependency(new ModuleDependency(loader, "org.eclipse.microprofile.openapi.api", false, false, false, false));
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        unit.removeAttachment(ATTACHMENT_KEY);
    }

    private static boolean isJaxrsDeployment(DeploymentUnit unit) {
        CompositeIndex index = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (JaxrsAnnotations annotation : EnumSet.allOf(JaxrsAnnotations.class)) {
            if (!index.getAnnotations(annotation.getDotName()).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
