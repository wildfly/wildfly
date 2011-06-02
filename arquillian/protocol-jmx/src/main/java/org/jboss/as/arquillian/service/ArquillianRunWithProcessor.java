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

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.junit.runner.RunWith;

/**
 * Uses the annotation index to check whether there is a class annotated with @RunWith.
 * In which case an {@link ArquillianConfig} object that names the test class is attached to the context.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
 */
public class ArquillianRunWithProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    private final ServiceName serviceName;
    private final DeploymentUnit deploymentUnit;

    ArquillianRunWithProcessor(ServiceName serviceName, DeploymentUnit deploymentUnit) {
        this.serviceName = serviceName;
        this.deploymentUnit = deploymentUnit;
    }

    ArquillianConfig getArquillianConfig() {

        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            log.infof("Cannot find composite annotation index in: %s", deploymentUnit);
            return null;
        }

        final DotName runWithName = DotName.createSimple(RunWith.class.getName());
        final List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);
        if (runWithList.isEmpty()) {
            return null;
        }

        log.infof("Arquillian test deployment detected: %s", deploymentUnit);
        ArquillianConfig arqConfig = new ArquillianConfig(serviceName, deploymentUnit);
        deploymentUnit.putAttachment(ArquillianConfig.KEY, arqConfig);

        for (AnnotationInstance instance : runWithList) {
            final AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) target;
                final String testClassName = classInfo.name().toString();
                arqConfig.addTestClass(testClassName);
            }
        }

        return arqConfig;
    }
}
