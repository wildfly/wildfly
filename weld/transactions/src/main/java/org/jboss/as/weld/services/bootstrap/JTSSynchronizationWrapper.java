/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.weld.services.bootstrap;

import javax.transaction.Synchronization;

/**
 *
 *  Stores NamespaceContextSelector during synchronization, and pushes it on top of the selector stack each time synchronization
 *  callback method is executed. This enables synchronization callbacks served by corba threads to work correctly.
 *
 *  @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

import org.jboss.as.naming.context.NamespaceContextSelector;

public class JTSSynchronizationWrapper implements Synchronization {

    private final Synchronization synchronization;
    private final NamespaceContextSelector selector;

    public JTSSynchronizationWrapper(final Synchronization synchronization) {
        this.synchronization = synchronization;
        selector = NamespaceContextSelector.getCurrentSelector();
    }

    @Override
    public void beforeCompletion() {
        try {
            NamespaceContextSelector.pushCurrentSelector(selector);
            synchronization.beforeCompletion();
        } finally {
            NamespaceContextSelector.popCurrentSelector();
        }
    }

    @Override
    public void afterCompletion(final int status) {
        try {
            NamespaceContextSelector.pushCurrentSelector(selector);
            synchronization.afterCompletion(status);

        } finally {
            NamespaceContextSelector.popCurrentSelector();
        }
    }

}
