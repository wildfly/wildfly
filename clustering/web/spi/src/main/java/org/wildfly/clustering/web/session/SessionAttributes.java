/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.session;

/**
 * Exposes accesses to the attributes of a session.
 * @author Paul Ferraro
 */
public interface SessionAttributes extends ImmutableSessionAttributes {
    /**
     * Removes the specified attribute.
     * @param name a unique attribute name
     * @return the removed attribute value, or null if the attribute does not exist.
     */
    Object removeAttribute(String name);

    /**
     * Sets the specified attribute to the specified value.
     * @param name a unique attribute name
     * @param value the attribute value
     * @return the old attribute value, or null if the attribute did not previously exist.
     */
    Object setAttribute(String name, Object value);
}
