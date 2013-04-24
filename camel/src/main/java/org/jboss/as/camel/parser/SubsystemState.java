/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.camel.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * The Camel subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public final class SubsystemState  {

    private Map<String, String> contextDefinitions = new HashMap<String,String>();

    public Set<String> getContextDefinitionNames() {
        synchronized (contextDefinitions) {
            return contextDefinitions.keySet();
        }
    }

    public String getContextDefinition(String name) {
        synchronized (contextDefinitions) {
            return contextDefinitions.get(name);
        }
    }

    public void putContextDefinition(String name, String contextDefinition) {
        synchronized (contextDefinitions) {
            contextDefinitions.put(name, contextDefinition);
        }
    }

    public String removeContextDefinition(String name) {
        synchronized (contextDefinitions) {
            return contextDefinitions.remove(name);
        }
    }

}
