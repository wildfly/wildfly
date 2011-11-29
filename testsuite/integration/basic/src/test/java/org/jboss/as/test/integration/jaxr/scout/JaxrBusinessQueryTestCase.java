/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jaxr.scout;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.jaxr.scout.SaajTransport;
import org.jboss.as.test.integration.jaxr.scout.JaxrTestBase;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.registry.BulkResponse;
import javax.xml.registry.JAXRException;
import javax.xml.registry.JAXRResponse;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.Organization;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/** Tests Jaxr capability to do business queries
 *  @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 *  @since Dec 29, 2004
 */
@RunWith(Arquillian.class)
public class JaxrBusinessQueryTestCase extends JaxrTestBase {

    private String querystr = "JBOSS";
    private Key orgKey = null;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        String keyid = "";
        login();
        try {
            getJAXREssentials();
            Collection orgs = new ArrayList();
            Organization org = createOrganization("JBOSS");

            orgs.add(org);
            BulkResponse br = blm.saveOrganizations(orgs);
            if (br.getStatus() == JAXRResponse.STATUS_SUCCESS) {
                Collection coll = br.getCollection();
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    Key key = (Key) iter.next();
                    keyid = key.getId();
                    Assert.assertNotNull(keyid);
                    orgKey = key;
                }//end while
            } else {
                Collection exceptions = br.getExceptions();
                if (exceptions != null) {
                    Iterator iter = exceptions.iterator();
                    while (iter.hasNext())
                    {
                        Exception e = (Exception) iter.next();
                        e.printStackTrace(System.err);
                    }
                }
                Assert.fail("Cannot save Organizations");
            }
        } catch (JAXRException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (orgKey != null)
            this.deleteOrganization(this.orgKey);
        super.tearDown();
    }

    @Test
    public void testBusinessQuery() throws JAXRException {
        searchBusiness(querystr);
    }
}
