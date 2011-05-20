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
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import java.util.List;
import java.util.Map;

/**
 * Processes {@link ConcurrencyManagement} annotation on a session bean, which allows concurrent access (like @Singleton and @Stateful beans),
 * and updates the {@link SessionBeanComponentDescription} with the relevant {@link ConcurrencyManagementType}
 *
 * @author Jaikiran Pai
 */
public class ConcurrencyManagementAnnotationProcessor extends AbstractAnnotationEJBProcessor<SessionBeanComponentDescription> {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(ConcurrencyManagementAnnotationProcessor.class);

    @Override
    protected Class<SessionBeanComponentDescription> getComponentDescriptionType() {
        return SessionBeanComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex compositeIndex, SessionBeanComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        if (!componentDescription.allowsConcurrentAccess()) {
            return;
        }
        Map<DotName, List<AnnotationInstance>> annotationsOnBean = beanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            return;
        }
        List<AnnotationInstance> concurrencyMgmtAnnotations = annotationsOnBean.get(DotName.createSimple(ConcurrencyManagement.class.getName()));
        if (concurrencyMgmtAnnotations == null || concurrencyMgmtAnnotations.isEmpty()) {
            return;
        }
        if (concurrencyMgmtAnnotations.size() > 1) {
            throw new DeploymentUnitProcessingException("More than one @ConcurrencyManagement annotation found on bean: " + componentDescription.getEJBName());
        }
        AnnotationInstance concurrencyMgmtAnnotation = concurrencyMgmtAnnotations.get(0);
        if (concurrencyMgmtAnnotation.target() instanceof ClassInfo == false) {
            throw new DeploymentUnitProcessingException("@ConcurrencyManagement can appear only on a class. Target: " + concurrencyMgmtAnnotation.target() + " is not a class");
        }
        AnnotationValue conMgmtAnnVal = concurrencyMgmtAnnotation.value();
        ConcurrencyManagementType concurrencyManagementType = ConcurrencyManagementType.valueOf(conMgmtAnnVal.asEnum());
        switch (concurrencyManagementType) {
            case CONTAINER:
                componentDescription.containerManagedConcurrency();
                break;
            case BEAN:
                componentDescription.beanManagedConcurrency();
                break;
            default:
                throw new DeploymentUnitProcessingException("Unexpected concurrency management type: " + concurrencyManagementType + " on bean " + componentDescription.getEJBName());
        }
        logger.debug(componentDescription.getEJBName() + " bean has been marked for " + componentDescription.getConcurrencyManagementType() + " concurrency management type");
    }
}
