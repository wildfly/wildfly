/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.metrics;


import static junit.framework.Assert.*;
import org.junit.AfterClass;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.test.integration.management.jca.DsMgmtTestBase;
import org.jboss.as.test.smoke.modular.utils.ShrinkWrapUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * XA datasource configuration and metrics unit test.
 * 
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class XaDataSourceCfgMetricUnitTestCase  extends DsMgmtTestBase{

	@Deployment
    public static Archive<?> getDeployment() {
    	initModelControllerClient("localhost",9999);
    	setBaseAddress("xa-data-source", "DS");
        return ShrinkWrapUtils.createEmptyJavaArchive("dummy");
    }

    @AfterClass
    public static void tearDown()  throws Exception {
    	closeModelControllerClient();
    }
    

    @Test
    public void testDefaultXaDsAttributes()throws Exception {
    	setModel("xa-basic-attributes.xml");
    	assertTrue(readAttribute(baseAddress,"use-ccm").asBoolean());
        assertTrue(readAttribute(baseAddress,"use-java-context").asBoolean());
        assertFalse(readAttribute(baseAddress,"spy").asBoolean());
        removeDs();
    }
    
    @Test(expected=Exception.class)
    @Ignore("AS7-3578")
    public void testNoXaDsProperties()throws Exception {
    	setBadModel("wrong-no-xa-ds-properties.xml");
    }
    
    @Test
     public void testDefaultXaDsProperties()throws Exception {
    	setModel("xa-default-properties.xml");
        assertTrue(readAttribute(baseAddress,"wrap-xa-resource").asBoolean());
        removeDs();
    }
    
    @Test
    public void testBooleanPresenceProperties()throws Exception {
    	setModel("xa-bool-pres-properties.xml");
    	assertTrue(readAttribute(baseAddress,"no-tx-separate-pool").asBoolean());
        assertTrue(readAttribute(baseAddress,"interleaving").asBoolean());
        assertTrue(readAttribute(baseAddress,"set-tx-query-timeout").asBoolean());
        assertTrue(readAttribute(baseAddress,"share-prepared-statements").asBoolean());
        removeDs();
    }
    
    @Test
    public void testFalseBooleanPresenceProperties()throws Exception {
    	setModel("xa-false-bool-pres-properties.xml");
    	assertFalse(readAttribute(baseAddress,"no-tx-separate-pool").asBoolean());
        assertFalse(readAttribute(baseAddress,"interleaving").asBoolean());
        assertFalse(readAttribute(baseAddress,"set-tx-query-timeout").asBoolean());
        assertFalse(readAttribute(baseAddress,"share-prepared-statements").asBoolean());
        removeDs();
    }
    
    @Test
    public void testTrueBooleanPresenceProperties()throws Exception {
    	setModel("xa-true-bool-pres-properties.xml");
    	assertTrue(readAttribute(baseAddress,"no-tx-separate-pool").asBoolean());
        assertTrue(readAttribute(baseAddress,"interleaving").asBoolean());
        assertTrue(readAttribute(baseAddress,"set-tx-query-timeout").asBoolean());
        assertTrue(readAttribute(baseAddress,"share-prepared-statements").asBoolean());
        removeDs();
    }
    
    @Test(expected=Exception.class)
    @Ignore("AS7-3578")
    public void testWrongXa2SecurityDomainsProperty()throws Exception {
    	setBadModel("wrong-xa-2-security-domains.xml");
    }
    
    @Test(expected=Exception.class)
    public void testWrongXaResTimeoutProperty()throws Exception {
    	setBadModel("wrong-xa-res-timeout-property.xml");
    }
    
}
