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

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.registry.BulkResponse;
import javax.xml.registry.BusinessLifeCycleManager;
import javax.xml.registry.BusinessQueryManager;
import javax.xml.registry.Connection;
import javax.xml.registry.ConnectionFactory;
import javax.xml.registry.FindQualifier;
import javax.xml.registry.JAXRException;
import javax.xml.registry.RegistryService;
import javax.xml.registry.infomodel.Classification;
import javax.xml.registry.infomodel.ClassificationScheme;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.EmailAddress;
import javax.xml.registry.infomodel.ExternalIdentifier;
import javax.xml.registry.infomodel.InternationalString;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.LocalizedString;
import javax.xml.registry.infomodel.Organization;
import javax.xml.registry.infomodel.PersonName;
import javax.xml.registry.infomodel.PostalAddress;
import javax.xml.registry.infomodel.RegistryObject;
import javax.xml.registry.infomodel.Service;
import javax.xml.registry.infomodel.ServiceBinding;
import javax.xml.registry.infomodel.TelephoneNumber;
import javax.xml.registry.infomodel.User;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Acts as the base class for Jaxr Test Cases
 *
 * @author Anil.Saldhana@jboss.com
 * @author Thomas.Diesler@jboss.com
 * @since 26-Oct-2011
 */
public abstract class JaxrTestBase {

    protected String userid = "jboss";
    protected String passwd = "jboss";
    protected BusinessLifeCycleManager blm = null;
    protected RegistryService rs = null;
    protected BusinessQueryManager bqm = null;
    protected Connection connection = null;
    protected BulkResponse br = null;

    protected ConnectionFactory factory = null;

    protected static String debugProp = System.getProperty("jaxr.debug", "true");

