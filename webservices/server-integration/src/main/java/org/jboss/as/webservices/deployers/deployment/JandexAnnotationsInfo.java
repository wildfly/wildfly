/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.server.deployment.Attachments.ANNOTATION_INDEX;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.wsf.spi.deployment.AnnotationsInfo;

/**
 * A Jandex based implementation of org.jboss.wsf.spi.deployment.AnnotationsInfo
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public final class JandexAnnotationsInfo implements AnnotationsInfo {

    private final List<ResourceRoot> resourceRoots;

    public JandexAnnotationsInfo(DeploymentUnit unit) {
        resourceRoots = ASHelper.getResourceRoots(unit);
    }

    @Override
    public boolean hasAnnotatedClasses(String... annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException();
        }
        if (resourceRoots != null) {
            Index index = null;
            for (ResourceRoot resourceRoot : resourceRoots) {
                index = resourceRoot.getAttachment(ANNOTATION_INDEX);
                if (index != null) {
                    for (String ann : annotation) {
                        List<AnnotationInstance> list = index.getAnnotations(DotName.createSimple(ann));
                        if (list != null && !list.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
