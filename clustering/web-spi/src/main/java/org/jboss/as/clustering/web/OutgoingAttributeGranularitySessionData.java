/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.clustering.web;

import java.util.Map;
import java.util.Set;

/**
 * Expands on {@link OutgoingDistributableSessionData} to expose information about changes in the session's attribute map.
 * @author Brian Stansberry
 */
public interface OutgoingAttributeGranularitySessionData extends OutgoingDistributableSessionData {
    /**
     * Gets those key/value pairs in the session's attribute map that have been modified versus what is already present in the
     * distributed cache, including newly added key/value pairs.
     * @return map containing the modified attributes map or <code>null</code> if there are no modified attributes.
     */
    Map<String, Object> getModifiedSessionAttributes();

    /**
     * Gets the names of session attributes that have been removed locally and thus need to be removed from the distributed cache.
     * @return a set of attribute keys that need to be removed from the distributed cache, or <code>null</code> if there are no such keys
     */
    Set<String> getRemovedSessionAttributes();
}
