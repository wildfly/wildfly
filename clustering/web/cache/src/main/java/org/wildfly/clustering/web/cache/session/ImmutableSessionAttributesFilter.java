/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionAttributesFilter implements SessionAttributesFilter {

    private final ImmutableSessionAttributes attributes;

    public ImmutableSessionAttributesFilter(ImmutableSession session) {
        this(session.getAttributes());
    }

    public ImmutableSessionAttributesFilter(ImmutableSessionAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public <T> Map<String, T> getAttributes(Class<T> targetClass) {
        Set<String> names = this.attributes.getAttributeNames();
        if (names.isEmpty()) return Collections.emptyMap();
        Map<String, T> result = new HashMap<>(names.size());
        for (String name : names) {
            Object attribute = this.attributes.getAttribute(name);
            if (targetClass.isInstance(attribute)) {
                result.put(name, targetClass.cast(attribute));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
