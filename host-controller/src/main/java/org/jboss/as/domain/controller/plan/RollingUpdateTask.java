/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.controller.plan;

import java.util.List;

/** A task that iterates through other tasks */
class RollingUpdateTask implements Runnable {

    private final List<Runnable> rollingTasks;

    RollingUpdateTask(final List<Runnable> rollingTasks) {
        this.rollingTasks = rollingTasks;
    }

    @Override
    public void run() {
        for (Runnable r : rollingTasks) {
            r.run();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RollingUpdateTask{tasks={");
        for (int i = 0; i < rollingTasks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(rollingTasks.get(i).toString());
        }
        sb.append("}}");
        return sb.toString();
    }
}
