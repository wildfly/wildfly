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

package org.jboss.as.jpa.interceptor;

import java.util.Map;
import java.util.Set;

import org.jboss.as.jpa.container.NonTxEmCloser;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.ServiceName;

/**
 * Web setup action that closes the entity managers created during the servlet invocation.
 * This provides a thread local collection of all created transactional entity managers (created without a
 * transaction).
 *
 * @author Scott Marlow
 */
public class WebNonTxEmCloserAction implements SetupAction {

    @Override
    public void setup(final Map<String, Object> properties) {
        NonTxEmCloser.pushCall();       // create a thread local place to hold created transactional entity managers
    }

    @Override
    public void teardown(final Map<String, Object> properties) {
        NonTxEmCloser.popCall();    // close any transactional entity managers that were created without a jta transaction.
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Set<ServiceName> dependencies() {
        return null;
    }
}
