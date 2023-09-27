/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.component;

/**
 * The jndi names for the default EE bindings.
 * @author Eduardo Martins
 */
public class EEDefaultResourceJndiNames {

    private volatile String contextService;
    private volatile String dataSource;
    private volatile String jmsConnectionFactory;
    private volatile String managedExecutorService;
    private volatile String managedScheduledExecutorService;
    private volatile String managedThreadFactory;

    public String getContextService() {
        return contextService;
    }

    public void setContextService(String contextService) {
        this.contextService = contextService;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getJmsConnectionFactory() {
        return jmsConnectionFactory;
    }

    public void setJmsConnectionFactory(String jmsConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
    }

    public String getManagedExecutorService() {
        return managedExecutorService;
    }

    public void setManagedExecutorService(String managedExecutorService) {
        this.managedExecutorService = managedExecutorService;
    }

    public String getManagedScheduledExecutorService() {
        return managedScheduledExecutorService;
    }

    public void setManagedScheduledExecutorService(String managedScheduledExecutorService) {
        this.managedScheduledExecutorService = managedScheduledExecutorService;
    }

    public String getManagedThreadFactory() {
        return managedThreadFactory;
    }

    public void setManagedThreadFactory(String managedThreadFactory) {
        this.managedThreadFactory = managedThreadFactory;
    }
}
