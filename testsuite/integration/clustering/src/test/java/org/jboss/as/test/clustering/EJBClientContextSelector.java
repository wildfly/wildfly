/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.ejb.client.EJBClientContext;

/**
 * @author Paul Ferraro
 */
public class EJBClientContextSelector {
    public static EJBClientContext setup(String file) throws IOException {
        return setup(file, null);
    }

    public static EJBClientContext setup(String file, Properties propertiesToReplace) throws IOException {
        // setUp the selector
        final InputStream inputStream = EJBClientContextSelector.class.getClassLoader().getResourceAsStream(file);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + file + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);

        // add or replace properties passed from file
        if (propertiesToReplace != null) {
            properties.putAll(propertiesToReplace);
        }

        // TODO Elytron: Once support for legacy EJB properties has been added back, actually set the EJB properties
        // that should be used for this test using properties
        return null;
    }
}
