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
package org.wildfly.clustering.ejb.bean;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.ee.Restartable;
import org.wildfly.clustering.ejb.remote.AffinitySupport;

/**
 * A SPI for managing beans.
 *
 * @author Paul Ferraro
 *
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <B> the batch type
 */
public interface BeanManager<K, V extends BeanInstance<K>, B extends Batch> extends Restartable, AffinitySupport<K>, BeanStatistics {
    Bean<K, V> createBean(V instance, K groupId);
    Bean<K, V> findBean(K id) throws TimeoutException;

    Supplier<K> getIdentifierFactory();

    Batcher<B> getBatcher();

    boolean isRemotable(Throwable throwable);
}
