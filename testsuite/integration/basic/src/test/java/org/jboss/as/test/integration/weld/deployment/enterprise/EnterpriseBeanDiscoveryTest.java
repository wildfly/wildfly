/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.weld.deployment.enterprise;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class EnterpriseBeanDiscoveryTest {

    private static final String ALPHA_JAR = "alpha.jar";
    private static final String BRAVO_JAR = "bravo.jar";
    private static final String CHARLIE_JAR = "charlie.jar";
    private static final String DELTA_JAR = "delta.jar";
    private static final String ECHO_JAR = "echo.jar";
    private static final String FOXTROT_JAR = "foxtrot.jar";
    private static final String LEGACY_JAR = "legacy.jar";

    @Deployment
    public static EnterpriseArchive createTestArchive() {

        // 1.1 version beans.xml with bean-discovery-mode of all
        JavaArchive alpha = ShrinkWrap
                .create(JavaArchive.class, ALPHA_JAR)
                .addClasses(Alpha.class, AlphaLocal.class)
                .addAsManifestResource(newBeans11Descriptor("all"), "beans.xml");
        // Empty beans.xml
        JavaArchive bravo = ShrinkWrap.create(JavaArchive.class, BRAVO_JAR).addClasses(Bravo.class, BravoLocal.class)
                .addAsManifestResource(new StringAsset(""), "beans.xml");
        // No version beans.xml
        JavaArchive charlie = ShrinkWrap
                .create(JavaArchive.class, CHARLIE_JAR)
                .addClasses(Charlie.class, CharlieLocal.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        // Session bean and no beans.xml
        JavaArchive delta = ShrinkWrap.create(JavaArchive.class, DELTA_JAR).addClasses(Delta.class, DeltaLocal.class);
        // Session bean and 1.1 version beans.xml with bean-discovery-mode of annotated
        JavaArchive echo = ShrinkWrap
                .create(JavaArchive.class, ECHO_JAR)
                .addClasses(Echo.class, EchoLocal.class)
                .addAsManifestResource(newBeans11Descriptor("annotated"), "beans.xml");
        // Session bean and 1.1 version beans.xml with bean-discovery-mode of none
        JavaArchive foxtrot = ShrinkWrap
                .create(JavaArchive.class, FOXTROT_JAR)
                .addClasses(Foxtrot.class, FoxtrotLocal.class)
                .addAsManifestResource(newBeans11Descriptor("none"), "beans.xml");

        // Archive which contains an extension and no beans.xml file - not a bean archive
        JavaArchive legacy = ShrinkWrap.create(JavaArchive.class, LEGACY_JAR)
                .addClasses(LegacyExtension.class, LegacyBean.class)
                .addAsServiceProvider(Extension.class, LegacyExtension.class);

        StringBuilder manifestBuilder = new StringBuilder("Class-Path:");
        for (String s : new String[] {ALPHA_JAR, BRAVO_JAR, CHARLIE_JAR, DELTA_JAR, ECHO_JAR, FOXTROT_JAR, LEGACY_JAR}) {
            manifestBuilder.append(" ");
            manifestBuilder.append(s);
        }

        WebArchive webArchive = ShrinkWrap.create(WebArchive.class)
                .addClasses(EnterpriseBeanDiscoveryTest.class, VerifyingExtension.class)
                .addAsServiceProvider(Extension.class, VerifyingExtension.class)
                .setManifest(new StringAsset(manifestBuilder.toString()));

        return ShrinkWrap.create(EnterpriseArchive.class)
                .addAsModules(webArchive, alpha, bravo, charlie, delta, echo, foxtrot, legacy)
                .addAsLibrary(ShrinkWrap.create(JavaArchive.class).addClass(Ping.class));
    }

    @Inject
    VerifyingExtension extension;

    @Inject
    private BeanManager manager;

    @Test
    public void testExplicitBeanArchiveModeAll() {
        assertDiscoveredAndAvailable(AlphaLocal.class, Alpha.class);
    }

    @Test
    public void testExplicitBeanArchiveEmptyDescriptor() {
        assertDiscoveredAndAvailable(BravoLocal.class, Bravo.class);
    }

    @Test
    public void testExplicitBeanArchiveLegacyDescriptor() {
        assertDiscoveredAndAvailable(CharlieLocal.class, Charlie.class);
    }

    @Test
    public void testImplicitBeanArchiveNoDescriptor() {
        assertDiscoveredAndAvailable(DeltaLocal.class, Delta.class);
    }

    @Test
    public void testImplicitBeanArchiveModeAnnotated() {
        assertDiscoveredAndAvailable(EchoLocal.class, Echo.class);
    }

    @Test
    public void testNoBeanArchiveModeNone() {
        assertNotDiscoveredAndNotAvailable(FoxtrotLocal.class, Foxtrot.class);
    }

    @Test
    public void testNotBeanArchiveExtension() {
        assertNotDiscoveredAndNotAvailable(LegacyBean.class, LegacyBean.class);
    }

    private <T extends Ping, B extends Ping> void assertDiscoveredAndAvailable(Class<T> beanType, Class<B> beanClazz) {
        Bean<?> bean = manager.resolve(manager.getBeans(beanType));
        CreationalContext<?> ctx = manager.createCreationalContext(bean);
        @SuppressWarnings("unchecked")
        T instance = (T) manager.getReference(bean, beanType, ctx);
        Assert.assertNotNull(instance);
        Assert.assertTrue(extension.getObservedAnnotatedTypes().contains(beanClazz));
        instance.pong();
    }

    private <T extends Ping, B extends Ping> void assertNotDiscoveredAndNotAvailable(Class<T> beanType, Class<B> beanClazz) {
        Assert.assertFalse(extension.getObservedAnnotatedTypes().contains(beanClazz));
        Assert.assertTrue(manager.getBeans(beanType).isEmpty());
    }

    private static StringAsset newBeans11Descriptor(String mode) {
        return new StringAsset("<beans bean-discovery-mode=\"" + mode + "\" version=\"1.1\"/>");
    }

}
