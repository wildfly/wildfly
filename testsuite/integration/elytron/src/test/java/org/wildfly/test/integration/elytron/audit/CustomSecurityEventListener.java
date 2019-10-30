/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
