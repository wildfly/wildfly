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
package org.jboss.as.cli.completion.mock;


import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.SSLConfig;

/**
 *
 * @author Alexey Loubyansky
 */
public class MockCliConfig implements CliConfig {

    @Override
    public SSLConfig getSslConfig() {
        return null;
    }

    @Override
    public String getDefaultControllerHost() {
        return "localhost";
    }

    @Override
    public int getDefaultControllerPort() {
        return 9999;
    }

    @Override
    public boolean isHistoryEnabled() {
        return false;
    }

    @Override
    public String getHistoryFileName() {
        return ".jboss-cli-history";
    }

    @Override
    public String getHistoryFileDir() {
        return null;
    }

    @Override
    public int getHistoryMaxSize() {
        return 500;
    }

    @Override
    public boolean isValidateOperationRequests() {
        return true;
    }
}
