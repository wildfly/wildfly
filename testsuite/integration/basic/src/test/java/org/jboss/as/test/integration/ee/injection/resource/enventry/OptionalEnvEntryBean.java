/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
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
