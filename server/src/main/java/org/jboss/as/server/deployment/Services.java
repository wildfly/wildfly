/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment;

import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Services {

    private Services() {}

    /**
     * The base name for deployment services.
     */
    public static final ServiceName JBOSS_DEPLOYMENT = ServiceName.JBOSS.append("deployment");
    /**
     * The base name for deployment services.
     */
    public static final ServiceName JBOSS_DEPLOYMENT_POLICY = JBOSS_DEPLOYMENT.append("policy");
    /**
     * The base name for deployment unit services and phase services.
     */
    public static final ServiceName JBOSS_DEPLOYMENT_UNIT = JBOSS_DEPLOYMENT.append("unit");
    /**
     * The base name for sub-deployment unit services and phase services.
     */
    public static final ServiceName JBOSS_DEPLOYMENT_SUB_UNIT = JBOSS_DEPLOYMENT.append("subunit");
    /**
     * The service name of the deployment chains service.
     */
    public static final ServiceName JBOSS_DEPLOYMENT_CHAINS = JBOSS_DEPLOYMENT.append("chains");
    /**
     * The service name of the deployment extension index service.
     */
    public static final ServiceName JBOSS_DEPLOYMENT_EXTENSION_INDEX = JBOSS_DEPLOYMENT.append("extension-index");

    /**
     * Get the service name of a top-level deployment unit.
     *
     * @param name the simple name of the deployment
     * @return the service name
     */
    public static ServiceName deploymentUnitName(String name) {
        return JBOSS_DEPLOYMENT_UNIT.append(name);
    }

    /**
     * Get the service name of a deployment policy.
     *
     * @param name the simple name of the deployment policy
     * @return the service name
     */
    public static ServiceName deploymentPolicyName(String policy) {
        return (policy != null) ? JBOSS_DEPLOYMENT_POLICY.append(policy) : JBOSS_DEPLOYMENT_POLICY;
    }

    /**
     * Get the service name of a subdeployment.
     *
     * @param parent the parent deployment name
     * @param name the subdeployment name
     * @return the service name
     */
    public static ServiceName deploymentUnitName(String parent, String name) {
        return JBOSS_DEPLOYMENT_SUB_UNIT.append(parent, name);
    }

    /**
     * Get the service name of a top-level deployment unit.
     *
     * @param name the simple name of the deployment
     * @param phase the deployment phase
     * @return the service name
     */
    public static ServiceName deploymentUnitName(String name, Phase phase) {
        return JBOSS_DEPLOYMENT_UNIT.append(name, phase.name());
    }

    /**
     * Get the service name of a subdeployment.
     *
     * @param parent the parent deployment name
     * @param name the subdeployment name
     * @param phase the deployment phase
     * @return the service name
     */
    public static ServiceName deploymentUnitName(String parent, String name, Phase phase) {
        return JBOSS_DEPLOYMENT_SUB_UNIT.append(parent, name, phase.name());
    }
}
