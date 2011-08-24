/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2010, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.context.spi;

import java.util.Map;

/**
 * Extend the default EJBContext interface with the ability to
 * query container provided runtime context associated to the EJB.
 *
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface EJBContext extends javax.ejb.EJBContext {
    /**
     * @since 3.1
     */
    Map<String, Object> getContextData();

    /**
     * @return the runtime context for this EJB
     */
    EJBComponent getComponent();

    /**
     * Returns the bean instance associated with this context. This must be
     * the same one which has the context injected.
     *
     * @return the bean instance associated with this context
     */
    Object getTarget();
}
