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

import java.util.Arrays;
import java.util.List;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamException;

import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractLoggerElement<E extends AbstractLoggerElement<E>> extends AbstractModelElement<E> {

    private static final long serialVersionUID = -4071924125278221764L;

    private static final String[] NONE = new String[0];

    private String[] handlers = NONE;

    private Level level;
    private FilterElement filter;

    AbstractLoggerElement() {
    }

    void setLevel(final Level level) {
        this.level = level;
    }

    void setFilter(final FilterElement filter) {
        this.filter = filter;
    }

    void setHandlers(final List<String> handlers) {
        this.handlers = handlers.toArray(new String[handlers.size()]);
    }

    public List<String> getHandlers() {
        return Arrays.asList(handlers);
    }

    public void writeContent(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if (level != null) {
            streamWriter.writeEmptyElement("level");
        }
        if (filter != null) {
            streamWriter.writeStartElement("filter");
            filter.writeContent(streamWriter);
        }
    }

    public Level getLevel() {
        return level;
    }

    public FilterElement getFilter() {
        return filter;
    }
}
