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

package org.jboss.as.host.controller;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;

/**
 * Bootstrap of the HostController process.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HostControllerBootstrap {

    private final ServiceContainer serviceContainer = ServiceContainer.Factory.create("host-controller");
    private final HostControllerEnvironment environment;
    private final byte[] authCode;

    public HostControllerBootstrap(final HostControllerEnvironment environment, final byte[] authCode) {
        this.environment = environment;
        this.authCode = authCode;
    }

    /**
     * Start the host controller services.
     *
     * @throws Exception
     */
    public void bootstrap() throws Exception {

        final HostRunningModeControl runningModeControl = new HostRunningModeControl(environment.getInitialRunningMode(), RestartMode.SERVERS);
        final ControlledProcessState processState = new ControlledProcessState(false);
        ServiceTarget target = serviceContainer.subTarget();
        ControlledProcessStateService.addService(target, processState);
        final HostControllerService hcs = new HostControllerService(environment, runningModeControl, authCode, processState);
        target.addService(HostControllerService.HC_SERVICE_NAME, hcs).install();
    }

}
