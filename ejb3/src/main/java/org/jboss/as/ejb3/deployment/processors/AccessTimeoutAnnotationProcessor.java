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

import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import javax.ejb.AccessTimeout;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Processes the {@link javax.ejb.AccessTimeout} annotation on a session bean, which allows concurrent access (like @Singleton and @Stateful beans),
 * and its methods and updates the {@link SessionBeanComponentDescription} accordingly.
 * <p/>
 *
 * @author Jaikiran Pai
 */
public class AccessTimeoutAnnotationProcessor extends AbstractAnnotationEJBProcessor<SessionBeanComponentDescription> {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(AccessTimeoutAnnotationProcessor.class);

    private static final DotName ACCESS_TIMEOUT_ANNOTATION_DOT_NAME = DotName.createSimple(AccessTimeout.class.getName());

    @Override
    protected Class<SessionBeanComponentDescription> getComponentDescriptionType() {
        return SessionBeanComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex compositeIndex, SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        // @AccessTimeout applies to all types of bean (since we == JBoss uses it even in case of SLSBs to allow configuring the timeout
        // while getting an SLSB instance).
        this.processAccessTimeoutAnnotations(beanClass, compositeIndex, componentDescription);
    }

    private void processAccessTimeoutAnnotations(ClassInfo beanClass, CompositeIndex compositeIndex, SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final DotName superName = beanClass.superName();
        if (superName != null) {
            ClassInfo superClass = compositeIndex.getClassByName(superName);
            if (superClass != null)
                processAccessTimeoutAnnotations(superClass, compositeIndex, componentDescription);
        }

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations == null) {
            return;
        }

        List<AnnotationInstance> annotations = classAnnotations.get(ACCESS_TIMEOUT_ANNOTATION_DOT_NAME);
        if (annotations == null) {
            return;
        }

        for (AnnotationInstance annotationInstance : annotations) {
            AnnotationTarget target = annotationInstance.target();
            AccessTimeout accessTimeout = this.getAccessTimeout(annotationInstance);
            if (target instanceof ClassInfo) {
                // bean level
                componentDescription.setBeanLevelAccessTimeout(((ClassInfo) target).name().toString(), accessTimeout);
                logger.debug("Bean " + componentDescription.getEJBName() + " marked for access timeout: " + accessTimeout);
            } else if (target instanceof MethodInfo) {
                // method specific access timeout
                final MethodInfo methodInfo = (MethodInfo) target;
                final String[] argTypes = new String[methodInfo.args().length];
                int i = 0;
                for (Type argType : methodInfo.args()) {
                    argTypes[i++] = argType.name().toString();
                }
                MethodIdentifier identifier = MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), argTypes);
                componentDescription.setAccessTimeout(accessTimeout, identifier);
            }
        }
    }

    private AccessTimeout getAccessTimeout(AnnotationInstance annotationInstance) {
        final long timeout = annotationInstance.value().asLong();
        AnnotationValue unitAnnVal = annotationInstance.value("unit");
        final TimeUnit unit = unitAnnVal != null ? TimeUnit.valueOf(unitAnnVal.asEnum()) : TimeUnit.MILLISECONDS;
        return new AccessTimeout() {
            @Override
            public long value() {
                return timeout;
            }

            @Override
            public TimeUnit unit() {
                return unit;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return AccessTimeout.class;
            }
        };

    }

}
