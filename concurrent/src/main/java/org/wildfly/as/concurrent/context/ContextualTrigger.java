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

package org.wildfly.as.concurrent.context;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;
import java.util.Date;

/**
 * A {@link Trigger} which invocations are done with a specific {@link Context} set.
 *
 * @author Eduardo Martins
 */
public class ContextualTrigger implements Trigger {

    private final Trigger trigger;
    private final Context context;

    /**
     * @param trigger
     * @param context
     */
    public ContextualTrigger(Trigger trigger, Context context) {
        this.trigger = trigger;
        this.context = context;
    }

    @Override
    public Date getNextRunTime(LastExecution lastExecutionInfo, Date taskScheduledTime) {
        final Context previousContext = Utils.setContext(context);
        try {
            return trigger.getNextRunTime(lastExecutionInfo, taskScheduledTime);
        } finally {
            Utils.setContext(previousContext);
        }
    }

    @Override
    public boolean skipRun(LastExecution lastExecutionInfo, Date scheduledRunTime) {
        final Context previousContext = Utils.setContext(context);
        try {
            return trigger.skipRun(lastExecutionInfo, scheduledRunTime);
        } finally {
            Utils.setContext(previousContext);
        }
    }
}
