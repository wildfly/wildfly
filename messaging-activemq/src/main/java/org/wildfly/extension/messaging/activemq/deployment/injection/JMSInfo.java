/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.deployment.injection;

import static javax.jms.JMSContext.AUTO_ACKNOWLEDGE;

import javax.jms.JMSConnectionFactory;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSSessionMode;

import org.jboss.metadata.property.PropertyReplacer;
import org.wildfly.extension.messaging.activemq.deployment.DefaultJMSConnectionFactoryBindingProcessor;

/**
 * Data structure containing the JMS information that can be annotated on an injected JMSContext.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */
class JMSInfo {

    private final String connectionFactoryLookup;
    private final String userName;
    private final String password;
    private final int sessionMode;

    JMSInfo(JMSConnectionFactory connectionFactory, JMSPasswordCredential credential, JMSSessionMode sessionMode) {
        PropertyReplacer propertyReplacer = JMSCDIExtension.propertyReplacer;
        if (connectionFactory != null) {
            connectionFactoryLookup = propertyReplacer.replaceProperties(connectionFactory.value());
        } else {
            connectionFactoryLookup = DefaultJMSConnectionFactoryBindingProcessor.COMP_DEFAULT_JMS_CONNECTION_FACTORY;
        }
        if (credential != null) {
            userName = propertyReplacer.replaceProperties(credential.userName());
            password = propertyReplacer.replaceProperties(credential.password());
        } else {
            userName = null;
            password = null;
        }
        if (sessionMode != null) {
            this.sessionMode = sessionMode.value();
        } else {
            this.sessionMode = AUTO_ACKNOWLEDGE;
        }
    }

    String getConnectionFactoryLookup() {
        return connectionFactoryLookup;
    }

    String getUserName() {
        return userName;
    }

    String getPassword() {
        return password;
    }

    int getSessionMode() {
        return sessionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JMSInfo jmsInfo = (JMSInfo) o;

        if (sessionMode != jmsInfo.sessionMode) return false;
        if (connectionFactoryLookup != null ? !connectionFactoryLookup.equals(jmsInfo.connectionFactoryLookup) : jmsInfo.connectionFactoryLookup != null)
            return false;
        if (password != null ? !password.equals(jmsInfo.password) : jmsInfo.password != null) return false;
        if (userName != null ? !userName.equals(jmsInfo.userName) : jmsInfo.userName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionFactoryLookup != null ? connectionFactoryLookup.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + sessionMode;
        return result;
    }
}
