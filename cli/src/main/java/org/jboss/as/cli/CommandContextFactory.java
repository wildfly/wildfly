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
package org.jboss.as.cli;

import java.io.InputStream;
import java.io.OutputStream;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CommandContextFactory {

    private static final String DEFAULT_FACTORY_CLASS = "org.jboss.as.cli.impl.CommandContextFactoryImpl";

    public static CommandContextFactory getInstance() throws CliInitializationException {
        Class<?> factoryCls;
        try {
            factoryCls = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged().loadClass(DEFAULT_FACTORY_CLASS);
        } catch (ClassNotFoundException e) {
            throw new CliInitializationException("Failed to load " + DEFAULT_FACTORY_CLASS, e);
        }

        try {
            return (CommandContextFactory) factoryCls.newInstance();
        } catch (Exception e) {
            throw new CliInitializationException("Failed to create an instance of " + factoryCls, e);
        }
    }

    protected CommandContextFactory() {}

    public abstract CommandContext newCommandContext() throws CliInitializationException;

    public abstract CommandContext newCommandContext(String username, char[] password) throws CliInitializationException;

    public abstract CommandContext newCommandContext(String controller, String username, char[] password) throws CliInitializationException;

    public abstract CommandContext newCommandContext(String controller, String username, char[] password, boolean initConsole,
            final int connectionTimeout) throws CliInitializationException;

    public abstract CommandContext newCommandContext(String controller, String username, char[] password, boolean disableLocalAuth,
            boolean initConsole, final int connectionTimeout) throws CliInitializationException;

    public abstract CommandContext newCommandContext(String controller, String username, char[] password, InputStream consoleInput,
            OutputStream consoleOutput) throws CliInitializationException;

    /**
     * @deprecated Use {@link #newCommandContext(String, String, char[])} instead.
     */
    @Deprecated
    public abstract CommandContext newCommandContext(String controllerHost, int controllerPort,
            String username, char[] password) throws CliInitializationException;

    /**
     * @deprecated Use {@link #newCommandContext(String, int, String, char[], boolean, int)} instead.
     */
    @Deprecated
    public abstract CommandContext newCommandContext(String controllerHost, int controllerPort,
            String username, char[] password, boolean initConsole, final int connectionTimeout) throws CliInitializationException;

    /**
     * @deprecated Use {@link #newCommandContext(String, int, String, char[], InputStream, OutputStream)} instead.
     */
    @Deprecated
    public abstract CommandContext newCommandContext(String controllerHost, int controllerPort,
            String username, char[] password,
            InputStream consoleInput, OutputStream consoleOutput) throws CliInitializationException;

}
