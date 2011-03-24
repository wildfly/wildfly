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

import java.util.List;
import java.util.Map;
import javax.ejb.Asynchronous;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * Deployment processor responsible for detecting @Asynchronous annotations on session bean components.
 *
 * @author John Bailey
 */
public class AsynchronousAnnotationProcessor extends AbstractAnnotationEJBProcessor<SessionBeanComponentDescription> {

    private static final DotName ASYNCHRONOUS_ANNOTATION = DotName.createSimple(Asynchronous.class.getName());

    protected Class<SessionBeanComponentDescription> getComponentDescriptionType() {
        return SessionBeanComponentDescription.class;
    }

    protected void processAnnotations(final ClassInfo beanClass, final CompositeIndex compositeIndex, final SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        processAsyncAnnotation(beanClass, compositeIndex, componentDescription);
    }

    private void processAsyncAnnotation(final ClassInfo beanClass, final CompositeIndex compositeIndex, final SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final DotName superName = beanClass.superName();
        if (superName != null) {
            ClassInfo superClass = compositeIndex.getClassByName(superName);
            if (superClass != null)
                processAsyncAnnotation(superClass, compositeIndex, componentDescription);
        }

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations == null) {
            return;
        }

        List<AnnotationInstance> annotations = classAnnotations.get(ASYNCHRONOUS_ANNOTATION);
        if (annotations == null) {
            return;
        }

        for (AnnotationInstance annotationInstance : annotations) {
            AnnotationTarget target = annotationInstance.target();
            if (target instanceof ClassInfo) {
                componentDescription.addAsynchronousView(ClassInfo.class.cast(target).name().toString());
            } else if (target instanceof MethodInfo) {
                final MethodInfo methodInfo = MethodInfo.class.cast(target);
                final String[] args = new String[methodInfo.args().length];
                for(int i = 0; i < methodInfo.args().length; i++) {
                    args[i] = methodInfo.args()[i].name().toString();
                }
                componentDescription.addAsynchronousMethod(MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), args));
            }
        }
    }

}
