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

package org.jboss.as.ejb3.deployment.processors.entity;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.deployment.processors.EJBComponentDescriptionFactory;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EntityBeanMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public class EntityBeanComponentDescriptionFactory extends EJBComponentDescriptionFactory {

    private static final Logger logger = Logger.getLogger(EntityBeanComponentDescriptionFactory.class);

    /**
     * If this is an appclient we want to make the components as not installable, so we can still look up which EJB's are in
     * the deployment, but do not actuall install them
     */
    private final boolean appclient;

    public EntityBeanComponentDescriptionFactory(final boolean appclient) {
        this.appclient = appclient;
    }

    protected void mark(final DeploymentUnit deploymentUnit) {
        // BMP Entities do not need a mark
    }

    @Override
    protected void processAnnotations(DeploymentUnit deploymentUnit, CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {

    }

    @Override
    protected void processBeanMetaData(final DeploymentUnit deploymentUnit, final EnterpriseBeanMetaData enterpriseBeanMetaData) throws DeploymentUnitProcessingException {
        if (enterpriseBeanMetaData.isEntity()) {
            assert enterpriseBeanMetaData instanceof EntityBeanMetaData : enterpriseBeanMetaData + " is not an EntityBeanMetaData";
            processEntityBeanMetaData(deploymentUnit, (EntityBeanMetaData) enterpriseBeanMetaData);
        }
    }


    private void processEntityBeanMetaData(final DeploymentUnit deploymentUnit, final EntityBeanMetaData entity) throws DeploymentUnitProcessingException {
        final EjbJarDescription ejbJarDescription = getEjbJarDescription(deploymentUnit);

        final String beanName = entity.getName();
        final String beanClassName = entity.getEjbClass();

        if (!shouldProcess(entity)) {
            return;
        }

        mark(deploymentUnit);

        final EntityBeanComponentDescription description = createDescription(beanName, beanClassName, ejbJarDescription, deploymentUnit.getServiceName());
        description.setDeploymentDescriptorEnvironment(new DeploymentDescriptorEnvironment("java:comp/env/", entity));

        // add it to the ejb jar description
        if (appclient) {
            deploymentUnit.addToAttachmentList(Attachments.ADDITIONAL_RESOLVABLE_COMPONENTS, description);
        } else {
            // Add this component description to module description
            ejbJarDescription.getEEModuleDescription().addComponent(description);
        }
        description.setDescriptorData(entity);

        description.setPersistenceType(entity.getPersistenceType());
        description.setReentrant(entity.isReentrant());
        description.setPrimaryKeyType(entity.getPrimKeyClass());
        //TODO: validation

        final String localHome = entity.getLocalHome();
        if (localHome != null) {
            description.addLocalHome(localHome);
        }

        final String local = entity.getLocal();
        if (local != null) {
            description.addEjbLocalObjectView(local);
        }

        final String home = entity.getHome();
        if (home != null) {
            description.addRemoteHome(home);
        }

        final String remote = entity.getRemote();
        if (remote != null) {
            description.addEjbObjectView(remote);
        }
    }

    protected boolean shouldProcess(EntityBeanMetaData entity) {
        return entity.isBMP();
    }

    protected EntityBeanComponentDescription createDescription(final String beanName, final String beanClassName, final EjbJarDescription ejbJarDescription, final ServiceName serviceName) {
        return new EntityBeanComponentDescription(beanName, beanClassName, ejbJarDescription, serviceName);
    }

}
