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
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * Processes org.jboss.ejb3.annotation.SecurityDomain annotation on EJBs
 * <p/>
 * User: Jaikiran Pai
 */
public class SecurityDomainProcessor extends AbstractAnnotationEJBProcessor<EJBComponentDescription> {

    private static final Logger logger = Logger.getLogger(SecurityDomainProcessor.class);

    private static final DotName SECURITY_DOMAIN_DOT_NAME = DotName.createSimple("org.jboss.ejb3.annotation.SecurityDomain");

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
        List<AnnotationInstance> securityDomainAnnotations = annotationsOnBean.get(SECURITY_DOMAIN_DOT_NAME);
        if (securityDomainAnnotations == null || securityDomainAnnotations.isEmpty()) {
            return;
        }
        final AnnotationInstance securityDomainAnnotation = securityDomainAnnotations.get(0);
        final String securityDomain = securityDomainAnnotation.value().asString();
        logger.debug("EJB " + componentDescription.getEJBName() + " is annotated with @SecurityDomain named: " + securityDomain);
        if (securityDomain.trim().isEmpty()) {
            throw new DeploymentUnitProcessingException("SecurityDomain value on bean class: " + componentDescription.getEJBClassName() + " cannot be an empty string");
        }
        componentDescription.setSecurityDomain(securityDomain);
    }
}
