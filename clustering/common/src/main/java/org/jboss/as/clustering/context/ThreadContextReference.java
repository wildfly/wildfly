/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.context;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Reference that can be associated with an arbitrary thread.
 * @author Paul Ferraro
 */
public interface ThreadContextReference<C> extends ContextReference<C>, Function<Thread, C>, BiConsumer<Thread, C> {

    @Override
    default void accept(C context) {
        this.accept(Thread.currentThread(), context);
    }

    @Override
    default C get() {
        return this.apply(Thread.currentThread());
    }
}
