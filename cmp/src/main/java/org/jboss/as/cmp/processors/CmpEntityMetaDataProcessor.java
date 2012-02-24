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

package org.jboss.as.cmp.processors;

import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.component.CmpEntityBeanComponentDescription;
import org.jboss.as.cmp.jdbc.metadata.JDBCApplicationMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ejb3.deployment.processors.merging.AbstractMergingProcessor;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;

/**
 * @author John Bailey
 */
public class CmpEntityMetaDataProcessor extends AbstractMergingProcessor<CmpEntityBeanComponentDescription> {

    public CmpEntityMetaDataProcessor(Class<CmpEntityBeanComponentDescription> typeParam) {
        super(typeParam);
    }

    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses, DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass, CmpEntityBeanComponentDescription description) throws DeploymentUnitProcessingException {
        // No-op
    }

    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final CmpEntityBeanComponentDescription description) throws DeploymentUnitProcessingException {
        final JDBCApplicationMetaData applicationMetaData = deploymentUnit.getAttachment(Attachments.JDBC_APPLICATION_KEY);
        if(applicationMetaData == null) {
            return;
        }

        final JDBCEntityMetaData entityMetaData = applicationMetaData.getBeanByEjbName(description.getEJBName());
        if(entityMetaData == null) {
            throw CmpMessages.MESSAGES.noEntityMetaDataForEntity(description.getEJBName());
        }
        description.setEntityMetaData(entityMetaData);
    }
}
