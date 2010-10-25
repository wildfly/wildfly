/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.model.UpdateFailedException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractLoggerAdd extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 1370469831899844699L;

    private String levelName;

    protected AbstractLoggerAdd() {
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(final String levelName) {
        this.levelName = levelName;
    }

    protected abstract String getLoggerName();

    /**
     * {@inheritDoc}
     */
    public LoggerRemove getCompensatingUpdate(LoggingSubsystemElement original) {
        return new LoggerRemove(getLoggerName());
    }

    protected void applyUpdate(final LoggingSubsystemElement element) throws UpdateFailedException {
        AbstractLoggerElement<?> loggerElement = addNewElement(element);
        loggerElement.setLevel(levelName);
    }

    protected abstract AbstractLoggerElement<?> addNewElement(final LoggingSubsystemElement element) throws UpdateFailedException;
}
