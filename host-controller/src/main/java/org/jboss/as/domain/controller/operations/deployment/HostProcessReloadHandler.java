/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller.operations.deployment;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.ProcessReloadHandler;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.RestartMode;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HostProcessReloadHandler extends ProcessReloadHandler<HostRunningModeControl>{

    public HostProcessReloadHandler(ServiceName rootService, HostRunningModeControl runningModeControl,
            ResourceDescriptionResolver resourceDescriptionResolver) {
        // FIXME HostProcessReloadHandler constructor
        super(rootService, runningModeControl, resourceDescriptionResolver);
    }

    protected boolean isIncludeRestartServers() {
        return true;
    }

    @Override
    protected void reloadInitiated(HostRunningModeControl runningModeControl, final boolean adminOnly, final boolean restartServers) {
        runningModeControl.setRestartMode(restartServers ? RestartMode.SERVERS : RestartMode.HC_ONLY);
    }

    @Override
    protected void doReload(final HostRunningModeControl runningModeControl, final boolean adminOnly, final boolean restartServers) {
        runningModeControl.setRunningMode(adminOnly ? RunningMode.ADMIN_ONLY : RunningMode.NORMAL);
        runningModeControl.setRestartMode(restartServers ? RestartMode.SERVERS : RestartMode.HC_ONLY);
    }
}
