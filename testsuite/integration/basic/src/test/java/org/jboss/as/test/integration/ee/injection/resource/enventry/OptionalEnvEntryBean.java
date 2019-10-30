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

package org.jboss.as.test.integration.ee.injection.resource.enventry;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * Return the value of a buried env entry.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Stateless
@Remote(OptionalEnvEntry.class)
public class OptionalEnvEntryBean implements OptionalEnvEntry {
    // @Resource(name="entry")
    private Double entry = 1.1;

    public void checkLookup() {
        try {
            InitialContext ctx = new InitialContext();
            ctx.lookup("java:comp/env/entry");
            throw new RuntimeException("Should have thrown a NameNotFoundException");
        } catch (NameNotFoundException e) {
            // okay
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public Double getEntry() {
        return entry;
    }
}
