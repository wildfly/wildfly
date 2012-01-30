/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.client.selector;

import org.jboss.ejb.client.ConstantContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@LocalBean
public class EJBClientContextSelectorChangingBean {

    private static final Logger logger = Logger.getLogger(EJBClientContextSelectorChangingBean.class);

    public void changeLockedSelector() {
        final EJBClientContext dummyContext = EJBClientContext.create();
        // try setting a selector
        this.callSetSelector(dummyContext);
        // now try the other API
        this.callSetConstantContext(dummyContext);

    }

    private void callSetSelector(final EJBClientContext clientContext) {
        try {
            EJBClientContext.setSelector(new ConstantContextSelector<EJBClientContext>(clientContext));
        } catch (SecurityException se) {
            // expected
            logger.info("Got the expected " + se + " while trying to call EJBClientContext.setSelector() on the server, from an EJB");
            return;
        }
        throw new RuntimeException("EJBClientContext.setSelector() was expected to fail on server side");
    }

    private void callSetConstantContext(final EJBClientContext clientContext) {
        try {
            EJBClientContext.setConstantContext(clientContext);
        } catch (SecurityException se) {
            // expected
            logger.info("Got the expected " + se + " while try to call EJBClientContext.setConstantContext() on the server, from an EJB");
            return;
        }
        throw new RuntimeException("EJBClientContext.setConstantContext() was expected to fail on server side");
    }

}
