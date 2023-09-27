/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.audit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Consumer;

import org.wildfly.security.auth.server.event.SecurityEvent;

public class CustomSecurityEventListener implements Consumer<SecurityEvent> {

    private static final String NAME = CustomSecurityEventListener.class.getSimpleName();
    private static final String AUDIT_LOG_NAME = NAME + ".log";
    private static final File AUDIT_LOG_FILE = new File("target", AUDIT_LOG_NAME);

    private String testingAttribute = null;

    @Override
    public void accept(SecurityEvent securityEvent) {
        try (PrintWriter writer = new PrintWriter(AUDIT_LOG_FILE)) {
            writer.print(testingAttribute + ":" + securityEvent.getClass().getName());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize(Map<String, String> configuration) {
        testingAttribute = configuration.get("testingAttribute");
    }

}
