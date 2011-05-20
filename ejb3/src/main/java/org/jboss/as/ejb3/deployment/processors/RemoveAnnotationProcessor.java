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

import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import javax.ejb.Remove;
import java.util.List;
import java.util.Map;

/**
 * Process {@link Remove} annotations on stateful beans and sets up the {@link StatefulComponentDescription} accordingly.
 *
 * User: Jaikiran Pai
 */
public class RemoveAnnotationProcessor extends AbstractAnnotationEJBProcessor<StatefulComponentDescription> {

    /**
     * {@link DotName} for @javax.ejb.Remove annotation
     */
    private static final DotName REMOVE_ANNOTATION_DOT_NAME = DotName.createSimple(Remove.class.getName());

    @Override
    protected Class<StatefulComponentDescription> getComponentDescriptionType() {
        return StatefulComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex index, StatefulComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        Map<DotName, List<AnnotationInstance>> annotationsOnBean = beanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            return;
        }
        List<AnnotationInstance> removeAnnotations = annotationsOnBean.get(REMOVE_ANNOTATION_DOT_NAME);
        if (removeAnnotations == null || removeAnnotations.isEmpty()) {
            return;
        }
        for (AnnotationInstance removeAnnotation : removeAnnotations) {
            final MethodInfo targetMethod = (MethodInfo) removeAnnotation.target();
            final MethodIdentifier removeMethod = MethodIdentifier.getIdentifier(targetMethod.returnType().toString(), targetMethod.name(), toString(targetMethod.args()));
            final AnnotationValue retainIfExceptionAnnValue = removeAnnotation.value("retainIfException");
            boolean retainIfException = false;
            if (retainIfExceptionAnnValue != null) {
                retainIfException = retainIfExceptionAnnValue.asBoolean();
            }
            componentDescription.addRemoveMethod(removeMethod, retainIfException);
        }
    }

    private static String[] toString(Object[] a) {
        final String[] result = new String[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = a[i].toString();
        }
        return result;
    }

}
