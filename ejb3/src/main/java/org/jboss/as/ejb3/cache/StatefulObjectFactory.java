/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.cache;

/**
 * Creates and destroys stateful objects.
 * <p/>
 * The object returned by create has dependencies injected. The PostConstruct
 * callback, if defined, has been called. It'll <b>not<b> call back on the appropriate
 * Init method.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface StatefulObjectFactory<T> {
    /**
     * Create a new instance of this component.  This may be invoked by a component interceptor, a client interceptor,
     * or in the course of creating a new client, or in the case of an "eager" singleton, at component start.  This
     * method will block until the component is available.  If the component fails to start then a runtime exception
     * will be thrown.
     * <p/>
     * The instance has been injected and post-construct has been called.
     *
     * @return the component instance
     */
    T createInstance();

    /**
     * Destroy an instance of the component.  This method causes all uninjection and pre-destroy lifecycle invocations
     * to occur.
     *
     * @param instance the instance to destroy
     */
    void destroyInstance(T instance);
}
