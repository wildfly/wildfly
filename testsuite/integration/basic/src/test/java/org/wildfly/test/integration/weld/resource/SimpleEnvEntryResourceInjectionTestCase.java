/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.weld.resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test of behavior of {@code SimpleEnvEntryCdiResourceInjectionProcessor}.
 */
@ExtendWith(ArquillianExtension.class)
public class SimpleEnvEntryResourceInjectionTestCase {

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "SimpleEnvEntryResourceInjectionTestCase.war")
                .addClasses(SimpleEnvEntryResourceInjectionTestCase.class, EnvEntryInjectionBean.class)
                .addAsWebInfResource(SimpleEnvEntryResourceInjectionTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    private static EnvEntryInjectionBean bean;

    static void setBean(EnvEntryInjectionBean initialized) {
        bean = initialized;
    }

    EnvEntryInjectionBean getBean() {
        Assertions.assertNotNull(bean, EnvEntryInjectionBean.class.getSimpleName() + " was not initialized");
        return bean;
    }

    @AfterAll
    public static void afterClass() {
        bean = null;
    }

    @Test
    public void testUnmapped() {
        Assertions.assertNotNull(getBean().getUnmappedEnvEntry(), "Null should not be injected into unmappedEnvEntry");
        Assertions.assertEquals(1,getBean().getUnmappedEnvEntry().intValue(), "unmappedEnvEntry should have its initial value");
    }

    @Test
    public void testMapped() {
        Assertions.assertEquals(1,getBean().getMappedEnvEntry().intValue(), "mappedEnvEntry should have its deployment descriptor value");
    }

    @Test
    public void testUnnamedMapped() {
        Assertions.assertEquals(1,getBean().getUnnamedMappedEnvEntry().intValue(), "unnamedMappedEnvEntry should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedString() {
        Assertions.assertEquals("injected",getBean().getUnannotatedString(), "unannotatedString should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedBoolean() {
        Assertions.assertTrue(getBean().isUnannotatedBoolean(), "unannotatedBoolean should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedChar() {
        Assertions.assertEquals(Character.valueOf('b'), Character.valueOf(getBean().getUnannotatedChar()), "unannotatedChar should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedByte() {
        Assertions.assertEquals(Byte.valueOf((byte)1), Byte.valueOf(getBean().getUnannotatedByte()), "unannotatedByte should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedShort() {
        Assertions.assertEquals(1, getBean().getUnannotatedShort(), "unannotatedShort should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedInt() {
        Assertions.assertEquals(1, getBean().getUnannotatedInt(), "unannotatedInt should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedLong() {
        Assertions.assertEquals(1L, getBean().getUnannotatedLong(), "unannotatedLong should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedFloat() {
        Assertions.assertEquals((float) 1, getBean().getUnannotatedFloat(), 0.1, "unannotatedFloat should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedDouble() {
        Assertions.assertEquals((double) 1, getBean().getUnannotatedDouble(), 0.1, "unannotatedDouble should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedStringProperty() {
        Assertions.assertEquals("injected",getBean().getUnannotatedStringProperty(), "unannotatedStringProperty should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedBooleanProperty() {
        Assertions.assertTrue(getBean().isUnannotatedBooleanProperty(), "unannotatedBooleanProperty should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedIntProperty() {
        Assertions.assertEquals(1, getBean().getUnannotatedIntProperty(), "unannotatedIntProperty should have its deployment descriptor value");
    }

    @Test
    public void testUnannotatedIntLookup() {
        Assertions.assertEquals(1, getBean().getUnannotatedIntLookup(), "unannotatedIntLookup should have its deployment descriptor value");
    }
}