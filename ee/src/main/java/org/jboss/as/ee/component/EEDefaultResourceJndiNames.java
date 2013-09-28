/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
