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
package org.wildfly.ee.concurrent;

import javax.enterprise.concurrent.ManagedTask;
import java.util.Map;

/**
 * A {@link ManagedTask} configuration object.
 *
 * @author Eduardo Martins
 */
public class ManagedTaskConfiguration {

    private final ManagedTask managedTask;
    private final String identityName;
    private final boolean contextualCallbacks;

    /**
     * @param managedTask
     */
    public ManagedTaskConfiguration(ManagedTask managedTask) {
        this.managedTask = managedTask;
        final Map<String, String> executionProperties = managedTask.getExecutionProperties();
        // task identity name
        String propertiesIdentityName = null;
        if (executionProperties != null) {
            propertiesIdentityName = executionProperties.get("javax.enterprise.ee.IDENTITY_NAME");
        }
        if (propertiesIdentityName == null) {
            identityName = managedTask.toString();
        } else {
            identityName = propertiesIdentityName;
        }
        // contextual callbacks
        boolean contextualCallbacks = false;
        if (executionProperties != null) {
            final String contextualCallbackHintProperty = executionProperties.get("javax.enterprise.ee.CONTEXTUAL_CALLBACK_HINT");
            if (contextualCallbackHintProperty != null) {
                contextualCallbacks = Boolean.valueOf(contextualCallbackHintProperty);
            }
        }
        this.contextualCallbacks = contextualCallbacks;
    }

    /**
     * @return
     */
    public boolean isManagedTaskWithContextualCallbacks() {
        return contextualCallbacks;
    }

    /**
     * @return
     */
    public String getIdentityName() {
        return identityName;
    }

    /**
     * @return
     */
    public ManagedTask getManagedTask() {
        return managedTask;
    }
}
