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

import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Map;

/**
 * This processor must be after the view annotation processors.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionAttributeAnnotationProcessor extends AbstractAnnotationEJBProcessor<EJBComponentDescription> {
    private static final DotName TRANSACTION_ATTRIBUTE_ANNOTATION_NAME = DotName.createSimple(TransactionAttribute.class.getName());

    @Override
    protected Class<EJBComponentDescription> getComponentDescriptionType() {
        return EJBComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex index, EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        processViewAnnotations(index, componentDescription);

        processClassAnnotations(beanClass, null, index, componentDescription);
    }

    /**
     * @param beanClass
     * @param methodIntf           the method-intf the annotations apply to or null if EJB class itself
     * @param index
     * @param componentDescription
     * @throws DeploymentUnitProcessingException
     *
     */
    private void processClassAnnotations(ClassInfo beanClass, MethodIntf methodIntf, CompositeIndex index, EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        final DotName superName = beanClass.superName();
        if (superName != null) {
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass != null)
                processClassAnnotations(superClass, methodIntf, index, componentDescription);
        }

        final Map<DotName, List<AnnotationInstance>> classAnnotations = beanClass.annotations();
        if (classAnnotations == null)
            return;

        List<AnnotationInstance> annotations = classAnnotations.get(TRANSACTION_ATTRIBUTE_ANNOTATION_NAME);
        if (annotations == null)
            return;

        for (AnnotationInstance annotationInstance : annotations) {
            AnnotationTarget target = annotationInstance.target();
            TransactionAttributeType transactionAttributeType = TransactionAttributeType.valueOf(annotationInstance.value().asEnum());
            if (target instanceof ClassInfo) {
                // Style 1
                final String className = target.toString();
                componentDescription.setTransactionAttribute(methodIntf, methodIntf == null ? className : null, transactionAttributeType);
            } else if (target instanceof MethodInfo) {
                // Style 3
                final MethodInfo method = (MethodInfo) target;
                final String className = method.declaringClass().toString();
                componentDescription.setTransactionAttribute(methodIntf, transactionAttributeType, className, method.name(), toString(method.args()));
            }
        }
    }

    private void processViewAnnotations(CompositeIndex index, EJBComponentDescription ejbComponentDescription) throws DeploymentUnitProcessingException {
        EJBViewDescription ejbViewDescription = null;
        for (ViewDescription viewDescription : ejbComponentDescription.getViews()) {
            ejbViewDescription = (EJBViewDescription) viewDescription;
            String viewClassName = viewDescription.getViewClassName();
            MethodIntf methodIntf = ejbViewDescription.getMethodIntf();
            ClassInfo viewClass = index.getClassByName(DotName.createSimple(viewClassName));
            if (viewClass != null) {
                processClassAnnotations(viewClass, methodIntf, index, ejbComponentDescription);
            }
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
