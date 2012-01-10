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

package org.jboss.as.weld;

import java.lang.reflect.Member;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Date: 05.11.2011
 *
 *
 * 16000-16050
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface WeldLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    WeldLogger ROOT_LOGGER = Logger.getMessageLogger(WeldLogger.class, "org.jboss.as.weld");

    /**
     * A logger with the category {@code org.jboss.weld}.
     */
    WeldLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(WeldLogger.class, "org.jboss.weld.deployer");


    @LogMessage(level= Logger.Level.ERROR)
    @Message(value = "Failed to setup Weld contexts", id = 16000)
    void failedToSetupWeldContexts(@Cause Throwable throwable);

    @LogMessage(level= Logger.Level.ERROR)
    @Message(value = "Failed to tear down Weld contexts", id = 16001)
    void failedToTearDownWeldContexts(@Cause Throwable throwable);

    @LogMessage(level= Logger.Level.INFO)
    @Message(value = "Processing weld deployment %s", id = 16002)
    void processingWeldDeployment(String deployment);

    @LogMessage(level = Logger.Level.WARN)
    @Message(value = "Found beans.xml file in non-standard location: %s, war deployments should place beans.xml files into WEB-INF/beans.xml", id = 16003)
    void beansXmlInNonStandardLocation(String location);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(value = "Could not find BeanManager for deployment %s", id = 16004)
    void couldNotFindBeanManagerForDeployment(String beanManager);

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Starting Services for CDI deployment: %s", id = 16005)
    void startingServicesForCDIDeployment(String deploymentName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(value = "Could not load portable extension class %s", id = 16006)
    void couldNotLoadPortableExceptionClass(String className, @Cause Throwable throwable);

    @LogMessage(level = Logger.Level.WARN)
    @Message(value = "@Resource injection of type %s is not supported for non-ejb components. Injection point: %s", id = 16007)
    void injectionTypeNotValue(Class<?> type, Member injectionPoint);

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Starting weld service for deployment %s", id = 16008)
    void startingWeldService(String deploymentName);

    @LogMessage(level = Logger.Level.INFO)
    @Message(value = "Stopping weld service for deployment %s", id = 16009)
    void stoppingWeldService(String deploymentName);
}
