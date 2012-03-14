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

package org.jboss.as.txn;

import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

/**
 * Date: 16.05.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface TransactionMessages {
    /**
     * The messages
     */
    TransactionMessages MESSAGES = Messages.getBundle(TransactionMessages.class);

    /**
     * Creates an exception indicating a create failed.
     *
     * @param cause the reason the creation failed.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause.
     */
    @Message(id = 10100, value = "Create failed")
    StartException createFailed(@Cause Throwable cause);

    /**
     * Creates an exception indicating the start of a manager failed.
     *
     * @param cause       the reason the start failed.
     * @param managerName the name of the manager that didn't start.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause and error message.
     */
    @Message(id = 10101, value = "%s manager create failed")
    StartException managerStartFailure(@Cause Throwable cause, String managerName);

    /**
     * Creates an exception indicating the failure of the object store browser.
     *
     * @param cause the reason the start failed.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause and error message.
     */
    @Message(id = 10102, value = "Failed to configure object store browser bean")
    StartException objectStoreStartFailure(@Cause Throwable cause);


    /**
     * Creates an exception indicating that a service was not started.
     *
     * @return a {@link IllegalStateException} initialized with the cause and error message.
     */
    @Message(id = 10103, value = "Service not started")
    IllegalStateException serviceNotStarted();

    /**
     * Creates an exception indicating the start failed.
     *
     * @param cause the reason the start failed.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause.
     */
    @Message(id = 10104, value = "Start failed")
    StartException startFailure(@Cause Throwable cause);

    /**
     * A message indicating the metric is unknown.
     *
     * @param metric the unknown metric.
     *
     * @return the message.
     */
    @Message(id = 10105, value = "Unknown metric %s")
    String unknownMetric(Object metric);

    @Message(id = 10106, value = "MBean Server service not installed, this functionality is not available if the JMX subsystem has not been installed.")
    RuntimeException jmxSubsystemNotInstalled();


}
