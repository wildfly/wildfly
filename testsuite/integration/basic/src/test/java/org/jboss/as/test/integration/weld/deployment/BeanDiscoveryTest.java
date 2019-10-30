package org.jboss.as.test.integration.weld.deployment;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
public class BeanDiscoveryTest {

    @Deployment
    public static WebArchive createTestArchive() {

        // 1.1 version beans.xml with bean-discovery-mode of all
        JavaArchive alpha = ShrinkWrap
                .create(JavaArchive.class)
                .addClass(Alpha.class)
                .addAsManifestResource(newBeans11Descriptor("all"), "beans.xml");
        // Empty beans.xml
        JavaArchive bravo = ShrinkWrap.create(JavaArchive.class).addClass(Bravo.class)
                .addAsManifestResource(new StringAsset(""), "beans.xml");
        // No version beans.xml
        JavaArchive charlie = ShrinkWrap
                .create(JavaArchive.class)
                .addClass(Charlie.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        // Bean defining annotation and no beans.xml
        JavaArchive delta = ShrinkWrap.create(JavaArchive.class).addClass(Delta.class);
        // Bean defining annotation and 1.1 version beans.xml with bean-discovery-mode of annotated
        JavaArchive echo = ShrinkWrap
                .create(JavaArchive.class)
                .addClasses(Echo.class, EchoNotABean.class)
                .addAsManifestResource(newBeans11Descriptor("annotated"), "beans.xml");
        // Bean defining annotation and 1.1 version beans.xml with bean-discovery-mode of none
        JavaArchive foxtrot = ShrinkWrap
                .create(JavaArchive.class)
                .addClass(Foxtrot.class)
                .addAsManifestResource(newBeans11Descriptor("none"), "beans.xml");

        // Archive which contains an extension and no beans.xml file
        JavaArchive legacy = ShrinkWrap.create(JavaArchive.class).addClasses(LegacyExtension.class, LegacyAlpha.class,
                LegacyBravo.class).addAsServiceProvider(Extension.class, LegacyExtension.class);

        return ShrinkWrap.create(WebArchive.class).addClasses(BeanDiscoveryTest.class, VerifyingExtension.class)
                .addAsServiceProvider(Extension.class, VerifyingExtension.class)
                .addAsLibrary(ShrinkWrap.create(JavaArchive.class).addClass(Ping.class))
                .addAsLibraries(alpha, bravo, charlie, delta, echo, foxtrot, legacy);
    }

    @Inject
    private VerifyingExtension extension;

    @Inject
    private BeanManager manager;

    @Test
    public void testExplicitBeanArchiveModeAll(Alpha alpha) {
        assertDiscoveredAndAvailable(alpha, Alpha.class);
    }

    @Test
    public void testExplicitBeanArchiveEmptyDescriptor(Bravo bravo) {
        assertDiscoveredAndAvailable(bravo, Bravo.class);
    }

    @Test
    public void testExplicitBeanArchiveLegacyDescriptor(Charlie charlie) {
        assertDiscoveredAndAvailable(charlie, Charlie.class);
    }

    @Test
    public void testImplicitBeanArchiveNoDescriptor(Delta delta) {
        assertDiscoveredAndAvailable(delta, Delta.class);
    }

    @Test
    public void testImplicitBeanArchiveModeAnnotated(Echo echo) {
        assertDiscoveredAndAvailable(echo, Echo.class);
        assertNotDiscoveredAndNotAvailable(EchoNotABean.class);
    }

    @Test
    public void testNoBeanArchiveModeNone() {
        assertNotDiscoveredAndNotAvailable(Foxtrot.class);
    }

    @Test
    public void testNotBeanArchiveExtension(LegacyAlpha legacyAlpha) {
        assertDiscoveredAndAvailable(legacyAlpha, LegacyAlpha.class);
        assertNotDiscoveredAndNotAvailable(LegacyBravo.class);
    }

    private <T extends Ping> void assertDiscoveredAndAvailable(T reference, Class<T> clazz) {
        Assert.assertTrue(extension.getObservedAnnotatedTypes().contains(clazz));
        Assert.assertNotNull(reference);
        reference.pong();
        manager.resolve(manager.getBeans(clazz));
    }

    private <T> void assertNotDiscoveredAndNotAvailable(Class<T> clazz) {
        Assert.assertFalse(extension.getObservedAnnotatedTypes().contains(clazz));
        Assert.assertTrue(manager.getBeans(clazz).isEmpty());
    }

    private static StringAsset newBeans11Descriptor(String mode) {
        return new StringAsset("<beans bean-discovery-mode=\"" + mode + "\" version=\"1.1\"/>");
    }

}
