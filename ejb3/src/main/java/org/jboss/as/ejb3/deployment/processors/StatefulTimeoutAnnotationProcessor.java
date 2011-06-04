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

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import javax.ejb.StatefulTimeout;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Processes the {@link javax.ejb.StatefulTimeout} annotation on a session bean
 *
 * @author Stuart Douglas
 */
public class StatefulTimeoutAnnotationProcessor extends AbstractAnnotationEJBProcessor<StatefulComponentDescription> {

    private static final DotName TIMEOUT_ANNOTATION_DOT_NAME = DotName.createSimple(StatefulTimeout.class.getName());

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(StatefulTimeoutAnnotationProcessor.class);

    @Override
    protected Class<StatefulComponentDescription> getComponentDescriptionType() {
        return StatefulComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex compositeIndex, StatefulComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations == null) {
            return;
        }

        List<AnnotationInstance> annotations = classAnnotations.get(TIMEOUT_ANNOTATION_DOT_NAME);
        if (annotations == null) {
            return;
        }

        for (AnnotationInstance annotationInstance : annotations) {
            AnnotationTarget target = annotationInstance.target();
            long value = annotationInstance.value().asLong();
            AnnotationValue unitValue = annotationInstance.value("unit");
            TimeUnit unit;
            if(unitValue != null) {
                unit = TimeUnit.valueOf(annotationInstance.value("unit").asEnum());
            } else {
                unit = TimeUnit.MINUTES;
            }
            if (target instanceof ClassInfo) {
                // bean level
                componentDescription.setStatefulTimeout(new StatefulTimeoutInfo(value, unit));
                logger.debug("Bean " + componentDescription.getEJBName() + " marked for expiration using @StatefulTimeout " + target);
            } else  {
                logger.warn("@StatefulTime not placed on class in " + target);
            }
        }
    }


}
