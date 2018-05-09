/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.service;

import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.common.function.ExceptionConsumer;

/**
 * Generic {@link Service} that provides no value.
 * @author Paul Ferraro
 * @deprecated Replaced by {@link FunctionalService}.
 */
@Deprecated
public class FunctionalVoidService implements Service<Void> {
    private static final Logger LOGGER = Logger.getLogger(FunctionalVoidService.class);

    private final ExceptionConsumer<StartContext, StartException> starter;
    private final Consumer<StopContext> stopper;

    /**
     * Create a new functional service.
     * @param starter
     * @param stopper
     */
    public FunctionalVoidService(ExceptionConsumer<StartContext, StartException> starter, Consumer<StopContext> stopper) {
        this.starter = starter;
        this.stopper = stopper;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (this.starter != null) {
            try {
                this.starter.accept(context);
            } catch (RuntimeException | Error e) {
                throw new StartException(e);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        if (this.stopper != null) {
            try {
                this.stopper.accept(context);
            } catch (RuntimeException | Error e) {
                LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public Void getValue() {
        return null;
    }
}
