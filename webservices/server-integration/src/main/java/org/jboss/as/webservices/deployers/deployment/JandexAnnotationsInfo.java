/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers.deployment;

import static org.jboss.as.server.deployment.Attachments.ANNOTATION_INDEX;
import static org.wildfly.common.Assert.checkNotNullParam;

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
        checkNotNullParam("annotation", annotation);
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
