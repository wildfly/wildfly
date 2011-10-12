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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * TODO: delete this
     *
     * Resource injections that only get installed if a binding is set up
     * See EE 5.4.1.3
     */
    private final Map<String, List<LazyResourceInjection>> lazyResourceInjections = Collections.synchronizedMap(new HashMap<String, List<LazyResourceInjection>>());

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

    public void addLazyResourceInjection(LazyResourceInjection injection) {
        //TODO: lazy binding and comp/module aliasing is not really compatible
        String name = injection.getLocalContextName();
        //we store all the bindings as absolute bindings
        if (!name.startsWith("java:")) {
            //there is the potential for both java:comp and java:module bindings to satisfy these injections
            List<LazyResourceInjection> list = lazyResourceInjections.get("java:comp/env/" + name);
            if (list == null) {
                lazyResourceInjections.put("java:comp/env/" + name, list = new ArrayList<LazyResourceInjection>(1));
            }
            list.add(injection);
            list = lazyResourceInjections.get("java:module/env/" + name);
            if (list == null) {
                lazyResourceInjections.put("java:module/env/" + name, list = new ArrayList<LazyResourceInjection>(1));
            }
            list.add(injection);
        } else {
            List<LazyResourceInjection> list = lazyResourceInjections.get(name);
            if (list == null) {
                lazyResourceInjections.put(name, list = new ArrayList<LazyResourceInjection>(1));
            }
            list.add(injection);
        }
    }

    public Map<String, List<LazyResourceInjection>> getLazyResourceInjections() {

        return lazyResourceInjections;
    }

}
