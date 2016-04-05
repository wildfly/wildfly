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
package org.wildfly.clustering.ejb;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.Batcher;

/**
 * A SPI for managing beans.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean instance type
 */
public interface BeanManager<I, T, B extends Batch> extends AffinitySupport<I>, BeanManagerStatistics {
    Bean<I, T> createBean(I id, I group, T bean);
    Bean<I, T> findBean(I id);

    boolean containsBean(I id);

    IdentifierFactory<I> getIdentifierFactory();

    Batcher<B> getBatcher();

    void start();
    void stop();

    boolean isRemotable(Throwable throwable);
}
