/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.ejb.Singleton;

/**
 * @author baranowb
 *
 */
@Singleton
public class OverlayEJB implements OverlayableInterface {

    @Override
    public String fetchResource() throws Exception {
        return fetch(RESOURCE);
    }

    @Override
    public String fetchResourceStatic() throws Exception {
        return fetch(RESOURCE_STATIC);
    }

    protected String fetch(final String res) throws Exception {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(res)) {
            if (is == null) {
                return null;
            }
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr);) {
                return br.readLine();
            }
        }
    }
}
