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
package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AdditionalInitializationUtil {

    private AdditionalInitializationUtil() {
    }

    public static ProcessType getProcessType(AdditionalInitialization additionalInit) {
        return additionalInit.getProcessType();
    }

    public static RunningMode getRunningMode(AdditionalInitialization additionalInit) {
        return additionalInit.getRunningMode();
    }

    public static void initializeModel(AdditionalInitialization additionalInit, Resource rootResource, ManagementResourceRegistration rootRegistration) {
        additionalInit.createControllerInitializer().initializeModel(rootResource, rootRegistration);
    }

    public static void doExtraInitialization(AdditionalInitialization additionalInit, ControllerInitializer controllerInitializer, ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration) {
        //TODO
        //controllerInitializer.setTestModelControllerService(this);
        controllerInitializer.initializeModel(rootResource, rootRegistration);
        additionalInit.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration);
    }
}
