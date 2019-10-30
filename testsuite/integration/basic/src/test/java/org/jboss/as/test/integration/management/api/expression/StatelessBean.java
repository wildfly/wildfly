/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.expression;

import javax.ejb.Stateless;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;

@Stateless
public class StatelessBean {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    private ModelControllerClient getClient() {
        return TestSuiteEnvironment.getModelControllerClient();
    }


    public String getJBossProperty(String name) {
        ModelControllerClient client = getClient();
        String result = Utils.getProperty(name, client);
        log.debug("JBoss system property " + name + " was resolved to be " + result);
        return result;
    }

    public void addSystemProperty(String name, String value) {
        System.setProperty(name, value);

    }

    public String getSystemProperty(String name) {
        String result = System.getProperty(name);
        log.debug("System property " + name + " has value " + result);
        return result;
    }

}
