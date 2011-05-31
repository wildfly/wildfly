/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.SessionSynchronization;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class SessionSynchronizationProcessor extends AbstractAnnotationEJBProcessor<StatefulComponentDescription> {
    private static final DotName AFTER_BEGIN = DotName.createSimple(AfterBegin.class.getName());
    private static final DotName AFTER_COMPLETION = DotName.createSimple(AfterCompletion.class.getName());
    private static final DotName BEFORE_COMPLETION = DotName.createSimple(BeforeCompletion.class.getName());

    private static interface MethodProcessor {
        void process(MethodInfo method);
    }

    @Override
    protected Class<StatefulComponentDescription> getComponentDescriptionType() {
        return StatefulComponentDescription.class;
    }

    private static boolean implementsSessionSynchronization(final ClassInfo classInfo) {
        for(DotName intf : classInfo.interfaces()) {
            if (intf.toString().equals(SessionSynchronization.class.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processAnnotations(final ClassInfo beanClass, final CompositeIndex index, final StatefulComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final DotName superName = beanClass.superName();
        if (superName != null) {
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass != null)
                processAnnotations(superClass, index, componentDescription);
        }

        if (implementsSessionSynchronization(beanClass)) {
            componentDescription.setAfterBegin(null, "afterBegin");
            componentDescription.setAfterCompletion(null, "afterCompletion");
            componentDescription.setBeforeCompletion(null, "beforeCompletion");
            return;
        }

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations == null)
            return;

        processClassAnnotations(classAnnotations, AFTER_BEGIN, new MethodProcessor() {
            @Override
            public void process(MethodInfo method) {
                componentDescription.setAfterBegin(method.declaringClass().toString(), method.name().toString());
            }
        });
        processClassAnnotations(classAnnotations, AFTER_COMPLETION, new MethodProcessor() {
            @Override
            public void process(MethodInfo method) {
                componentDescription.setAfterCompletion(method.declaringClass().toString(), method.name().toString());
            }
        });
        processClassAnnotations(classAnnotations, BEFORE_COMPLETION, new MethodProcessor() {
            @Override
            public void process(MethodInfo method) {
                componentDescription.setBeforeCompletion(method.declaringClass().toString(), method.name().toString());
            }
        });
    }

    private static void processClassAnnotations(final Map<DotName, List<AnnotationInstance>> classAnnotations, final DotName annotationName, final MethodProcessor methodProcessor) throws DeploymentUnitProcessingException {
        final List<AnnotationInstance> annotations = classAnnotations.get(annotationName);
        if (annotations == null || annotations.size() == 0)
            return;

        if (annotations.size() > 1)
            throw new DeploymentUnitProcessingException("EJB 3.1 FR 4.9.4: at most one session synchronization method is allowed for " + annotationName);

        // session synchronization annotations can only be encountered on a method, so this cast is safe.
        final MethodInfo method = (MethodInfo) annotations.get(0).target();
        methodProcessor.process(method);
    }
}
