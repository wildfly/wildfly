/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.timerservice.spi;

import java.lang.reflect.Method;

import org.jboss.as.ejb3.timerservice.TimerImpl;

/**
 * An implementation can invoke the ejbTimeout method on a TimedObject.
 * <p/>
 * The TimedObjectInvoker has knowledge of the TimedObjectId, it
 * knows which object to invoke.
 *
 * @author Thomas.Diesler@jboss.org
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface TimedObjectInvoker {
    /**
     * The globally unique identifier for this timed object invoker.
     *
     * @return the identifier
     */
    String getTimedObjectId();

    /**
     * Invokes the ejbTimeout method on the TimedObject with the given id.
     *
     * @param timer the Timer that is passed to ejbTimeout
     */
    void callTimeout(TimerImpl timer) throws Exception;

    /**
     * Responsible for invoking the timeout method on the target object.
     * <p/>
     * <p>
     * The timerservice implementation invokes this method as a callback when a timeout occurs for the passed
     * <code>timer</code>. The timerservice implementation will be responsible for passing the correct
     * timeout method corresponding to the <code>timer</code> on which the timeout has occurred.
     * </p>
     *
     * @param timer         the Timer that is passed to ejbTimeout
     * @param timeoutMethod The timeout method
     */
    void callTimeout(TimerImpl timer, Method timeoutMethod) throws Exception;

    /**
     * @return The class loader that should be used to load restore any timers for this object
     */
    ClassLoader getClassLoader();

}
