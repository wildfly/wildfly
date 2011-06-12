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
package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.metadata.ejb.spec.RemoveMethodMetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.modules.Module;

import java.lang.reflect.Method;

/**
 * DUP that add remove methods defined in the DD to session beans. This cannot be run
 * at the same time as other session bean DD processing as it requires the Module to
 * resolve the method.
 *
 * @author Stuart Douglas
 */
public class RemoveMethodDeploymentDescriptorProcessor extends AbstractEjbXmlDescriptorProcessor<SessionBeanMetaData> {

    @Override
    protected Class<SessionBeanMetaData> getMetaDataType() {
        return SessionBeanMetaData.class;
    }

    @Override
    protected void processBeanMetaData(final SessionBeanMetaData beanMetaData, final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if(beanMetaData.getRemoveMethods() == null || beanMetaData.getRemoveMethods().isEmpty()){
            return;
        }

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final StatefulComponentDescription sessionBean = (StatefulComponentDescription) moduleDescription.getComponentByName(beanMetaData.getEjbName());
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if(module == null) {
            return;
        }
        if(sessionBean == null) {
            //should not happen
            return;
        }

        final Class<?> componentClass;
        try {
            componentClass = module.getClassLoader().loadClass(sessionBean.getComponentClassName());
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Could not load EJB class " + sessionBean.getComponentClassName());
        }

        for(final RemoveMethodMetaData removeMethod : beanMetaData.getRemoveMethods()) {
            final NamedMethodMetaData methodData = removeMethod.getBeanMethod();
            final Method method = MethodResolutionUtils.resolveMethod(methodData, componentClass, reflectionIndex);
            sessionBean.addRemoveMethod(MethodIdentifier.getIdentifierForMethod(method), removeMethod.isRetainIfException());
        }
    }


}
