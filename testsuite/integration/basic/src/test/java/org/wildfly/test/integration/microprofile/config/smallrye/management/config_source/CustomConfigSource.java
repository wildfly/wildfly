/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class CustomConfigSource implements ConfigSource{
    public static final String PROP_NAME = "my.prop.from.class";
    public static final String PROP_VALUE = "I'm from a custom config source!";
    public static final String PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER = "my.prop.from.class.overriden.service.loader";
    public static final String PROP_VALUE_OVERRIDEN_BY_SERVICE_LOADER = "I'm from a custom config source! However I should be " +
            "overriden by property from ConfigSource provided by service loader.";

    final Map<String, String> props;

    public CustomConfigSource() {
        props = new HashMap<>();
        props.put(PROP_NAME, PROP_VALUE);
        props.put(PROP_NAME_OVERRIDEN_BY_SERVICE_LOADER, PROP_VALUE_OVERRIDEN_BY_SERVICE_LOADER);
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
