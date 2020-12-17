/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2018 Red Hat, Inc.
 */
public class CustomConfigSourceAServiceLoader implements ConfigSource {

    public static final String PROP_NAME_SAME_ORDINALITY_OVERRIDE = "my.prop.from.class.overriden.same.ordinality";
    public static final String PROP_VALUE_SAME_ORDINALITY_OVERRIDE = "I'm from a custom config source provided by service " +
            "loader overriding ConfigSource with same ordinality since my FQCN is ranked higher lexicographically.";


    final Map<String, String> props;

    public CustomConfigSourceAServiceLoader() {
        props = new HashMap<>();
        props.put(PROP_NAME_SAME_ORDINALITY_OVERRIDE, PROP_VALUE_SAME_ORDINALITY_OVERRIDE);
    }

    @Override
    public int getOrdinal() {
        return 101;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(props);
    }

    @Override
    public String getValue(String s) {
        return props.get(s);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }
}
