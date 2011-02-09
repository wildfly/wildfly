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

package org.jboss.as.domain.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Emanuel Muckenhuber
 */
public final class DomainControllerService implements Service<DomainController> {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final ExtensibleConfigurationPersister configurationPersister;
    private DomainController controller;

    public DomainControllerService(final ExtensibleConfigurationPersister configurationPersister) {
        this.configurationPersister = configurationPersister;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(final StartContext context) throws StartException {
        log.info("Starting Domain Controller");
        final DomainController controller = new DomainControllerImpl(configurationPersister);
        final List<ModelNode> updates;
        try {
             updates = configurationPersister.load();
        } catch (final Exception e) {
            log.error("failed to start domain controller", e);
            throw new StartException(e);
        }

        final AtomicInteger count = new AtomicInteger(1);
        final ResultHandler resultHandler = new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode result) {
            }

            @Override
            public void handleResultComplete(final ModelNode compensatingOperation) {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }

            @Override
            public void handleCancellation() {
                if (count.decrementAndGet() == 0) {
                    // some action
                }
            }
        };
        for (ModelNode update : updates) {
            count.incrementAndGet();
            controller.execute(update, resultHandler);
        }
        if (count.decrementAndGet() == 0) {
            // some action?
        }
        this.controller = controller;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(final StopContext context) {
        this.controller = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized DomainController getValue() throws IllegalStateException, IllegalArgumentException {
        final DomainController controller = this.controller;
        if(controller == null) {
            throw new IllegalStateException();
        }
        return controller;
    }

}
