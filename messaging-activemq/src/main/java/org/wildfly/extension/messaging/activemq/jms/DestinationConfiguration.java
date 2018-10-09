/*
 * Copyright 2018 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.jms;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class DestinationConfiguration {

    private final boolean durable;
    private final String selector;
    private final String name;
    private final String managementQueueAddress;
    private final String managementUsername;
    private final String managementPassword;
    private final String resourceAdapter;
    private final ServiceName destinationServiceName;

    public DestinationConfiguration(boolean durable, String selector, String name, String managementQueueAddress,
            String managementUsername, String managementPassword, String resourceAdapter, ServiceName destinationServiceName) {
        this.durable = durable;
        this.selector = selector;
        this.name = name;
        this.managementQueueAddress = managementQueueAddress;
        this.managementUsername = managementUsername;
        this.managementPassword = managementPassword;
        this.resourceAdapter = resourceAdapter;
        this.destinationServiceName = destinationServiceName;
    }

    public boolean isDurable() {
        return durable;
    }

    public String getSelector() {
        return selector;
    }

    public String getName() {
        return name;
    }

    public ServiceName getDestinationServiceName() {
        return destinationServiceName;
    }

    public String getResourceAdapter() {
        return resourceAdapter;
    }

    public Queue getManagementQueue() {
        return ActiveMQJMSClient.createQueue(this.managementQueueAddress);
    }

    public QueueConnection createQueueConnection(QueueConnectionFactory cf) throws JMSException {
        if(this.managementUsername != null && !this.managementUsername.isEmpty()) {
            return cf.createQueueConnection(managementUsername, managementPassword);
        }
        return cf.createQueueConnection();
    }

    public static class Builder {

        private boolean durable = false;
        private String selector = null;
        private String name;
        private String managementQueueAddress = "activemq.management";
        private String managementUsername = null;
        private String managementPassword = null;
        private String resourceAdapter;
        private ServiceName destinationServiceName;

        private Builder() {
        }

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder setDurable(boolean durable) {
            this.durable = durable;
            return this;
        }

        public Builder setSelector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDestinationServiceName(ServiceName destinationServiceName) {
            this.destinationServiceName = destinationServiceName;
            return this;
        }

        public Builder setResourceAdapter(String resourceAdapter) {
            this.resourceAdapter = resourceAdapter;
            return this;
        }

        public Builder setManagementQueueAddress(String managementQueueAddress) {
            this.managementQueueAddress = managementQueueAddress;
            return this;
        }

        public Builder setManagementUsername(String managementUsername) {
            this.managementUsername = managementUsername;
            return this;
        }

        public Builder setManagementPassword(String managementPassword) {
            this.managementPassword = managementPassword;
            return this;
        }

        public DestinationConfiguration build() {
            return new DestinationConfiguration(durable, selector, name, managementQueueAddress, managementUsername,
                    managementPassword,resourceAdapter, destinationServiceName);
        }

    }
}
