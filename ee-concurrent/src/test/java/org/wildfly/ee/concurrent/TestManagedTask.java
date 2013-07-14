/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.ee.concurrent;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Martins
 */
public class TestManagedTask extends TestTask implements ManagedTask {

    private final TestManagedTaskListener listener;
    String identityName = null;
    final boolean contextualListener;

    public TestManagedTask(int executions, int expectedCallbacks) {
        this(executions, expectedCallbacks, true);
    }

    public TestManagedTask(int executions, int expectedCallbacks, boolean contextualListener) {
        super(executions);
        listener = new TestManagedTaskListener(expectedCallbacks, contextualListener);
        this.contextualListener = contextualListener;
    }

    public TestManagedTaskListener getTestManagedTaskListener() {
        return listener;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        Map<String, String> executionProperties = new HashMap<>();
        executionProperties.put("javax.enterprise.ee.CONTEXTUAL_CALLBACK_HINT", Boolean.toString(contextualListener));
        if (identityName != null) {
            executionProperties.put("javax.enterprise.ee.IDENTITY_NAME", identityName);
        }
        return executionProperties;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return getTestManagedTaskListener();
    }

}
