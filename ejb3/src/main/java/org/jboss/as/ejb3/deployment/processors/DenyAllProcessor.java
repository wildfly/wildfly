/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import javax.annotation.security.DenyAll;
import java.util.List;
import java.util.Map;

/**
 * Processes the {@link javax.annotation.security.DenyAll} annotation on a EJB
 * <p/>
 * This processor should be run *after* all the views of the EJB have been identified and set in the {@link EJBComponentDescription}
 * User: Jaikiran Pai
 */
public class DenyAllProcessor extends AbstractAnnotationEJBProcessor<EJBComponentDescription> {

    private static final DotName DENY_ALL_DOT_NAME = DotName.createSimple(DenyAll.class.getName());

    private static final Logger logger = Logger.getLogger(DenyAllProcessor.class);

    @Override
    protected Class<EJBComponentDescription> getComponentDescriptionType() {
        return EJBComponentDescription.class;
    }

    @Override
    protected void processAnnotations(final ClassInfo beanClass, final CompositeIndex index, final EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        ClassInfo klass = beanClass;
        while (klass != null) {
            Map<DotName, List<AnnotationInstance>> annotationsOnBean = klass.annotations();
            if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
                // move to the super class
                klass = getSuperClass(klass, index);
                continue;
            }
            final List<AnnotationInstance> denyAllAnnotations = annotationsOnBean.get(DENY_ALL_DOT_NAME);
            if (denyAllAnnotations == null || denyAllAnnotations.isEmpty()) {
                // move to the super class
                klass = getSuperClass(klass, index);
                continue;
            }
            for (final AnnotationInstance denyAllAnnotation : denyAllAnnotations) {
                final AnnotationTarget target = denyAllAnnotation.target();
                if (target instanceof ClassInfo) {
                    componentDescription.applyDenyAllOnAllViewsForClass(((ClassInfo) target).name().toString());
                } else if (target instanceof MethodInfo) {
                    final MethodInfo methodInfo = (MethodInfo) target;
                    final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethodInfo(methodInfo);
                    componentDescription.applyDenyAllOnAllViewsForMethod(ejbMethodIdentifier);
                }
            }
            // move to super class
            klass = this.getSuperClass(klass, index);
        }
    }

}
