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
package org.jboss.ejb3.context.spi;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
import java.security.Principal;

/**
 * Allows an EJBContext to query for runtime context information
 * which is per EJB. This as opposed to of EJBContext which provides
 * information per EnterpriseBean instance or InvocationContext is
 * per invocation.
 * <p/>
 * Formerly known as Container.
 *
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface EJBComponent {
    /**
     * @see javax.ejb.EJBContext#getEJBHome()
     */
    EJBHome getEJBHome() throws IllegalStateException;

    /**
     * @see javax.ejb.EJBContext#getEJBLocalHome()
     */
    EJBLocalHome getEJBLocalHome() throws IllegalStateException;

    /**
     * TODO: should this really be per EJB or per invocation
     *
     * @see javax.ejb.EJBContext#getRollbackOnly()
     */
    boolean getRollbackOnly() throws IllegalStateException;

    /**
     * @see javax.ejb.EJBContext#getTimerService()
     */
    TimerService getTimerService() throws IllegalStateException;

    /**
     * @see javax.ejb.EJBContext#getUserTransaction()
     */
    UserTransaction getUserTransaction() throws IllegalStateException;

    /**
     * Allows an invocation to query the security service associated
     * with this EJB.
     *
     * @see javax.ejb.EJBContext#isCallerInRole(String)
     */
    boolean isCallerInRole(Principal callerPrincipal, String roleName) throws IllegalStateException;

    /**
     * @see javax.ejb.EJBContext#lookup(String)
     */
    Object lookup(String name) throws IllegalArgumentException;

    /**
     * @see javax.ejb.EJBContext#setRollbackOnly()
     */
    void setRollbackOnly() throws IllegalStateException;
}
