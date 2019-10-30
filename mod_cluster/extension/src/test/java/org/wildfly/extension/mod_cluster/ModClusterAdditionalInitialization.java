/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import java.io.Serializable;

import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.LegacyKernelServicesInitializer;

/**
 * Static simplistic {@link AdditionalInitialization} implementation which registers undertow capabilities and socket bindings.
 * To avoid class loading problems ensure that this is a standalone {@link Serializable} class and {@link LegacyKernelServicesInitializer#addSingleChildFirstClass(java.lang.Class[])} is called.
 * Also, this class is only usable on capability-enabled containers (EAP 7.0 and newer).
 *
 * @author Radoslav Husar
 */
public class ModClusterAdditionalInitialization extends AdditionalInitialization implements Serializable {

    private static final long serialVersionUID = 776664289594803865L;

    @Override
    protected RunningMode getRunningMode() {
        return RunningMode.ADMIN_ONLY;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
        registerCapabilities(capabilityRegistry, "org.wildfly.undertow.listener.ajp");
        registerCapabilities(capabilityRegistry, "org.wildfly.undertow.listener.default");
    }

    @Override
    protected void setupController(ControllerInitializer controllerInitializer) {
        super.setupController(controllerInitializer);

        controllerInitializer.addSocketBinding("modcluster", 0); // "224.0.1.105", "23364"
        controllerInitializer.addRemoteOutboundSocketBinding("proxy1", "localhost", 6666);
        controllerInitializer.addRemoteOutboundSocketBinding("proxy2", "localhost", 6766);
        controllerInitializer.addRemoteOutboundSocketBinding("proxy3", "localhost", 6866);
    }

}