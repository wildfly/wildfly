/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jpa.hibernate5;


import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.engine.transaction.jta.platform.internal.JtaSynchronizationStrategy;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryAccess;
import org.hibernate.engine.transaction.jta.platform.internal.SynchronizationRegistryBasedSynchronizationStrategy;
import org.jipijapa.plugin.spi.JtaManager;


/**
 * @author Steve Ebersole
 */
public class JBossAppServerJtaPlatform extends  org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform {

    private final JtaSynchronizationStrategy synchronizationStrategy;

    protected JtaManager getJtaManager() {
        return jtaManager;
    }

    private final JtaManager jtaManager;

    public JBossAppServerJtaPlatform(final JtaManager jtaManager) {
        this.jtaManager = jtaManager;
        this.synchronizationStrategy = new SynchronizationRegistryBasedSynchronizationStrategy(new SynchronizationRegistryAccess() {
            @Override
            public TransactionSynchronizationRegistry getSynchronizationRegistry() {
                return jtaManager.getSynchronizationRegistry();
            }
        });
    }

    @Override
    protected boolean canCacheTransactionManager() {
        return true;
    }

    @Override
    protected TransactionManager locateTransactionManager() {
        return jtaManager.locateTransactionManager();
    }

    @Override
    protected JtaSynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }
}
