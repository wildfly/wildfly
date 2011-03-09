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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class EEApplicationDescription {
    private final Map<String, Set<AbstractComponentDescription>> componentsByViewName = new HashMap<String, Set<AbstractComponentDescription>>();

    /**
     * Add a component to this application.
     *
     * @param description the component description
     */
    public void addComponent(AbstractComponentDescription description) {
        for(String viewName : description.getViewClassNames()) {
            Set<AbstractComponentDescription> viewComponents = componentsByViewName.get(viewName);
            if(viewComponents == null) {
                viewComponents = new HashSet<AbstractComponentDescription>();
                componentsByViewName.put(viewName, viewComponents);
            }
            viewComponents.add(description);
        }
    }

    public Set<AbstractComponentDescription> getComponentsForViewName(final String name) {
        return componentsByViewName.get(name);
    }
}
