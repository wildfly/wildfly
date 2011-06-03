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
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.junit.runner.RunWith;

/**
 * Uses the annotation index to check whether there is a class annotated with @RunWith.
 * In which case an {@link ArquillianConfig} service is created.
 *
 * @author Thomas.Diesler@jboss.com
 */
public class ArquillianConfigBuilder {

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    ArquillianConfigBuilder(DeploymentUnit deploymentUnit) {
    }

    static ArquillianConfig processDeployment(ArquillianService arqService, DeploymentUnit depUnit) {

        final CompositeIndex compositeIndex = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            log.warnf("Cannot find composite annotation index in: %s", depUnit);
            return null;
        }

        final DotName runWithName = DotName.createSimple(RunWith.class.getName());
        final List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);
        if (runWithList.isEmpty()) {
            return null;
        }

        // FIXME: Why do we get another service started event from a deployment INSTALLED service?
        ArquillianConfig arqConfig = new ArquillianConfig(arqService, depUnit, runWithList);
        if (arqService.getServiceContainer().getService(arqConfig.getServiceName()) != null) {
            log.warnf("Arquillian config already registered: %s", arqConfig);
            return null;
        }

        depUnit.putAttachment(ArquillianConfig.KEY, arqConfig);
        return arqConfig;
    }
}
