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

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.logging.Logger;

@Stateless
@Remote(IStatelessBean.class)
public class StatelessBean implements IStatelessBean {
	private static final Logger log = Logger.getLogger(StatelessBean.class);

	private ModelControllerClient getClient() {
	    ModelControllerClient client = ExpressionTestManagementService.getTestExpressionClient();
        assert(client != null); //client can't be null
        return client;
	}
	
	@Override
	public void addJBossProperty(String name, String value) {
	    ModelControllerClient client = getClient();
	    log.debugf("Adding jboss property %s with value %s via client %s", name, value, client);
	    Utils.setProperty(name, value, client);
	}
	
	@Override
	public void removeJBossProperty(String name) {
	    ModelControllerClient client = getClient();
	    log.debugf("Removing jboss property %s via client %s", name, client);
	    Utils.removeProperty(name, client);
	}
	
	@Override
	public String getJBossProperty(String name) {
		ModelControllerClient client = getClient();
		String result = Utils.getProperty(name, client);
		log.debug("JBoss system property " + name + " was resolved to be " + result);
		return result;
	}
	
	@Override
	public void redefineJBossProperty(String name, String value) {
	    ModelControllerClient client = getClient();
	    log.debugf("Redefine jboss property %s with value %s via client %s", name, value, client);
        Utils.redefineProperty(name, value, client);
	}

    @Override
    public void addSystemProperty(String name, String value) {
        System.setProperty(name, value);
        
    }

    @Override
    public void removeSystemProperty(String name) {
        System.clearProperty(name);
    }

    @Override
    public String getSystemProperty(String name) {
        String result = System.getProperty(name);
        log.debug("System property " + name + " has value " + result);
        return result;
    }

}
