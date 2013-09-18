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

package org.jboss.as.naming.util;

import java.util.Enumeration;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * @author baranowb
 *
 */
public class RmiNameParser implements NameParser {

    private static final String REPLACE_TO = "//";
    private static final String REPLACE_WITH = "\\\\/\\\\/";
    private static final Properties SYNTAX;
    static {
        SYNTAX = new Properties();
        SYNTAX.put("jndi.syntax.direction", "left_to_right");
        SYNTAX.put("jndi.syntax.ignorecase", "false");
        SYNTAX.put("jndi.syntax.separator", "/");
        SYNTAX.put("jndi.syntax.escape", "\\");
    }

    @Override
    public Name parse(String name) throws NamingException {
        name = name.replaceFirst(REPLACE_TO, REPLACE_WITH);
        CompoundName tempName = new CompoundName(name, SYNTAX);
        return new FlyName(tempName.getAll());
    }

    private class FlyName extends CompositeName {

        private static final long serialVersionUID = -1460841804550306348L;

        public FlyName(Enumeration<String> comps) {
            super(comps);
        }
    }
}
