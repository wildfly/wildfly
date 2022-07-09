/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.jakarta;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adapters for transaction exceptions.
 * @author Paul Ferraro
 */
public interface ExceptionAdapter {

    final Function<String, javax.transaction.HeuristicCommitException> HEURISTIC_COMMIT_EXCEPTION_FACTORY = javax.transaction.HeuristicCommitException::new;
    final Function<String, javax.transaction.HeuristicMixedException> HEURISTIC_MIXED_EXCEPTION_FACTORY = javax.transaction.HeuristicMixedException::new;
    final Function<String, javax.transaction.HeuristicRollbackException> HEURISTIC_ROLLBACK_EXCEPTION_FACTORY = javax.transaction.HeuristicRollbackException::new;
    final Function<String, javax.transaction.RollbackException> ROLLBACK_EXCEPTION_FACTORY = javax.transaction.RollbackException::new;

    static <S extends Throwable, T extends Throwable> T adapt(S source, Supplier<T> factory) {
        T result = factory.get();
        result.initCause(source.getCause());
        result.setStackTrace(source.getStackTrace());
        return result;
    }

    static <S extends Throwable, T extends Throwable> T adapt(S source, Function<String, T> factory) {
        return adapt(source, new Supplier<T>() {
            @Override
            public T get() {
                return factory.apply(source.getMessage());
            }
        });
    }

    static javax.transaction.HeuristicCommitException adapt(jakarta.transaction.HeuristicCommitException source) {
        return adapt(source, HEURISTIC_COMMIT_EXCEPTION_FACTORY);
    }

    static javax.transaction.HeuristicMixedException adapt(jakarta.transaction.HeuristicMixedException source) {
        return adapt(source, HEURISTIC_MIXED_EXCEPTION_FACTORY);
    }

    static javax.transaction.HeuristicRollbackException adapt(jakarta.transaction.HeuristicRollbackException source) {
        return adapt(source, HEURISTIC_ROLLBACK_EXCEPTION_FACTORY);
    }

    static javax.transaction.RollbackException adapt(jakarta.transaction.RollbackException source) {
        return adapt(source, ROLLBACK_EXCEPTION_FACTORY);
    }

    static javax.transaction.SystemException adapt(jakarta.transaction.SystemException source) {
        return adapt(source, new Supplier<javax.transaction.SystemException>() {
            @Override
            public javax.transaction.SystemException get() {
                return source.errorCode != 0 ? new javax.transaction.SystemException(source.errorCode) : new javax.transaction.SystemException(source.getMessage());
            }
        });
    }
}
