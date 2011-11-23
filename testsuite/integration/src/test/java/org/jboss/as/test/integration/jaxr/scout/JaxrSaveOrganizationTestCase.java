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


import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.registry.BulkResponse;
import javax.xml.registry.JAXRException;
import javax.xml.registry.JAXRResponse;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.Organization;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Tests Jaxr Save Organization
 *
 * @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 * @author Thomas.Diesler@jboss.com
 * @since Dec 29, 2004
 */
@RunWith(Arquillian.class)
public class JaxrSaveOrganizationTestCase extends JaxrTestBase {

    private static Logger log = Logger.getLogger(JaxrSaveOrganizationTestCase.class);

    private Key orgKey = null;

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (this.orgKey != null)
            this.deleteOrganization(orgKey);
    }

    @Test
    public void testSaveOrg() throws JAXRException {
        String keyid = "";
        login();
        Organization org = null;
        try {
            rs = connection.getRegistryService();

            blm = rs.getBusinessLifeCycleManager();
            Collection orgs = new ArrayList();
            org = createOrganization("JBOSS");

            orgs.add(org);
            BulkResponse br = blm.saveOrganizations(orgs);
            if (br.getStatus() == JAXRResponse.STATUS_SUCCESS) {
                if ("true".equalsIgnoreCase(debugProp))
                    System.out.println("Organization Saved");
                Collection coll = br.getCollection();
                Iterator iter = coll.iterator();
                while (iter.hasNext()) {
                    Key key = (Key) iter.next();
                    keyid = key.getId();
                    if ("true".equalsIgnoreCase(debugProp))
                        System.out.println("Saved Key=" + key.getId());
                    Assert.assertNotNull(keyid);
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
            checkBusinessExists("JBOSS");
        } catch (JAXRException e) {
            log.error("Exception:", e);
            Assert.fail(e.getMessage());
        } finally {
            if (org != null) {
                try {
                    Key orgkey = org.getKey();
                    if (orgkey != null)
                        this.deleteOrganization(org.getKey());
                } catch (Exception e) {
                    log.error("Exception in finally:", e);
                }
            }
        }
    }

    private void checkBusinessExists(String bizname) {
        String request = "<find_business generic='2.0' xmlns='urn:uddi-org:api_v2'>" +
                "<name xml:lang='en'>" + bizname + "</name></find_business>";
        String response = null;
        try {
            response = rs.makeRegistrySpecificRequest(request);
        } catch (Exception e) {
            Assert.fail(e.getLocalizedMessage());
        }
        if (response == null || "".equals(response))
            Assert.fail("Find Business failed");

    }
}
