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

package org.jboss.as.server.manager;

import java.io.Serializable;

/**
 * TODO:  Replace with a full SM->S protocol
 *
 * @author Brian Stansberry
 * @author John E. Bailey
 */
public class ServerMessage implements Serializable {
    private static final long serialVersionUID = 203020978147623518L;
    
    private static final Object[] NULL_ARGS = new Object[0];
    private static final Class<?>[] NULL_TYPES = new Class<?>[0];

    private final String message;
    private final Object[] args;
    private final Class<?>[] types;

    public ServerMessage(String message) {
        this(message, NULL_ARGS, NULL_TYPES);
    }

    public ServerMessage(String message, Object[] args, Class<?>[] types) {
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        this.message = message;
        if (args == null) {
            throw new IllegalArgumentException("args is null");
        }
        this.args = args;
        if (types == null) {
            throw new IllegalArgumentException("types is null");
        }
        if (args.length != types.length) {
            throw new IllegalStateException("Invalid number of types; expected " +
                    args.length + " but got " + types.length);
        }
        this.types = types;
    }

    public String getMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }

    public Class<?>[] getTypes() {
        return types;
    }
}
