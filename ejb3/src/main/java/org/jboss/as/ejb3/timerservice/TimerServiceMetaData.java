/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.timerservice;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Encapsulates timer service meta data for an EJB component.
 * @author Stuart Douglas
 * @author Paul Ferraro
 */
public class TimerServiceMetaData extends AbstractEJBBoundMetaData {
    private static final long serialVersionUID = 8290412083429128705L;

    private String dataStoreName;
    private String persistentProvider;
    private String transientProvider;

    public String getDataStoreName() {
        return dataStoreName;
    }

    public void setDataStoreName(final String dataStoreName) {
        this.dataStoreName = dataStoreName;
    }

    public String getPersistentTimerManagementProvider() {
        return this.persistentProvider;
    }

    public void setPersistentTimerManagementProvider(String persistentProvider) {
        this.persistentProvider = persistentProvider;
    }

    public String getTransientTimerManagementProvider() {
        return this.transientProvider;
    }

    public void setTransientTimerManagementProvider(String transientProvider) {
        this.transientProvider = transientProvider;
    }

    @Override
    public String toString() {
        return String.format("data-store=%s, persistent-provider=%s, transient-provider=%s", this.dataStoreName, this.persistentProvider, this.transientProvider);
    }
}
