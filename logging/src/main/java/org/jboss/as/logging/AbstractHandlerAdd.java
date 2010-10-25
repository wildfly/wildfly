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

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateFailedException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractHandlerAdd extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 5791037187352350769L;

    private final String name;

    private String levelName;

    private Boolean autoflush;

    private String encoding;

    private AbstractFormatterSpec formatter;

    private String[] subhandlers;

    protected AbstractHandlerAdd(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public AbstractSubsystemUpdate<LoggingSubsystemElement, ?> getCompensatingUpdate(LoggingSubsystemElement original) {
        return new HandlerRemove(name);
    }

    /**
     * {@inheritDoc}
     */
    protected void applyUpdate(LoggingSubsystemElement element) throws UpdateFailedException {
        final AbstractHandlerElement<?> handler = createElement(name);
        handler.setLevelName(levelName);
        handler.setAutoflush(autoflush);
        handler.setEncoding(encoding);
        formatter.apply(handler);
        if (subhandlers != null) handler.setSubhandlers(subhandlers);
        element.addHandler(handler.getName(), handler);
    }

    protected abstract AbstractHandlerElement<?> createElement(String name);

    public void setLevelName(final String levelName) {
        this.levelName = levelName;
    }

    public void setAutoflush(final Boolean autoflush) {
        this.autoflush = autoflush;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public void setFormatter(final AbstractFormatterSpec formatter) {
        this.formatter = formatter;
    }

    public void setSubhandlers(final String... subhandlers) {
        this.subhandlers = subhandlers;
    }

    public String[] getSubhandlers() {
        return subhandlers;
    }

    public AbstractFormatterSpec getFormatter() {
        return formatter;
    }

    public String getEncoding() {
        return encoding;
    }

    public Boolean getAutoflush() {
        return autoflush;
    }

    public String getLevelName() {
        return levelName;
    }

    public String getName() {
        return name;
    }
}
