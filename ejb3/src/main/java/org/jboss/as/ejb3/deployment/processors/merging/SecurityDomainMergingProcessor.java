/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.security.metadata.EJBBoundSecurityMetaData;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

import static org.jboss.as.ejb3.logging.EjbLogger.ROOT_LOGGER;

/**
 * @author Stuart Douglas
 */
public class SecurityDomainMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {


    public SecurityDomainMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<SecurityDomain, String> securityDomain = clazz.getAnnotationInformation(SecurityDomain.class);
        if (securityDomain == null) {
            return;
        }
        if (!securityDomain.getClassLevelAnnotations().isEmpty()) {
            if (ROOT_LOGGER.isDebugEnabled()) {
                ROOT_LOGGER.debug("EJB " + description.getEJBName() + " is part of security domain " + securityDomain.getClassLevelAnnotations().get(0));
            }
            description.setSecurityDomain(securityDomain.getClassLevelAnnotations().get(0));
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription description) throws DeploymentUnitProcessingException {
        String securityDomain = getJBossAppSecurityDomain(deploymentUnit);
        String globalSecurityDomain = null;
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData != null) {
            final AssemblyDescriptorMetaData assemblyMetadata = ejbJarMetaData.getAssemblyDescriptor();
            if (assemblyMetadata != null) {
                final List<EJBBoundSecurityMetaData> securityMetaDatas = assemblyMetadata.getAny(EJBBoundSecurityMetaData.class);
                if (securityMetaDatas != null) {
                    for (final EJBBoundSecurityMetaData securityMetaData : securityMetaDatas) {
                        if (securityMetaData.getEjbName().equals(description.getComponentName())) {
                            securityDomain = securityMetaData.getSecurityDomain();
                            break;
                        }
                        // check global security domain
                        if (securityMetaData.getEjbName().equals("*")) {
                            globalSecurityDomain = securityMetaData.getSecurityDomain();
                            continue;
                        }
                    }
                }
            }
        }
        if (securityDomain != null)
            description.setSecurityDomain(securityDomain);
        else if (globalSecurityDomain != null)
            description.setSecurityDomain(globalSecurityDomain);
    }

    /**
     * Try to obtain the security domain configured in jboss-app.xml at the ear level if available
     *
     * @param deploymentUnit
     * @return
     */
    private String getJBossAppSecurityDomain(final DeploymentUnit deploymentUnit) {
        String securityDomain = null;
        DeploymentUnit parent = deploymentUnit.getParent();
        if (parent != null) {
            final EarMetaData jbossAppMetaData = parent.getAttachment(Attachments.EAR_METADATA);
            if (jbossAppMetaData instanceof JBossAppMetaData) {
                securityDomain = ((JBossAppMetaData) jbossAppMetaData).getSecurityDomain();
            }
        }
        return securityDomain;
    }
}
