/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.deployment.WeldPortableExtensions;

/**
 * @author paul.robinson@redhat.com, 2012-02-09
 */
public class CDIExtensionProcessor implements DeploymentUnitProcessor {

    private static final String[] EMPTY_STRING_ARRAY = {};
    private static final String[] EXTENSIONS = {"org.jboss.narayana.txframework.impl.as.TXFrameworkCDIExtension",
            "org.jboss.narayana.compensations.impl.CompensationsCDIExtension"};

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();

        if (!XTSDeploymentMarker.isXTSAnnotationDeployment(unit)) {
            return;
        }

        final ClassLoader cl = unit.getAttachment(Attachments.MODULE).getClassLoader();
        WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(unit);
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            for (String fqn : EXTENSIONS) {

                try {
                    final Class<?> extension = Class.forName(fqn, true, cl);
                    extensions.tryRegisterExtension(extension, unit);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
