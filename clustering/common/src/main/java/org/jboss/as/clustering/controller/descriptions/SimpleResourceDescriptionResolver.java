/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller.descriptions;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;

/**
 * Simple {@link org.jboss.as.controller.descriptions.ResourceDescriptionResolver} implementation
 * that uses a static name/description mapping.
 * @author Paul Ferraro
 */
public class SimpleResourceDescriptionResolver extends StandardResourceDescriptionResolver {
    final Map<String, String> descriptions = new HashMap<>();

    public SimpleResourceDescriptionResolver(String name, String description) {
        super(name, null, SimpleResourceDescriptionResolver.class.getClassLoader());
        this.descriptions.put(name, description);
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                return SimpleResourceDescriptionResolver.this.descriptions.get(key);
            }

            @Override
            protected Set<String> handleKeySet() {
                return SimpleResourceDescriptionResolver.this.descriptions.keySet();
            }

            @Override
            public Enumeration<String> getKeys() {
                return Collections.enumeration(this.handleKeySet());
            }
        };
    }

    public void addDescription(String key, String description) {
        this.descriptions.put(String.join(".", this.getKeyPrefix(), key), description);
    }
}
