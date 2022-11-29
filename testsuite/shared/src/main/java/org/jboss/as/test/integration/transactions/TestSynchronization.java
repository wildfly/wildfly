/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.transactions;

import javax.transaction.Synchronization;
import org.jboss.logging.Logger;

/**
 * Transaction {@link Synchronization} class for testing purposes.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class TestSynchronization implements Synchronization {
    private static final Logger log = Logger.getLogger(TestSynchronization.class);

    private TransactionCheckerSingletonRemote checker;

    public TestSynchronization(TransactionCheckerSingletonRemote checker) {
        this.checker = checker;
    }

    @Override
    public void beforeCompletion() {
        log.tracef("beforeCompletion called");
        checker.setSynchronizedBefore();
    }

    /**
     * For status see {@link javax.transaction.Status}.
     */
    @Override
    public void afterCompletion(int status) {
        log.tracef("afterCompletion called with status '%s'", status);
        boolean isCommitted = status == javax.transaction.Status.STATUS_COMMITTED ? true : false;
        checker.setSynchronizedAfter(isCommitted);
    }

}
