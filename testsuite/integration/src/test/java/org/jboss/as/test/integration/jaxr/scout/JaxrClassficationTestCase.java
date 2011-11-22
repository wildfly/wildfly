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

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.registry.BulkResponse;
import javax.xml.registry.LifeCycleManager;
import javax.xml.registry.infomodel.ClassificationScheme;
import javax.xml.registry.infomodel.Concept;
import javax.xml.registry.infomodel.ExternalLink;
import javax.xml.registry.infomodel.Key;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.jaxr.scout.JaxrTestBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the storage of classifications on Concepts and Services
 *
 * @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 * @author <a href="mailto:Noel.Rocher@jboss.org">Noel Rocher</a>
 * @author Thomas.Diesler@jboss.com
 * @since Apr 11, 2006
 */
@RunWith(Arquillian.class)
@Ignore("[AS7-2636] Refactor JAXR subsystem into a deployable application")
public class JaxrClassficationTestCase extends JaxrTestBase {
    private static final String UUID_TYPE = "uuid:C1ACF26D-9672-4404-9D70-39B756E62AB4";

    @Test
    public void testClassificationOnConcepts() throws Exception {
        login();
        getJAXREssentials();
        Concept concept = null;
        Collection concepts = new ArrayList(1);
        String portTypeName = "Test Port Type";
        concept = blm.createConcept(null, portTypeName, "");
        ExternalLink wsdlLink = blm.createExternalLink("http://test.org/" + portTypeName, "TEST Port Type definition");
        concept.addExternalLink(wsdlLink);

        ClassificationScheme TYPE = (ClassificationScheme) bqm.getRegistryObject(UUID_TYPE, LifeCycleManager.CLASSIFICATION_SCHEME);
        //assertTrue("Classifications are not empty", TYPE.getClassifications().size() > 0);
        System.out.println("TYPE.Classifications = " + TYPE.getClassifications());
        concept.addClassification(blm.createClassification(TYPE, blm.createInternationalString("TEST CLASSIFICATION"), "test portType"));

        concepts.add(concept);
        BulkResponse response = blm.saveConcepts(concepts);
        if (response != null && response.getCollection().size() > 0) {
            concept.setKey((Key) response.getCollection().iterator().next());
            Assert.assertNotNull("Key created != null", concept.getKey());
            System.out.println("Concept Key = " + concept.getKey() + "\".");
        }

        //Obtain the saved concepts
        Concept savedConcept = (Concept) bqm.getRegistryObject(concept.getKey().getId(),
                LifeCycleManager.CONCEPT);
        Assert.assertNotNull("savedConcept is not null", savedConcept);
        Collection collection = savedConcept.getClassifications();
        Assert.assertNotNull("Classifications is not null", collection);
        Assert.assertTrue("Classifications is not empty", collection.isEmpty() == false);
    }
}
