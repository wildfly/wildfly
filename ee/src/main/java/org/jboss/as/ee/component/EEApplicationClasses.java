/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import java.util.List;

/**
 * Allows a deployment to get old of class descriptions from all sub deployments it has access to.
 *
 * This maintains a list of all {@link EEModuleDescription}s that this sub deployment has access to,
 * in the same order they appear in the dependencies list.
 *
 * @author Stuart Douglas
 */
public final class EEApplicationClasses {

    //TODO: should we build a map of the available classes
    private final List<EEModuleDescription> availableModules;

    public EEApplicationClasses(final List<EEModuleDescription> availableModules) {
        this.availableModules = availableModules;
    }


    /**
     * Look for a class description in all available modules.
     * @param name The class to lookup
     * @return
     */
    public EEModuleClassDescription getClassByName(String name) {
        for(EEModuleDescription module : availableModules) {
            final EEModuleClassDescription desc = module.getClassDescription(name);
            if(desc != null) {
                return desc;
            }
        }
        return null;
    }
}
