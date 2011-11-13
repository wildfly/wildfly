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

package org.jboss.as.controller.client.helpers.domain;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.Serializable;

/**
 * Indicates how the actions in a {@link DeploymentSetPlan} are to be
 * applied to a particular server group.
 *
 * @author Brian Stansberry
 */
public class ServerGroupDeploymentPlan implements Serializable {

    private static final long serialVersionUID = 4868990805217024722L;

    private final String serverGroupName;
    private final boolean rollback;
    private final boolean rollingToServers;
    private final int maxFailures;
    private final int maxFailurePercentage;

    public ServerGroupDeploymentPlan(final String serverGroupName) {
        this(serverGroupName, false, false, 0, 0);
    }

    private ServerGroupDeploymentPlan(final String serverGroupName, final boolean rollback, final boolean rollingToServers, final int maxFailures, final int maxFailurePercentage) {
        if (serverGroupName == null) {
            throw MESSAGES.nullVar("serverGroupName");
        }
        this.serverGroupName = serverGroupName;
        this.rollback = rollback;
        this.rollingToServers = rollingToServers;
        this.maxFailures = maxFailures;
        this.maxFailurePercentage = maxFailurePercentage;
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

    public boolean isRollback() {
        return rollback;
    }

    public boolean isRollingToServers() {
        return rollingToServers;
    }

    public int getMaxServerFailures() {
        return maxFailures;
    }

    public int getMaxServerFailurePercentage() {
        return maxFailurePercentage;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ServerGroupDeploymentPlan
                && ((ServerGroupDeploymentPlan) obj).serverGroupName.equals(serverGroupName));
    }

    @Override
    public int hashCode() {
        return serverGroupName.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
            .append("{serverGroupName=")
            .append(serverGroupName)
            .append(",rollback=")
            .append(rollback)
            .append(",rollingToServers=")
            .append(rollingToServers)
            .append("}")
            .toString();
    }

    public ServerGroupDeploymentPlan createRollback() {
        return new ServerGroupDeploymentPlan(serverGroupName, true, rollingToServers, maxFailures, maxFailurePercentage);
    }

    public ServerGroupDeploymentPlan createRollingToServers() {
        return new ServerGroupDeploymentPlan(serverGroupName, rollback, true, maxFailures, maxFailurePercentage);
    }

    public ServerGroupDeploymentPlan createAllowFailures(int serverFailures) {
        if (serverFailures < 1)
            throw MESSAGES.invalidValue("serverFailures", serverFailures, 0);
        return new ServerGroupDeploymentPlan(serverGroupName, true, rollingToServers, serverFailures, maxFailurePercentage);
    }

    public ServerGroupDeploymentPlan createAllowFailurePercentage(int serverFailurePercentage) {
        if (serverFailurePercentage < 1 || serverFailurePercentage > 99)
            throw MESSAGES.invalidValue("serverFailurePercentage", serverFailurePercentage, 0, 100);
        return new ServerGroupDeploymentPlan(serverGroupName, true, rollingToServers, maxFailures, serverFailurePercentage);
    }

}
