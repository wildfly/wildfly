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

package org.jboss.as.ejb3.deployment.processors.merging;

import java.util.List;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.pool.EJBBoundPoolMetaData;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.Pool;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;

/**
 * Sets up the EJB component description with the pool name configured via the {@link org.jboss.ejb3.annotation.Pool}
 * annotation and/or the deployment descriptor, for an EJB
 *
 * @author Jaikiran Pai
 */
public abstract class AbstractPoolMergingProcessor<T extends EJBComponentDescription> extends AbstractMergingProcessor<T> {

    public AbstractPoolMergingProcessor(Class<T> descriptionType) {
        super(descriptionType);
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, T description) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<Pool, String> pool = clazz.getAnnotationInformation(Pool.class);
        if (pool == null) {
            return;
        }
        if (!pool.getClassLevelAnnotations().isEmpty()) {
            final String poolName = pool.getClassLevelAnnotations().get(0);
            if (poolName == null || poolName.trim().isEmpty()) {
                throw EjbLogger.ROOT_LOGGER.poolNameCannotBeEmptyString(description.getEJBName());
            }
            this.setPoolName(description, poolName);
        }
    }

    @Override
    protected void handleDeploymentDescriptor(DeploymentUnit deploymentUnit, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, T description) throws DeploymentUnitProcessingException {
        final String ejbName = description.getEJBName();
        final EjbJarMetaData metaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (metaData == null) {
            return;
        }
        final AssemblyDescriptorMetaData assemblyDescriptor = metaData.getAssemblyDescriptor();
        if (assemblyDescriptor == null) {
            return;
        }
        // get the pool metadata
        final List<EJBBoundPoolMetaData> pools = assemblyDescriptor.getAny(EJBBoundPoolMetaData.class);

        String poolName = null;
        if (pools != null) {
            for (final EJBBoundPoolMetaData poolMetaData : pools) {
                // if this applies for all EJBs and if there isn't a pool name already explicitly specified
                // for the specific bean (i.e. via an ejb-name match)
                if ("*".equals(poolMetaData.getEjbName()) && poolName == null) {
                    poolName = poolMetaData.getPoolName();
                } else if (ejbName.equals(poolMetaData.getEjbName())) {
                    poolName = poolMetaData.getPoolName();
                }
            }
        }
        if (poolName != null) {
            this.setPoolName(description, poolName);
        }
    }

    /**
     * Set the pool name for the component
     *
     * @param componentDescription The component description
     * @param poolName             The pool name
     */
    protected abstract void setPoolName(final T componentDescription, final String poolName);

}
