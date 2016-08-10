/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.deploymentoverlay.jar;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ejb.Singleton;

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
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr);) {
                return br.readLine();
            }
        }
    }
}
