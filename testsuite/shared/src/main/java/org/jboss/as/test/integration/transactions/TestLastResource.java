/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.transactions;

import org.jboss.tm.LastResource;

/**
 * Test {@link LastResource} class which causes that <code>XAOnePhaseResource</code>
 * will be instantiated at <code>TransactionImple#createRecord</code>.<br>
 * The information about {@link LastResource} is taken from definition
 * <code>jtaEnvironmentBean.setLastResourceOptimisationInterfaceClassName</code>
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class TestLastResource extends TestXAResource implements LastResource {

    public TestLastResource(TransactionCheckerSingleton checker) {
        super(checker);
    }

    public TestLastResource(TestAction testAction, TransactionCheckerSingleton checker) {
        super(testAction, checker);
    }

}
