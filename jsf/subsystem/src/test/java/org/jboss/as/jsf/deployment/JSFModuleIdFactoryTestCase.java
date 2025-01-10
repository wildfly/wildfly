/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.deployment;

import static org.jboss.as.controller.ModuleIdentifierUtil.canonicalModuleIdentifier;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the JSFModuleIdFactory
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JSFModuleIdFactoryTestCase {

    private static final String API_MODULE = "jakarta.faces.api";
    private static final String IMPL_MODULE = "jakarta.faces.impl";
    private static final String INJECTION_MODULE = "org.jboss.as.jsf-injection";

    private static final JSFModuleIdFactory factory = JSFModuleIdFactory.getInstance();

    @Test
    public void getActiveJSFVersionsTest() {
        List<String> versions = factory.getActiveJSFVersions();
        Assert.assertEquals(3, versions.size());
        Assert.assertTrue(versions.contains("main"));
        Assert.assertTrue(versions.contains("myfaces"));
    }

    @Test
    public void computeSlotTest() {
        Assert.assertEquals("main", factory.computeSlot("main"));
        Assert.assertEquals("main", factory.computeSlot(null));
        Assert.assertEquals("main", factory.computeSlot(JsfVersionMarker.JSF_4_0));
        Assert.assertEquals("myfaces2", factory.computeSlot("myfaces2"));
    }

    @Test
    public void validSlotTest() {
        Assert.assertTrue(factory.isValidJSFSlot("main"));
        Assert.assertTrue(factory.isValidJSFSlot("myfaces"));
        Assert.assertTrue(factory.isValidJSFSlot(JsfVersionMarker.JSF_4_0));
        Assert.assertFalse(factory.isValidJSFSlot(JsfVersionMarker.WAR_BUNDLES_JSF_IMPL));
        Assert.assertFalse(factory.isValidJSFSlot("bogus"));
        Assert.assertFalse(factory.isValidJSFSlot("bogus2"));
   }

    @Test
    public void modIdsTest() {
        String apiModIdMain = factory.getApiModId("main");
        String implModIdMain = factory.getImplModId("main");
        String injectionModIdMain = factory.getInjectionModId("main");


        Assert.assertEquals(canonicalModuleIdentifier(API_MODULE, "main"), apiModIdMain);
        Assert.assertEquals(canonicalModuleIdentifier(IMPL_MODULE, "main"), implModIdMain);
        Assert.assertEquals(canonicalModuleIdentifier(INJECTION_MODULE, "main"), injectionModIdMain);

        String apiModIdMyfaces = factory.getApiModId("myfaces");
        String implModIdMyfaces = factory.getImplModId("myfaces");
        String injectionModIdMyfaces = factory.getInjectionModId("myfaces");

        Assert.assertEquals(canonicalModuleIdentifier(API_MODULE, "myfaces"), apiModIdMyfaces);
        Assert.assertEquals(canonicalModuleIdentifier(IMPL_MODULE, "myfaces"), implModIdMyfaces);
        Assert.assertEquals(canonicalModuleIdentifier(INJECTION_MODULE, "myfaces"), injectionModIdMyfaces);

        String apiModIdMyfaces2 = factory.getApiModId("myfaces2");
        String implModIdMyfaces2 = factory.getImplModId("myfaces2");
        String injectionModIdMyfaces2 = factory.getInjectionModId("myfaces2");

        Assert.assertEquals(canonicalModuleIdentifier(API_MODULE, "myfaces2"), apiModIdMyfaces2);
        Assert.assertEquals(canonicalModuleIdentifier(IMPL_MODULE, "myfaces2"), implModIdMyfaces2);
        Assert.assertEquals(canonicalModuleIdentifier(INJECTION_MODULE, "myfaces2"), injectionModIdMyfaces2);
    }
}
