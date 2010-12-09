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

package org.jboss.as.service.descriptor;

import java.io.Serializable;

/**
 * Configuration object for a JBoss service constructor.
 *
 * @author John E. Bailey
 */
public class JBossServiceConstructorConfig  implements Serializable {
    private static final long serialVersionUID = -4307592928958905408L;

    public static final Argument[] EMPTY_ARGS = {};
    private Argument[] arguments = EMPTY_ARGS;

    public Argument[] getArguments() {
        return arguments;
    }

    public void setArguments(Argument[] arguments) {
        this.arguments = arguments;
    }

    public static class Argument implements Serializable {
        private static final long serialVersionUID = 7644229980407045584L;

        private final String value;
        private final String type;

        public Argument(final String type, final String value) {
            this.value = value;
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
