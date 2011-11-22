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
import org.jboss.as.test.integration.jaxr.scout.ScoutUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.registry.BulkResponse;
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

/**
 * Tests FindAssociation using JAXR API
 *
 * @author <mailto:Anil.Saldhana@jboss.org>Anil Saldhana
 * @author Thomas.Diesler@jboss.com
 * @since Mar 9, 2005
 */
@RunWith(Arquillian.class)
@Ignore("[AS7-2636] Refactor JAXR subsystem into a deployable application")
public class JaxrFindAssociationsTestCase extends JaxrTestBase {

    @Test
    public void testFindAssociations() throws JAXRException {

        String orgSource = "Source Organization";
        String type = "Implements";

        Key savekey = null;
        Key assockey = null;

        BusinessQueryManager bqm2 = null;
        BusinessLifeCycleManager blm2 = null;
        Collection associationKeys = null;
        Collection sourceKeys = null;
        Collection targetKeys = null;
        String targetId = null;
        String sourceId = null;
        String orgTarget = "Target Organization";

        try {
            login();
            getJAXREssentials();
            // Authenticate second user
            Connection con2 = loginSecondUser();
            RegistryService rs2 = con2.getRegistryService();
            blm2 = rs2.getBusinessLifeCycleManager();
            bqm2 = rs2.getBusinessQueryManager();

            Organization source = blm.createOrganization(blm.createInternationalString(orgSource));
            Organization target = blm2.createOrganization(blm.createInternationalString(orgTarget));

            // publish the source organization
            Collection orgs = new ArrayList();
            orgs.add(source);
            br = blm.saveOrganizations(orgs);
            if (br.getExceptions() != null) {
                Assert.fail(" Source:Save Orgs failed");
            }

            sourceKeys = br.getCollection();
            Iterator iter = sourceKeys.iterator();
            while (iter.hasNext()) {
                savekey = (Key) iter.next();
            }
            sourceId = savekey.getId();
            String objectType = LifeCycleManager.ORGANIZATION;
            Organization pubSource = (Organization) bqm.getRegistryObject(sourceId, objectType);
            Assert.assertNotNull(pubSource.getName().getValue());

            // publish the target
            orgs.clear();
            orgs.add(target);
            br = blm2.saveOrganizations(orgs);
            if (br.getExceptions() != null) {
                Assert.fail(" Target:Save Orgs failed");
            }
            targetKeys = br.getCollection();
            iter = targetKeys.iterator();
            while (iter.hasNext()) {
                savekey = (Key) iter.next();
            }
            targetId = savekey.getId();
            Organization pubTarget = (Organization) bqm2.getRegistryObject(targetId, objectType);

            Concept associationType = getAssociationConcept(type);
            if (associationType == null)
                Assert.fail(" getAssociationConcept returned null associationType");

            Association a = blm.createAssociation(pubTarget, associationType);
            a.setSourceObject(pubSource);

            blm.confirmAssociation(a); //First user
            blm2.confirmAssociation(a); //Second user

            // publish Association
            Collection associations = new ArrayList();
            associations.add(a);
            // Second user saves the association.
            br = blm2.saveAssociations(associations, false);

            if (br.getExceptions() != null) {
                Assert.fail("Error:Save Associations failed \n");
            }
            BulkResponse targetAssoc = bqm.findCallerAssociations(null,
                    new Boolean(true),
                    new Boolean(true),
                    null);

            if (targetAssoc.getExceptions() == null) {
                Collection targetCol = targetAssoc.getCollection();
                if (targetCol.size() > 0) {
                    iter = targetCol.iterator();
                    while (iter.hasNext()) {
                        Association a1 = (Association) iter.next();
                        Organization o = (Organization) a1.getSourceObject();
                        o = (Organization) a1.getTargetObject();
                        Concept atype = a1.getAssociationType();
                        Assert.assertNotNull("Concept Type stored in Association", atype);
                    }
                }
            }


            br = null;
            Collection associationTypes = new ArrayList();
            associationTypes.add(type);
            br = bqm.findAssociations(null, sourceId, targetId, null);
            if (br.getExceptions() != null) {
                Assert.fail("Error: findAssociations failed ");
            }
            associations = null;
            associations = br.getCollection();
            if (associations.size() > 0) {
                iter = associations.iterator();
                while (iter.hasNext()) {
                    Association a1 = (Association) iter.next();
                    assockey = a1.getKey();
                    ScoutUtil.validateAssociation(a1, orgSource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(" Test failed ");
        } finally {
            //Clean Up
            try {
                if (assockey != null) {
                    associationKeys = new ArrayList();
                    associationKeys.add(assockey);
                    blm.deleteAssociations(associationKeys);
                }
                blm2.deleteOrganizations(targetKeys);
                blm.deleteOrganizations(sourceKeys);
            } catch (JAXRException ex) {
                ex.printStackTrace();
                Assert.fail("Error: Cleanup failed");
            }
        }

    } // end of method


}
