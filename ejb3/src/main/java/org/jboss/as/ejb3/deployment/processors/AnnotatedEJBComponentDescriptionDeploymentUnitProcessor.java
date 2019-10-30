/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;

/**
 * Make sure we process annotations first and then start on the descriptors.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class AnnotatedEJBComponentDescriptionDeploymentUnitProcessor extends AbstractDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private final EJBComponentDescriptionFactory[] factories;

    public AnnotatedEJBComponentDescriptionDeploymentUnitProcessor(final boolean appclient, final boolean defaultMdbPoolAvailable, final boolean defaultSlsbPoolAvailable) {
        super(appclient);
        this.factories = new EJBComponentDescriptionFactory[] {
                new MessageDrivenComponentDescriptionFactory(appclient, defaultMdbPoolAvailable),
                new SessionBeanComponentDescriptionFactory(appclient, defaultSlsbPoolAvailable)
        };
    }

    @Override
    protected void processAnnotations(final DeploymentUnit deploymentUnit, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {
        for (final EJBComponentDescriptionFactory factory : factories) {
            factory.processAnnotations(deploymentUnit, compositeIndex);
        }
    }

    @Override
    protected void processBeanMetaData(final DeploymentUnit deploymentUnit, final EnterpriseBeanMetaData ejb) throws DeploymentUnitProcessingException {
        for (final EJBComponentDescriptionFactory factory : factories) {
            factory.processBeanMetaData(deploymentUnit, ejb);
        }
    }
}
