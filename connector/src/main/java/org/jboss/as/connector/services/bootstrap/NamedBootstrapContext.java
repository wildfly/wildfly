/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.bootstrap;

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
    public NamedBootstrapContext(final String name) {
        super();
        setName(name);
    }

    public NamedBootstrapContext(final String name, final String workManagerName) {
        this(name);
        this.setWorkManagerName(workManagerName);
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
    public void setName(String v) {
        name = v;
    }

    @Override
    public CloneableBootstrapContext clone() throws CloneNotSupportedException {
        NamedBootstrapContext nbc = (NamedBootstrapContext)super.clone();
        nbc.setName(getName());
        nbc.setWorkManagerName(getWorkManagerName());
        return nbc;
    }
}
