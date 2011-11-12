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
 * Checks Deletion of Organization
 *
 * @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 * @author Thomas.Diesler@jboss.com
 * @since Jan 3, 2005
 */
@RunWith(Arquillian.class)
public class JaxrDeleteOrganizationTestCase extends JaxrTestBase
{
   private static Logger log = Logger.getLogger(JaxrDeleteAssociationTestCase.class);

    @Test
    public void testDeleteOrgs() throws Exception
    {
        String keyid = this.saveOrg("DELETEORG");
        Assert.assertNotNull(keyid);
        Key key = blm.createKey(keyid);
        this.deleteOrganization(key);
    }

    private String saveOrg(String orgname)
    {
        String keyid = "";
        login();
        Organization org = null;
        try
        {
            getJAXREssentials();
            Collection orgs = new ArrayList();
            org = createOrganization("JBOSS");

            orgs.add(org);
            BulkResponse br = blm.saveOrganizations(orgs);
            if (br.getStatus() == JAXRResponse.STATUS_SUCCESS)
            {
                if ("true".equalsIgnoreCase(debugProp))
                    System.out.println("Organization Saved");
                Collection coll = br.getCollection();
                Iterator iter = coll.iterator();
                while (iter.hasNext())
                {
                    Key key = (Key) iter.next();
                    keyid = key.getId();
                    if ("true".equalsIgnoreCase(debugProp))
                        System.out.println("Saved Key=" + key.getId());
                    Assert.assertNotNull(keyid);
                }//end while
            } else
            {
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
        } catch (JAXRException e)
        {
            log.error("Exception:",e);
            Assert.fail(e.getMessage());
        }
        finally
        {
           if(org != null)
           {
              try
              {
                 Key orgkey = org.getKey();
                 if(orgkey != null)
                   this.deleteOrganization(org.getKey()); 
              }
              catch(Exception e)
              {
                 log.error("Cleanup failed:",e); 
              }  
           }
        }
        return keyid;
    }
}
