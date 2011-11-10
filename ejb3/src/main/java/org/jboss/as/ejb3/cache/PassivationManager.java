/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.cache;

import org.jboss.marshalling.MarshallingConfiguration;

/**
 * Manages passivation and replication lifecycle callbacks on an object.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @author Paul Ferraro
 */
public interface PassivationManager<K, V extends Identifiable<K>> {
    /**
     * This method is called after an object has been retrieved deserialized after passivation.
     * @param obj the object
     */
    void postActivate(V obj);

    /**
     * This method is called before an object is serialized for passivation.
     * @param obj the object
     * @throws IllegalStateException if <code>obj</code>, or another object in the same serialization group as <code>obj</code>,
     *         is in use. Checking if an object is in use and throwing this exception is not required, so callers should not
     *         assume it will be thrown.
     */
    void prePassivate(V obj);

    MarshallingConfiguration getMarshallingConfiguration();
    /**
     * This method is called after a previously replicated object has been retrieved from a clustered cache.
     * @param obj the object.
     */
//    void postReplicate(V obj);

    /**
     * This method is called before an object is replicated by a clustered cache.
     * @param obj the object
     * @throws IllegalStateException if <code>obj</code>, or another object in the same serialization group as <code>obj</code>,
     *         is in use. Checking if an object is in use and throwing this exception is not required, so callers should not
     *         assume it will be thrown.
     */
//    void preReplicate(V obj);
}
