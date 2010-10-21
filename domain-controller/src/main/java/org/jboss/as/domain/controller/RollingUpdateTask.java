package org.jboss.as.domain.controller;

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
