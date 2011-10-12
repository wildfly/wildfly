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
