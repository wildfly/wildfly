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

package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.List;

/**
 * An element representing a list of jvm options.
 *
 * @author <a href="mailto:kkhan@redhat.com">Kabir Khan</a>
 */
public final class JvmOptionsElement {

    private static final long serialVersionUID = 1614693052895734582L;

    private final List<String> options = new ArrayList<String>();

    /**
     * Construct a new instance.
     *
     */
    public JvmOptionsElement() {
    }

    public JvmOptionsElement(final Element propertyType, boolean allowNullValue, JvmOptionsElement ... toCombine) {
        if (toCombine != null) {
            for (JvmOptionsElement pe : toCombine) {
                if (pe == null)
                    continue;
                for (String value : pe.getOptions()) {
                    addOption(value);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public long elementHash() {
        synchronized (options) {
            return options.hashCode();
        }
    }

    /**
     * Adds an option to the Jvm options
     *
     * @param value the option to add
     */
    void addOption(final String value) {
        synchronized (options) {
            if (value == null) {
                throw MESSAGES.nullVar("value");
            }
            options.add(value);
        }
    }

    public int size() {
        return options.size();
    }

    /**
     * Get a copy of the options.
     *
     * @return the copy of the options
     */
    public List<String> getOptions() {
        return new ArrayList<String>(options);
    }

}
