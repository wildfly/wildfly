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
package org.wildfly.clustering.ejb.infinispan;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Command that temporarily holds off the sheduling of a session, preventing
 * the cost of a cancellation. The scheduling must be canceled or rescheduled at
 * the near future.
 * @author Flavia Rainone
 */
public class PrepareReschedulingSchedulerCommand<I> implements Command<Void, Scheduler<I>> {
    private static final long serialVersionUID = 788517670339502640L;

    private final I beanId;

    public PrepareReschedulingSchedulerCommand(I beanId) {
        this.beanId = beanId;
    }

    @Override
    public Void execute(Scheduler<I> scheduler) {
        scheduler.prepareRescheduling(beanId);
        return null;

    }
}
