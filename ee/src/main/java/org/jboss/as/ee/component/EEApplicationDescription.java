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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author John Bailey
 */
public class EEApplicationDescription {
    private final Map<String, Set<ViewDescription>> componentsByViewName = new HashMap<String, Set<ViewDescription>>();

    /**
     * Add a component to this application.
     *
     * @param description the component description
     */
    public void addComponent(ComponentDescription description) {
        for (ViewDescription viewDescription : description.getViews()) {
            Set<ViewDescription> viewComponents = componentsByViewName.get(viewDescription.getViewClassName());
            if (viewComponents == null) {
                viewComponents = new HashSet<ViewDescription>();
                componentsByViewName.put(viewDescription.getViewClassName(), viewComponents);
            }
            viewComponents.add(viewDescription);
        }
    }

    public Set<ViewDescription> getComponentsForViewName(final String name) {
        final Set<ViewDescription> ret = componentsByViewName.get(name);
        return ret == null ? Collections.<ViewDescription>emptySet() : ret;
    }
}
