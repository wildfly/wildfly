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

package org.jboss.as.test.integration.ejb.transaction.exception.bean.ejb3;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.as.test.integration.ejb.transaction.exception.TestConfig.TxManagerException;
import org.jboss.as.test.integration.ejb.transaction.exception.bean.TestBean;

@LocalBean
@Remote
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BmtEjb3 implements TestBean {

    @Resource
    private UserTransaction utx;

    @Override
    public void throwRuntimeException() {
        try {
            utx.begin();
            throw new RuntimeException();
        } catch (NotSupportedException | SystemException e) {
            throw new IllegalStateException("Can't begin transaction!", e);
        }
    }

    @Override
    public void throwExceptionFromTm(TxManagerException txManagerException) throws Exception {
        throw new UnsupportedOperationException();
    }

}
