/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.jdbc.metadata;

import java.util.HashMap;

/**
 * This immutable class contains information about entity command
 *
 * @author <a href="mailto:loubyansky@ua.fm">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCEntityCommandMetaData {

    // Attributes -----------------------------------------------------

    /**
     * The name (alias) of the command.
     */
    private String commandName;

    /**
     * The class of the command
     */
    private Class<?> commandClass;

    /**
     * Command attributes
     */
    private final HashMap<String, String> attributes = new HashMap<String, String>();

    public JDBCEntityCommandMetaData() {
    }

    public JDBCEntityCommandMetaData(JDBCEntityCommandMetaData entityCommand, JDBCEntityCommandMetaData defaultValues) {
        // command name
        commandName = defaultValues.getCommandName();

        if (entityCommand.getCommandClass() != null) {
            commandClass = entityCommand.getCommandClass();
        } else {
            commandClass = defaultValues.getCommandClass();
        }

        // attributes
        attributes.putAll(defaultValues.attributes);
        attributes.putAll(entityCommand.attributes);
    }

    // Public ----------------------------------------------------------

    /**
     * @return the name of the command
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * @return the class of the command
     */
    public Class<?> getCommandClass() {
        return commandClass;
    }

    /**
     * @return value for the passed in parameter name
     */
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    // Object overrides --------------------------------------------------

    public String toString() {
        return new StringBuffer("[commandName=").append(commandName).
                append(",commandClass=").append(commandClass).
                append(",attributes=").append(attributes.toString()).
                append("]").toString();
    }

    public void setName(final String commandName) {
        this.commandName = commandName;
    }

    public void setClass(final Class<?> commandClass) {
        this.commandClass = commandClass;
    }

    public void addAttribute(final String name, final String value) {
        this.attributes.put(name, value);
    }
}
