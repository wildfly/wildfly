/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

public class ServerLogTailerListener implements TailerListener {
    public final List<String> logs = new CopyOnWriteArrayList<>();

    @Override
    public void fileNotFound() {
        throw new IllegalStateException("Server log file was not found");
    }

    @Override
    public void fileRotated() {
        logs.clear();
    }

    @Override
    public void handle(Exception exception) {
        throw new IllegalStateException(exception);
    }

    @Override
    public void handle(String line) {
        logs.add(line);
    }

    @Override
    public void init(Tailer tailer) {
    }
}
