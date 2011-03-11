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

import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import javax.ejb.Startup;
import java.util.List;
import java.util.Map;

/**
 * Processes {@link Startup} annotation on a singleton bean and updates the {@link SingletonComponentDescription}
 * with this info.
 *
 * @author Jaikiran Pai
 */
public class StartupAnnotationProcessor extends AbstractAnnotationEJBProcessor<SingletonComponentDescription> {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(StartupAnnotationProcessor.class);

    @Override
    protected Class<SingletonComponentDescription> getComponentDescriptionType() {
        return SingletonComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex compositeIndex, SingletonComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        Map<DotName, List<AnnotationInstance>> annotationsOnBean = beanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            return;
        }
        List<AnnotationInstance> startupAnnotations = annotationsOnBean.get(DotName.createSimple(Startup.class.getName()));
        if (startupAnnotations == null || startupAnnotations.isEmpty()) {
            return;
        }
        if (startupAnnotations.size() > 1) {
            throw new DeploymentUnitProcessingException("More than one @Startup annotation found on bean: " + componentDescription.getEJBName());
        }
        AnnotationInstance startupAnnotation = startupAnnotations.get(0);
        if (startupAnnotation.target() instanceof ClassInfo == false) {
            throw new DeploymentUnitProcessingException("@Startup can appear only on a class. Target: " + startupAnnotation.target() + " is not a class");
        }
        // mark the component description for init-on-startup
        componentDescription.initOnStartup();
        logger.debug(componentDescription.getEJBName() + " bean has been marked for init-on-startup (a.k.a @Startup)");
    }
}
