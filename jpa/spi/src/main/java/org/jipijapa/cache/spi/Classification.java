/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jipijapa.cache.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Type of cache
 *
 * @author Scott Marlow
 */
public enum Classification {
    INFINISPAN("Infinispan"),
    NONE(null);

    private final String name;

    Classification(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Classification> MAP;

    static {
        final Map<String, Classification> map = new HashMap<String, Classification>();
        for (Classification element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Classification forName(String localName) {
        final Classification element = MAP.get(localName);
        return element == null ? NONE : element;
    }

}
