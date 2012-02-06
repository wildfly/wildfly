/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2011, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.jaxr;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.DEBUG;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 14000-14099. This file is using the subset 14000-14079 for host
 * controller logger messages. See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved
 * JBAS message id blocks.
 *
 * Date: 31.1.2012
 *
 * @author Kurt T Stam>
 */
@MessageLogger(projectCode = "JBAS")
public interface JAXRLogger extends BasicLogger {

    /**
     * Default root level logger with the package name for he category.
     */
    JAXRLogger ROOT_LOGGER = Logger.getMessageLogger(JAXRLogger.class, JAXRLogger.class.getPackage().getName());

    JAXRLogger JAXR_LOGGER = Logger.getMessageLogger(JAXRLogger.class, "org.jboss.jaxr");

    /**
     * Logs an info message indicating that the jaxr implementation is bound into JNDI.
     *
     * @param jndiName    the JNDI name of the JAXR ConnectionFactory.
     */
    @LogMessage(level = INFO)
    @Message(id = 14000, value = "Started JAXR subsystem, binding JAXR connection factory into JNDI as: %s")
    void bindingJAXRConnectionFactory(Object jndiName);

    /**
     * Logs an error message indicating that the jaxr implementation cannot be bound into JNDI.
     *
     * @param jndi-name    the JNDI name of the JAXR ConnectionFactory.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14001, value = "Cannot bind JAXR ConnectionFactory")
    void bindingJAXRConnectionFactoryFailed();

    /**
     * Logs an info message indicating that the jaxr implementation is unbound from JNDI.
     *
     * @param jndiName    the JNDI name of the JAXR ConnectionFactory.
     */
    @LogMessage(level = INFO)
    @Message(id = 14002, value = "UnBinding JAXR ConnectionFactory: %s")
    void unBindingJAXRConnectionFactory(Object jndiName);

    /**
     * Logs an error message indicating that the jaxr implementation cannot be unbound from JNDI.
     *
     * @param jndi-name    the JNDI name of the JAXR ConnectionFactory.
     */
    @LogMessage(level = ERROR)
    @Message(id = 14003, value = "Cannot unbind JAXR ConnectionFactory")
    void unBindingJAXRConnectionFactoryFailed();

    /**
     * Obtained JAXR factory class name using a System Property.
     *
     * @param key    the name of the JAXR ConnectionFactory implementation property.
     * @param value  the value of the JAXR ConnectionFactory implementation property.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 14004, value = "Obtained the JAXR factory name from System Property %s, using jaxr implementation %s")
    void factoryNameFromSystemProperty(String key, String value);

    /**
     * Obtained JAXR factory class name using a JBoss Configuration.
     *
     * @param key    the name of the JAXR ConnectionFactory implementation property.
     * @param value  the value of the JAXR ConnectionFactory implementation property.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 14005, value = "Obtained the JAXR factory name from JBoss configuration %s, using jaxr implementation %s")
    void factoryNameFromJBossConfig(String key, String value);

    /**
     * Obtained JAXR factory class name using the ServiceLoader API.
     *
     * @param factoryClassName    the JAXR ConnectionFactory implementation class.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 14006, value = "Obtained the JAXR factory name from the ServiceLoader API reading file META-INF/services/javax.xml.registry.ConnectionFactory, using jaxr implementation %s")
    void factoryNameFromServiceLoader(String factoryClassName);

    /**
     * Obtained JAXR factory class name using the Default Scout implementation.
     *
     * @param factoryClassName    the JAXR ConnectionFactory implementation class.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 14007, value = "Using default JAXR factory implementation %s")
    void factoryNameFromDefault(String factoryClassName);
}
