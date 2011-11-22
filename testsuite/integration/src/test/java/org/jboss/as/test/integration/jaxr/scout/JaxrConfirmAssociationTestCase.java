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
import org.jboss.as.test.integration.jaxr.scout.JaxrTestBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.registry.BusinessLifeCycleManager;
import javax.xml.registry.BusinessQueryManager;
import javax.xml.registry.Connection;
import javax.xml.registry.JAXRException;
import javax.xml.registry.LifeCycleManager;
import javax.xml.registry.RegistryService;
import javax.xml.registry.infomodel.Association;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.Key;
import javax.xml.registry.infomodel.Organization;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Tests confirming Association
 *
 * @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 * @author Thomas.Diesler@jboss.com
 * @since Mar 9, 2005
 */
@RunWith(Arquillian.class)
@Ignore("[AS7-2636] Refactor JAXR subsystem into a deployable application")
public class JaxrConfirmAssociationTestCase extends JaxrTestBase {
    /**
     * Testcase that tests the association between two organizations
     */
    @Test
    public void testConfirmAssociation() throws JAXRException {
        String orgTarget = "Target Organization2";
        String orgSource = "Source Organization2";
        String type = "Implements";
        Key savekey = null;

        Collection associationKeys = null;
        Collection sourceKeys = null;
        Collection targetKeys = null;
        String targetId = null;
        String sourceId = null;
        BusinessQueryManager bqm2 = null;
        BusinessLifeCycleManager blm2 = null;
        Association association = null;

        try {
            login();
            getJAXREssentials();

            // second user.
            Connection con2 = loginSecondUser();
            RegistryService rs2 = con2.getRegistryService();
            blm2 = rs2.getBusinessLifeCycleManager();
            bqm2 = rs2.getBusinessQueryManager();

            Organization target = blm2.createOrganization(blm.createInternationalString(orgTarget));
            Organization source = blm.createOrganization(blm.createInternationalString(orgSource));

            Collection orgs = new ArrayList();
            orgs.add(source);
            br = blm.saveOrganizations(orgs);
            if (br.getExceptions() != null) {
                Assert.fail("Source:Save Orgs failed");
            }

            sourceKeys = br.getCollection();
            Iterator iter = sourceKeys.iterator();
            while (iter.hasNext()) {
                savekey = (Key) iter.next();
            }
            sourceId = savekey.getId();

            String objectType = LifeCycleManager.ORGANIZATION;
            Organization pubSource = (Organization) bqm.getRegistryObject(sourceId, objectType);
            Assert.assertNotNull("Source retrieved: ", pubSource.getName().getValue());

            orgs.clear();
            orgs.add(target);
            br = blm2.saveOrganizations(orgs);
            if (br.getExceptions() != null) {
                Assert.fail("Target:Save Orgs failed");
            }
            targetKeys = br.getCollection();
            iter = targetKeys.iterator();
            while (iter.hasNext()) {
                savekey = (Key) iter.next();
            }
            targetId = savekey.getId();

            Organization pubTarget = (Organization) bqm2.getRegistryObject(targetId, objectType);
            Assert.assertNotNull("Target: ", pubTarget.getName().getValue());

            Concept associationType = getAssociationConcept(type);
            if (associationType == null)
                Assert.fail(" getAssociationConcept returned null");

            association = blm.createAssociation(pubTarget, associationType);
            association.setSourceObject(pubSource);

            blm2.confirmAssociation(association);

            Collection associations = new ArrayList();
            associations.add(association);
            br = blm2.saveAssociations(associations, false);
            if (br.getExceptions() != null) {
                Assert.fail(" Save Association did not complete due to errors");
            }

            associationKeys = br.getCollection();
            iter = associationKeys.iterator();

            Collection associationTypes = new ArrayList();
            associationTypes.add(associationType);
            //confirmedByCaller = false, confirmedByOtherParty = true.
            br = bqm.findCallerAssociations(null,
                    new Boolean(false),
                    new Boolean(true), associationTypes);
            if (br.getExceptions() != null) {
                Assert.fail(" Find Caller Association failed");
            }
            associations = br.getCollection();
            if (associations.size() == 0) {
                Assert.fail(" Retrieving Associations failed");
            }
            iter = associations.iterator();
            while (iter.hasNext()) {
                association = (Association) iter.next();
            }

            Assert.assertNotNull("Association type:", association.getAssociationType().getValue());
            if (association.isConfirmed()) {
                Assert.fail("FAIL: isConfirmed returned true  ");
            }
            if (association.isConfirmedBySourceOwner()) {
                Assert.fail("FAIL: isConfirmedBySourceOwner returned true  ");
            }

            blm.confirmAssociation(association);
            br = blm.saveAssociations(associations, false);
            if (br.getExceptions() != null) {
                Assert.fail("Error:  saveAssociations failed  ");
            }


            br = bqm.findCallerAssociations(null, new Boolean(true), new Boolean(true), associationTypes);

            if (br.getExceptions() != null) {
                Assert.fail("Error:  findCallerAssociations failed  ");
            }

            associations = br.getCollection();
            iter = associations.iterator();
            while (iter.hasNext()) {
                association = (Association) iter.next();
            }

            if (!(association.isConfirmed())) {
                Assert.fail("FAIL: isConfirmed incorrectly returned false ");
            }

            if (!(association.isConfirmedBySourceOwner())) {
                Assert.fail("FAIL: isConfirmedBySourceOwner incorrectly returned false ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Caught unexpected exception: " + e.getMessage());
        } finally {
            // Clean up
            try {
                blm2.deleteOrganizations(targetKeys);
                blm.deleteOrganizations(sourceKeys);
                if (association != null) {
                    Key asskey = association.getKey();
                    List<Key> keyList = new ArrayList<Key>();
                    keyList.add(asskey);
                    blm.deleteAssociations(keyList);
                }
            } catch (JAXRException je) {
                Assert.fail("Error: not able to delete registry objects");
            }
        }
    }

}
