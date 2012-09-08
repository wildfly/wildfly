/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.logmanager;

import java.util.Map;

import org.jboss.logmanager.config.NamedConfigurable;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class AbstractBasicConfiguration<T, C extends AbstractBasicConfiguration<T, C>> implements NamedConfigurable {

    private final LogContextConfigurationImpl configuration;
    private final String name;
    private boolean removed;
    protected final Map<String, T> refs;
    protected final Map<String, C> configs;

    AbstractBasicConfiguration(final String name, final LogContextConfigurationImpl configuration, final Map<String, T> refs, final Map<String, C> configs) {
        this.name = name;
        this.configuration = configuration;
        this.refs = refs;
        this.configs = configs;
    }

    public String getName() {
        return name;
    }

    void clearRemoved() {
        removed = false;
    }

    void setRemoved() {
        removed = true;
    }

    boolean isRemoved() {
        return removed;
    }

    LogContextConfigurationImpl getConfiguration() {
        return configuration;
    }

    ConfigAction<Void> getRemoveAction() {
        return new ConfigAction<Void>() {
            public Void validate() throws IllegalArgumentException {
                return null;
            }

            public void applyPreCreate(final Void param) {
                refs.remove(name);
            }

            public void applyPostCreate(final Void param) {
            }

            @SuppressWarnings({ "unchecked" })
            public void rollback() {
                configs.put(name, (C) AbstractBasicConfiguration.this);
                clearRemoved();
            }
        };
    }

    Map<String, T> getRefs() {
        return refs;
    }

    Map<String, C> getConfigs() {
        return configs;
    }
}
