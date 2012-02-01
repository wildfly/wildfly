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

package org.jboss.as.arquillian.osgi.service;

import java.util.List;

import javax.inject.Inject;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;

/**
 * Uses the annotation index to check whether the OSGi needs to get activated.
 * If so, the {@link ArquillianConfig} service has a dependency on the framework.
 *
 * @author Thomas.Diesler@jboss.com
 */
class FrameworkActivationProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.as.arquillian");

    static void process(final ServiceBuilder<ArquillianConfig> builder, final ArquillianConfig arqConfig) {

        final DeploymentUnit depUnit = arqConfig.getDeploymentUnit();
        final CompositeIndex compositeIndex = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if(compositeIndex == null) {
            log.warnf("Cannot find composite annotation index in: %s", depUnit);
            return;
        }

        final DotName dotName = DotName.createSimple(Inject.class.getName());
        final List<AnnotationInstance> annotationList = compositeIndex.getAnnotations(dotName);
        if (annotationList.isEmpty()) {
            return;
        }

        for (AnnotationInstance instance : annotationList) {
            final AnnotationTarget target = instance.target();
            if (target instanceof FieldInfo) {
                final FieldInfo fieldInfo = (FieldInfo) target;
                String typeName = fieldInfo.type().toString();
                if (typeName.startsWith("org.osgi.framework")) {
                    log.debugf("OSGi injection point of type '%s' detected: %s", typeName, fieldInfo.declaringClass());
                    arqConfig.addFrameworkDependency(builder);
                    break;
                }
            }
        }
    }
}
