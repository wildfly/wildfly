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
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractHandlerAdd extends AbstractLoggingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 5791037187352350769L;

    private final String name;

    private Level level;
    private Boolean autoflush;
    private String encoding;
    private AbstractFormatterSpec formatter;
    private String[] subhandlers;

    protected AbstractHandlerAdd(final String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {

    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<LoggingSubsystemElement, ?> getCompensatingUpdate(LoggingSubsystemElement original) {
        return new HandlerRemoveUpdate(name);
    }

    /** {@inheritDoc} */
    protected void applyUpdate(LoggingSubsystemElement element) throws UpdateFailedException {
        final AbstractHandlerElement<?> handler = createHandler(name);
        handler.setLevel(level);
        handler.setAutoflush(autoflush);
        handler.setEncoding(encoding);
        formatter.apply(handler);
        handler.setSubhandlers(subhandlers);
        element.addHandler(handler.getName(), handler);
    }

    protected abstract AbstractHandlerElement<?> createHandler(String name);

    public void setLevel(final Level level) {
        this.level = level;
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

    public void setSubhandlers(final String[] subhandlers) {
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

    public Level getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }
}
