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

import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import javax.annotation.security.DeclareRoles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Processes {@link javax.annotation.security.DeclareRoles} annotation on a EJB
 * <p/>
 * User: Jaikiran Pai
 */
public class DeclareRolesProcessor extends AbstractAnnotationEJBProcessor<EJBComponentDescription> {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(DeclareRolesProcessor.class);

    private static final DotName DECLARE_ROLES_DOT_NAME = DotName.createSimple(DeclareRoles.class.getName());

    @Override
    protected Class<EJBComponentDescription> getComponentDescriptionType() {
        return EJBComponentDescription.class;
    }

    @Override
    protected void processAnnotations(ClassInfo beanClass, CompositeIndex index, EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        Map<DotName, List<AnnotationInstance>> annotationsOnBean = beanClass.annotations();
        if (annotationsOnBean == null || annotationsOnBean.isEmpty()) {
            return;
        }
        final List<AnnotationInstance> declareRolesAnnotations = annotationsOnBean.get(DECLARE_ROLES_DOT_NAME);
        if (declareRolesAnnotations == null || declareRolesAnnotations.isEmpty()) {
            return;
        }
        if (declareRolesAnnotations.size() > 1) {
            throw new DeploymentUnitProcessingException("More than one @DeclareRoles annotation found on bean: " + componentDescription.getEJBName());
        }
        AnnotationInstance declareRolesAnnotation = declareRolesAnnotations.get(0);
        if (declareRolesAnnotation.target() instanceof ClassInfo == false) {
            throw new DeploymentUnitProcessingException("@DeclareRoles can appear only on a class. Target: " + declareRolesAnnotation.target() + " is not a class");
        }
        final AnnotationValue annotationValue = declareRolesAnnotation.value();
        final String[] roles = annotationValue.asStringArray();
        if (roles != null) {
            // add the declared roles
            componentDescription.addDeclaredRoles(roles);
            logger.debug(componentDescription.getEJBName() + " bean declares roles: " + Arrays.toString(roles));
        }
    }
}
