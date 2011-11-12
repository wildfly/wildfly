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
import javax.xml.registry.LifeCycleManager;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.Organization;
import javax.xml.registry.infomodel.Service;
import javax.xml.registry.infomodel.ServiceBinding;
import javax.xml.registry.infomodel.SpecificationLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


/**
 * Tests Saving ServiceBindings
 *
 * @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 * @author Thomas.Diesler@jboss.com
 * @since Mar 8, 2005
 */
@RunWith(Arquillian.class)
public class JaxrSaveServiceBindingTestCase extends JaxrTestBase
{
   private static Logger log = Logger.getLogger(JaxrSaveServiceBindingTestCase.class);

    @Test
    public void testSaveServiceBinding() throws JAXRException
    {
        String serviceName = "jbosstestService";
        String sbDescription = "jbosstest sb description";

        String conceptName = "jbosstest concept";
        Collection sbKeys = null;
        Collection serviceKeys = null;
        Collection orgKeys = null;
        Collection conceptKeys = null;
        Key conceptKey = null;
        Key serviceKey = null;
        Key orgKey = null;

        String accessURI = "http://myhost/jaxrTest.jsp";
        login();


        try
        {
            getJAXREssentials();
            String orgname = "Jaxr Org";
            Organization org = blm.createOrganization(getIString(orgname));
            Collection orgs = new ArrayList();
            orgs.add(org);
            BulkResponse br = blm.saveOrganizations(orgs);
            if (br.getExceptions() != null)
            {
                Assert.fail("Save Organizations failed ");
            }
            orgKeys = br.getCollection();
            Iterator iter = orgKeys.iterator();
            while (iter.hasNext())
            {
                orgKey = (Key) iter.next();
            }


            org = (Organization) bqm.getRegistryObject(orgKey.getId(), LifeCycleManager.ORGANIZATION);

            Service service = blm.createService(serviceName);
            org.addService(service);
            Collection services = new ArrayList();
            services.add(service);
            br = blm.saveServices(services);
            if (br.getExceptions() != null)
            {
                Assert.fail("Save Services failed ");
            }
            serviceKeys = br.getCollection();
            iter = serviceKeys.iterator();
            while (iter.hasNext())
            {
                serviceKey = (Key) iter.next();
            }

            service = (Service) bqm.getRegistryObject(serviceKey.getId(), LifeCycleManager.SERVICE);

            //Save some concepts
            Concept testConcept = (Concept) blm.createObject(LifeCycleManager.CONCEPT);
            testConcept.setName(blm.createInternationalString(conceptName));
            Collection concepts = new ArrayList();
            concepts.add(testConcept);
            br = blm.saveConcepts(concepts);
            if (br.getExceptions() != null)
            {
                Assert.fail("Save Concepts failed ");
            }
            conceptKeys = br.getCollection();
            iter = conceptKeys.iterator();
            while (iter.hasNext())
            {
                conceptKey = (Key) iter.next();
            }

            testConcept = (Concept) bqm.getRegistryObject(conceptKey.getId(), LifeCycleManager.CONCEPT);
            SpecificationLink sl = blm.createSpecificationLink();
            sl.setSpecificationObject(testConcept);
            ServiceBinding sb = blm.createServiceBinding();
            sb.setDescription(blm.createInternationalString(sbDescription));
            sb.setAccessURI(accessURI);
            sb.addSpecificationLink(sl);
            service.addServiceBinding(sb);
            Collection sbs = new ArrayList();
            sbs.add(sb);
            br = blm.saveServiceBindings(sbs);
            if (br.getExceptions() != null)
            {
                Assert.fail("Save ServiceBindings failed ");
            }


            Collection specifications = new ArrayList();
            specifications.add(testConcept);

            br = bqm.findServiceBindings(serviceKey, null, null, specifications);
            sbs = br.getCollection();
            iter = sbs.iterator();
            while (iter.hasNext())
            {
                sb = (ServiceBinding) iter.next();
                Service storedService = sb.getService();
                if (!(storedService.getName().getValue().equals(serviceName)))
                {
                    Assert.fail("Error: service name");
                }
                Organization storedOrg = storedService.getProvidingOrganization();
                if (!(storedOrg.getName().getValue().equals(orgname)))
                {
                    Assert.fail("Error: unexpected organization name \n");
                }
                if (!(sb.getDescription().getValue().equals(sbDescription)))
                {
                    Assert.fail("Error: servicebinding description");
                }
                if (!(sb.getAccessURI().equals(accessURI)))
                {
                    Assert.fail("Error: unexpected accessURI name");
                }
            }

            //Lets update the ServiceBinding
            sbs = new ArrayList();
            sb.setAccessURI("http://newURI");
            sbs.add(sb);
            br = blm.saveServiceBindings(sbs);
            br = bqm.findServiceBindings(serviceKey, null, null, specifications);
            sbs = br.getCollection();
            iter = sbs.iterator();
            while (iter.hasNext())
            {
                sb = (ServiceBinding) iter.next();
                Service storedService = sb.getService();
                if (!(storedService.getName().getValue().equals(serviceName)))
                {
                    Assert.fail("Error: service name");
                }
                Organization storedOrg = storedService.getProvidingOrganization();
                if (!(storedOrg.getName().getValue().equals(orgname)))
                {
                    Assert.fail("Error: unexpected organization name \n");
                }
                if (!(sb.getDescription().getValue().equals(sbDescription)))
                {
                    Assert.fail("Error: servicebinding description");
                }
                if (!(sb.getAccessURI().equals("http://newURI")))
                {
                    Assert.fail("Error: unexpected accessURI name");
                }
            }
        } catch (Exception e)
        {
            log.error("Exception:",e);
            Assert.fail("Test has failed due to an exception:" + e.getMessage());
        } finally
        {
           try
           {
              if (conceptKeys != null)
              {
                 blm.deleteConcepts(conceptKeys);
              } 
           } catch (JAXRException je)
           {
              Assert.fail("Cleanup of JAXR objects failed:" + je);
           }
           try{

              if (sbKeys != null)
              {
                 blm.deleteServiceBindings(sbKeys);
              } 

           } catch (JAXRException je)
           {
              Assert.fail("Cleanup of JAXR objects failed:" + je);
           }

           try
           {
              if (serviceKeys != null)
              {
                 blm.deleteServices(serviceKeys);
              } 

           } catch (JAXRException je)
           {
              Assert.fail("Cleanup of JAXR objects failed:" + je);
           } 
           try
           { 
              if (orgKeys != null)
              {
                 blm.deleteOrganizations(orgKeys);
              }
           } 
           catch (JAXRException je)
           {
              Assert.fail("Cleanup of JAXR objects failed:" + je);
           }
        }
    } //end method
}