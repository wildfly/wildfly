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

package org.jboss.as.connector.bootstrap;

import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;

/**
 * A named bootstrap context.
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class NamedBootstrapContext extends BaseCloneableBootstrapContext {

    /** Default BootstrapContext name */
    public static final String DEFAULT_NAME = "default";

    /** The name of the BootstrapContext - unique container wide */
    private String name;

    /**
     * Constructor
     * @param name The name of the WorkManager
     */
    public NamedBootstrapContext(String name) {
        super();
        setName(name);
    }

    /**
     * Get the name
     * @return The value
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name
     * @param v The value
     */
    void setName(String v) {
        name = v;
    }

    @Override
    public CloneableBootstrapContext clone() throws CloneNotSupportedException {
        NamedBootstrapContext nbc = (NamedBootstrapContext)super.clone();
        nbc.setName(getName());

        return nbc;
    }
}
