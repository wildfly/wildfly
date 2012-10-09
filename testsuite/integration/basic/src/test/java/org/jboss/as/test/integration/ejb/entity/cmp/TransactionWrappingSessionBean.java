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

package org.jboss.as.test.integration.ejb.entity.cmp;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.Status;

/**
 * @author John Bailey
 */
@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class TransactionWrappingSessionBean {

    @Resource
    private EJBContext ejbContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <T> T invokeWithTransaction(final Task<T> task) {
        try {
            ejbContext.getUserTransaction().begin();
            try {
                return task.execute();
            } catch (Exception e) {
                throw new EJBException(e);
            } finally {
                if (ejbContext.getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                    ejbContext.getUserTransaction().commit();
                }
            }
        } catch (Throwable t) {
            try {
                ejbContext.getUserTransaction().rollback();
            } catch (Throwable unused) {
            }
            if(t instanceof EJBException) {
                throw (EJBException)t;
            }
            throw new RuntimeException(t);
        }
    }

    public interface Task<T> {
        T execute() throws Throwable;
    }
}
