/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Function;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.jipijapa.plugin.spi.ScopedStatelessSessionInvocationHandler;
import org.jipijapa.plugin.spi.ScopedStatelessSessionSupplier;
import org.jipijapa.plugin.spi.StatelessSessionFactory;
import org.kohsuke.MetaInfServices;

@MetaInfServices(StatelessSessionFactory.class)
public class HibernateStatelessSessionFactory implements StatelessSessionFactory<StatelessSession> {

    @Override
    public StatelessSession getTransactionScopedSession(Function<Function<EntityManagerFactory, AutoCloseable>, ScopedStatelessSessionSupplier> supplierFactory) {
        ScopedStatelessSessionSupplier sessionSupplier = supplierFactory.apply(new SerializableFunction());
        return ScopedStatelessSessionInvocationHandler.createStatelessSessionProxy(
                    StatelessSession.class,
                    getClass().getClassLoader(),
                    sessionSupplier);
    }

    /**
     * This function may get serialized with the ScopedStatelessSessionInvocationHandler that indirectly
     * references it, so we want it to be serializable as well.
     */
    private static class SerializableFunction implements Function<EntityManagerFactory, AutoCloseable>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public AutoCloseable apply(EntityManagerFactory entityManagerFactory) {
            return entityManagerFactory.unwrap(SessionFactory.class).openStatelessSession();
        }
    }
}
