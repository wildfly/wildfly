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

package org.jboss.as.test.integration.jca.datasource.remove;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the removal of a data-source used in a web deployment.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(AbstractDsRemove.DataSourceSetupTask.class)
public class WebAppDsRemoveTestCase extends AbstractDsRemove {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "remove-ds.war");
        war.addClasses(RemoveDsServlet.class, TimeoutUtil.class);
        return war;
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testDatasourceRemove() throws Exception {
        final String resultBeforeRemoval = performCall(url, RemoveDsServlet.SERVLET_NAME);
        assertEquals("ok", resultBeforeRemoval);
        try {
            remove(datasourceAddress());
            fail("Removal of the datasource should have failed.");
        } catch (MgmtOperationException expected) {
        }
        final String resultAfterRemoval = performCall(url, RemoveDsServlet.SERVLET_NAME);
        assertEquals("ok", resultAfterRemoval);
    }

}
