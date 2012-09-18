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

package org.jboss.as.jsr77;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * This module is using message IDs in the range 19000-19999
 * This file is using the subset 19950-19999 JSR-77 logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * @author Kabir Khan
 */
@MessageLogger(projectCode = "JBAS")
public interface JSR77Logger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    JSR77Logger ROOT_LOGGER = Logger.getMessageLogger(JSR77Logger.class, JSR77Logger.class.getPackage().getName());

}
