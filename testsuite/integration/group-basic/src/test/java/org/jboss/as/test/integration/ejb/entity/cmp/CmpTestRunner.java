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

import static org.junit.Assert.fail;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * @author John Bailey
 */
public class CmpTestRunner extends Arquillian {

    public CmpTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        final AbstractCmpTest cmpTest = AbstractCmpTest.class.cast(test);
        return super.methodInvoker(new FrameworkMethod(method.getMethod()) {
            public Object invokeExplosively(final Object target, final Object... params) throws Throwable {

                getTransactionWrapper(cmpTest).invokeWithTransaction(new TransactionWrappingSessionBean.Task<Void>() {
                    public Void execute() throws Throwable {
                        cmpTest.setUpEjb();
                        return null;
                    }
                });
                Object result = getTransactionWrapper(cmpTest).invokeWithTransaction(new TransactionWrappingSessionBean.Task<Object>() {
                    public Object execute() throws Throwable {
                        return method.invokeExplosively(target, params);
                    }
                });

                getTransactionWrapper(cmpTest).invokeWithTransaction(new TransactionWrappingSessionBean.Task<Void>() {
                    public Void execute() throws Throwable {
                        cmpTest.tearDownEjb();
                        return null;
                    }
                });
                return result;
            }
        }, test);
    }

    public void run(RunNotifier notifier) {
        super.run(notifier);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private TransactionWrappingSessionBean getTransactionWrapper(final AbstractCmpTest target) {
        try {
            return (TransactionWrappingSessionBean) target.getInitialContext().lookup("java:module/TransactionWrappingSessionBean!org.jboss.as.test.integration.ejb.entity.cmp.TransactionWrappingSessionBean");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getTransactionWrapper: " + e.getMessage());
        }
        return null;
    }
}
