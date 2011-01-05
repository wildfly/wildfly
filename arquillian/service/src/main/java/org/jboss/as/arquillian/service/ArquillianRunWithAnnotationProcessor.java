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

package org.jboss.as.arquillian.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.runner.RunWith;

/**
 * Uses the annotation index to check whether there is a class annotated with @RunWith.
 * In which case an {@link ArquillianConfig} object that names the test class is attached to the context.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 */
public class ArquillianRunWithAnnotationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(phaseContext.getDeploymentUnit());

        final List<AnnotationInstance> instances = new ArrayList<AnnotationInstance>();
        final DotName runWithName = DotName.createSimple(RunWith.class.getName());

        for (Index index : indexes.values()) {
            final List<AnnotationInstance> annotations = index.getAnnotations(runWithName);
            if (annotations != null) {
                instances.addAll(annotations);
            }
        }
        if (instances.isEmpty())
            return; // Skip if there are no @RunWith annotations

        final DeploymentUnit deploymentUnitContext = phaseContext.getDeploymentUnit();
        ArquillianConfig arqConfig = new ArquillianConfig(deploymentUnitContext);
        deploymentUnitContext.putAttachment(ArquillianConfig.KEY, arqConfig);

        for (AnnotationInstance instance : instances) {
            final AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) target;
                final String testClassName = classInfo.name().toString();
                arqConfig.addTestClass(testClassName);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
        context.removeAttachment(ArquillianConfig.KEY);
    }
}