    @Before
    public void setUp() throws Exception {
        //Ensure that the Jaxr Connection Factory class is setup
        String factoryString = "javax.xml.registry.ConnectionFactoryClass";
        String factoryClass = System.getProperty(factoryString);
        if (factoryClass == null || factoryClass.length() == 0)
            System.setProperty(factoryString, "org.apache.ws.scout.registry.ConnectionFactoryImpl");

        String queryurl = System.getProperty("jaxr.query.url", "http://localhost:8080/juddi/inquiry");
        String puburl = System.getProperty("jaxr.publish.url", "http://localhost:8080/juddi/publish");

        Properties props = new Properties();
        props.setProperty("javax.xml.registry.queryManagerURL", queryurl);
        props.setProperty("javax.xml.registry.lifeCycleManagerURL", puburl);

        String transportClass = System.getProperty("scout.proxy.transportClass", "org.jboss.as.jaxr.scout.SaajTransport");
        System.setProperty("scout.proxy.transportClass", transportClass);
        try {
            // Create the connection, passing it the configuration properties
            factory = ConnectionFactory.newInstance();
            factory.setProperties(props);
            connection = factory.createConnection();
        } catch (JAXRException e) {
            Assert.fail("Setup failed" + e);
        }
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) connection.close();
    }

    protected void login() {
        PasswordAuthentication passwdAuth = new PasswordAuthentication(userid, passwd.toCharArray());
        Set creds = new HashSet();
        creds.add(passwdAuth);
        try {
            connection.setCredentials(creds);
        } catch (JAXRException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    protected void getJAXREssentials() throws JAXRException {
        rs = connection.getRegistryService();
        blm = rs.getBusinessLifeCycleManager();
        bqm = rs.getBusinessQueryManager();
    }

    protected InternationalString getIString(String str) throws JAXRException {
        return blm.createInternationalString(str);
    }

    /**
     * Locale aware Search a business in the registry
     */
    protected void searchBusiness(String bizname) throws JAXRException {
        try {
            // Get registry service and business query manager
            this.getJAXREssentials();

            // Define find qualifiers and name patterns
            Collection findQualifiers = new ArrayList();
            findQualifiers.add(FindQualifier.SORT_BY_NAME_ASC);
            Collection namePatterns = new ArrayList();
            String pattern = "%" + bizname + "%";
            LocalizedString ls = blm.createLocalizedString(Locale.getDefault(), pattern);
            namePatterns.add(ls);

            // Find based upon qualifier type and values
            BulkResponse response = bqm.findOrganizations(findQualifiers, namePatterns, null, null, null, null);

            // check how many organisation we have matched
            Collection orgs = response.getCollection();
            if (orgs == null) {
                if ("true".equalsIgnoreCase(debugProp))
                    System.out.println(" -- Matched 0 orgs");

            } else {
                if ("true".equalsIgnoreCase(debugProp))
                    System.out.println(" -- Matched " + orgs.size() + " organizations -- ");

                // then step through them
                for (Iterator orgIter = orgs.iterator(); orgIter.hasNext(); ) {
                    Organization org = (Organization) orgIter.next();
                    if ("true".equalsIgnoreCase(debugProp)) {
                        System.out.println("Org name: " + getName(org));
                        System.out.println("Org description: " + getDescription(org));
                        System.out.println("Org key id: " + getKey(org));
                    }
                    checkUser(org);
                    checkServices(org);
                }
            }//end else
        } catch (JAXRException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            connection.close();
        }

    }

    protected RegistryService getRegistryService() throws JAXRException {
        Assert.assertNotNull(connection);
        return connection.getRegistryService();
    }

    protected BusinessQueryManager getBusinessQueryManager() throws JAXRException {
        Assert.assertNotNull(connection);
        if (rs == null) rs = this.getRegistryService();
        return rs.getBusinessQueryManager();
    }

    protected BusinessLifeCycleManager getBusinessLifeCycleManager() throws JAXRException {
        Assert.assertNotNull(connection);
        if (rs == null) rs = this.getRegistryService();
        return rs.getBusinessLifeCycleManager();
    }

    private static void checkServices(Organization org)
            throws JAXRException {
        // Display service and binding information
        Collection services = org.getServices();
        for (Iterator svcIter = services.iterator(); svcIter.hasNext(); ) {
            Service svc = (Service) svcIter.next();
            if ("true".equalsIgnoreCase(debugProp)) {
                System.out.println(" Service name: " + getName(svc));
                System.out.println(" Service description: " + getDescription(svc));
            }
            Assert.assertEquals("JBOSS JAXR Service", getName(svc));
            Assert.assertEquals("Services of XML Registry", getDescription(svc));

            Collection serviceBindings = svc.getServiceBindings();
            for (Iterator sbIter = serviceBindings.iterator(); sbIter.hasNext(); ) {
                ServiceBinding sb = (ServiceBinding) sbIter.next();
                if ("true".equalsIgnoreCase(debugProp)) {
                    System.out.println("  Binding Description: " + getDescription(sb));
                    System.out.println("  Access URI: " + sb.getAccessURI());
                }
                Assert.assertEquals("http://testjboss.org", sb.getAccessURI());
                Assert.assertEquals("Test Service Binding", getDescription(sb));
            }
        }
    }

    private static void checkUser(Organization org)
            throws JAXRException {
        // Display primary contact information
        User pc = org.getPrimaryContact();
        if (pc != null) {
            PersonName pcName = pc.getPersonName();
            System.out.println(" Contact name: " + pcName.getFullName());
            Assert.assertEquals("Anil S", pcName.getFullName());
            Collection phNums = pc.getTelephoneNumbers(pc.getType());
            for (Iterator phIter = phNums.iterator(); phIter.hasNext(); ) {
                TelephoneNumber num = (TelephoneNumber) phIter.next();
                System.out.println("  Phone number: " + num.getNumber());
            }
            Collection eAddrs = pc.getEmailAddresses();
            for (Iterator eaIter = eAddrs.iterator(); eaIter.hasNext(); ) {
                System.out.println("  Email Address: " + (EmailAddress) eaIter.next());
            }
        }
    }

    private static String getName(RegistryObject ro) throws JAXRException {
        if (ro != null && ro.getName() != null) {
            return ro.getName().getValue();
        }
        return "";
    }

    private static String getDescription(RegistryObject ro) throws JAXRException {
        if (ro != null && ro.getDescription() != null) {
            return ro.getDescription().getValue();
        }
        return "";
    }

    private static String getKey(RegistryObject ro) throws JAXRException {
        if (ro != null && ro.getKey() != null) {
            return ro.getKey().getId();
        }
        return "";
    }

    /**
     * Creates a Jaxr Organization with 1 or more services
     */
    protected Organization createOrganization(String orgname)
            throws JAXRException {
        Organization org = blm.createOrganization(getIString(orgname));
        org.setDescription(getIString("JBoss Inc"));
        Service service = blm.createService(getIString("JBOSS JAXR Service"));
        service.setDescription(getIString("Services of XML Registry"));
        //Create serviceBinding
        ServiceBinding serviceBinding = blm.createServiceBinding();
        serviceBinding.setDescription(blm.createInternationalString("Test Service Binding"));

        //Turn validation of URI off
        serviceBinding.setValidateURI(false);
        serviceBinding.setAccessURI("http://testjboss.org");

        // Add the serviceBinding to the service
        service.addServiceBinding(serviceBinding);

        User user = blm.createUser();
        org.setPrimaryContact(user);
        PersonName personName = blm.createPersonName("Anil S");
        TelephoneNumber telephoneNumber = blm.createTelephoneNumber();
        telephoneNumber.setNumber("111-111-7777");
        telephoneNumber.setType(null);
        PostalAddress address = blm.createPostalAddress("111", "My Drive", "BuckHead", "GA", "USA", "1111-111", "");
        Collection postalAddresses = new ArrayList();
        postalAddresses.add(address);
        Collection emailAddresses = new ArrayList();
        EmailAddress emailAddress = blm.createEmailAddress("anil@apache.org");
        emailAddresses.add(emailAddress);

        Collection numbers = new ArrayList();
        numbers.add(telephoneNumber);
        user.setPersonName(personName);
        user.setPostalAddresses(postalAddresses);
        user.setEmailAddresses(emailAddresses);
        user.setTelephoneNumbers(numbers);

        ClassificationScheme cScheme = getClassificationScheme("ntis-gov:naics", "");
        Key cKey = blm.createKey("uuid:C0B9FE13-324F-413D-5A5B-2004DB8E5CC2");
        cScheme.setKey(cKey);
        Classification classification = blm.createClassification(cScheme, "Computer Systems Design and Related Services", "5415");
        org.addClassification(classification);
        ClassificationScheme cScheme1 = getClassificationScheme("D-U-N-S", "");
        Key cKey1 = blm.createKey("uuid:3367C81E-FF1F-4D5A-B202-3EB13AD02423");
        cScheme1.setKey(cKey1);
        ExternalIdentifier ei = blm.createExternalIdentifier(cScheme1, "D-U-N-S number", "08-146-6849");
        org.addExternalIdentifier(ei);
        org.addService(service);
        return org;
    }

    /**
     * Delete an Organization with a given key
     */
    protected void deleteOrganization(Key orgkey) throws Exception {
        Assert.assertNotNull("Org Key is null?", orgkey);
        if (blm == null) blm = this.getBusinessLifeCycleManager();
        Collection keys = new ArrayList();
        keys.add(orgkey);
        BulkResponse response = blm.deleteOrganizations(keys);
        Collection exceptions = response.getExceptions();
        Assert.assertNull("Deleting Org with Key=" + orgkey, exceptions);
    }

    private ClassificationScheme getClassificationScheme(String str1, String str2) throws JAXRException {
        ClassificationScheme cs = blm.createClassificationScheme(getIString(str1), getIString(str2));
        return cs;
    }

    protected Connection loginSecondUser() {
        Connection con = null;
        try {
            if (factory == null)
                throw new IllegalStateException("ConnectionFactory is null");
            con = factory.createConnection();
        } catch (JAXRException e) {
            e.printStackTrace();
        }
        PasswordAuthentication passwdAuth = new PasswordAuthentication("jbosscts", passwd.toCharArray());
        Set creds = new HashSet();
        creds.add(passwdAuth);

        try {
            con.setCredentials(creds);
        } catch (JAXRException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        return con;
    }

    protected Concept getAssociationConcept(String associationType) {
        try {
            BusinessQueryManager bqm = rs.getBusinessQueryManager();
            ClassificationScheme associationTypes =
                    bqm.findClassificationSchemeByName(null, "AssociationType");
            Collection types = associationTypes.getChildrenConcepts();
            Iterator iter = types.iterator();
            Concept concept = null;
            while (iter.hasNext()) {
                concept = (Concept) iter.next();
                if (concept.getName().getValue().equals(associationType)) {
                    return concept;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;

    }
}
