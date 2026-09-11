/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.observability.micrometer.addremove;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;


class ServerLogTailerListener implements TailerListener {
    final List<String> logs = new ArrayList<>();

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
