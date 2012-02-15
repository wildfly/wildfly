/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.web.ext;

import org.apache.catalina.core.StandardContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * @author Emanuel Muckenhuber
 */
public interface WebContextFactory {

    /**
     * The context factory attachment key
     */
    AttachmentKey<WebContextFactory> ATTACHMENT = AttachmentKey.create(WebContextFactory.class);

    /**
     * Create a new web context.
     *
     * @param deploymentUnit the deployment unit
     * @return the context
     * @throws DeploymentUnitProcessingException
     */
    StandardContext createContext(final DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException;

    void postProcessContext(final DeploymentUnit deploymentUnit, final StandardContext webContext);

    /**
     * The default factory.
     */
    WebContextFactory DEFAULT = new WebContextFactory() {

        @Override
        public StandardContext createContext(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
            return new StandardContext();
        }

        @Override
        public void postProcessContext(final DeploymentUnit deploymentUnit, final StandardContext webContext) {
            // NOOP
        }
    };
}
