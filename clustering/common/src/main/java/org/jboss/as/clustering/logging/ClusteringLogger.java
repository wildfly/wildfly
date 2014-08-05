/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLCOM", length = 4)
public interface ClusteringLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = "org.jboss.as.clustering";

    /**
     * The root logger.
     */
    ClusteringLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringLogger.class, ROOT_LOGGER_CATEGORY);

    @Message(id = 1, value = "%2$g is not a valid value for parameter %1$s. The value must be %3$s %4$g")
    OperationFailedException parameterValueOutOfBounds(String name, double value, String relationalOperator, double bound);
}
