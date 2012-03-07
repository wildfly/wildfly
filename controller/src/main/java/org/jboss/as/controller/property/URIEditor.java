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
package org.jboss.as.controller.property;

import java.net.URISyntaxException;

/**
 * A property editor for {@link java.net.URI}.
 *
 *
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 */
public class URIEditor extends TextPropertyEditorSupport {
    /**
     * Returns a URI for the input object converted to a string.
     *
     * @return a URI object
     *
     * @throws NestedRuntimeException An MalformedURLException occured.
     */
    public Object getValue() {
        try {
            return Strings.toURI(getAsText());
        } catch (URISyntaxException e) {
            throw new NestedRuntimeException(e);
        }
    }
}
