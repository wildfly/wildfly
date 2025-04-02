/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import java.util.PropertyPermission;

import jakarta.inject.Inject;
import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.testing.tools.deployments.DeploymentDescriptors;

/**
 * Tests that an entity callbacks, e.g. {@link jakarta.persistence.PostPersist}, work with Jakarta Context and Dependency
 * Injection.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractManagedInjectionTestCase {
    private static final String PERSISTENCE_XML = """
            <persistence xmlns="https://jakarta.ee/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                                             https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
                version="3.0">
                <persistence-unit name="test">
                    <jta-data-source>%s</jta-data-source>
                    <properties>
                        <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create" />
                        %s
                    </properties>
                </persistence-unit>
            </persistence>
            """;

    @Inject
    private UserRegistry userRegistry;

    @Inject
    private UserCollector collector;

    @Before
    public void cleanup() {
        collector.clear();
    }

    static WebArchive createDeployment() {
        return createDeploymentWithPersistenceXml(String.format(PERSISTENCE_XML, "java:comp/DefaultDataSource", ""));
    }

    static WebArchive createDeployment(final boolean enhance) {
        final String property = String.format("<property name=\"jboss.as.jpa.classtransformer\" value=\"%s\" />", enhance);
        final String persistenceXml = String.format(PERSISTENCE_XML, "java:comp/DefaultDataSource", property);
        return createDeploymentWithPersistenceXml(persistenceXml);
    }

    static WebArchive createDeployment(final String jndiName) {
        final String persistenceXml = String.format(PERSISTENCE_XML, jndiName, "");
        return createDeploymentWithPersistenceXml(persistenceXml);
    }

    static WebArchive createDeployment(final String jndiName, final boolean enhance) {
        final String property = String.format("<property name=\"jboss.as.jpa.classtransformer\" value=\"%s\" />", enhance);
        final String persistenceXml = String.format(PERSISTENCE_XML, jndiName, property);
        return createDeploymentWithPersistenceXml(persistenceXml);
    }

    private static WebArchive createDeploymentWithPersistenceXml(final String persistenceXml) {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(
                        AbstractManagedInjectionTestCase.class,
                        AddDataSourceSetupTask.class,
                        EventType.class,
                        User.class,
                        UserCollector.class,
                        UserListener.class,
                        UserRegistry.class,
                        TimeoutUtil.class
                )
                .addAsWebInfResource(new StringAsset(persistenceXml), "classes/META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(DeploymentDescriptors.createPermissionsXmlAsset(
                        new PropertyPermission("ts.timeout.factor", "read"),
                        new HibernateValidatorPermission("accessPrivateMembers")
                ), "permissions.xml");
    }

    /**
     * Tests that the {@link jakarta.persistence.PrePersist} and {@link jakarta.persistence.PostPersist} methods
     * were invoked on the listener.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @InSequence(1)
    public void addUser() throws Exception {
        final User user = new User();
        user.setName("JBoss");
        user.setEmail("jboss@jboss.org");
        final User added = userRegistry.add(user);
        Assert.assertNotNull(added);
        Assert.assertNotNull(added.getId());
        compareUser(added, collector.pop(EventType.PRE_PERSIST));
        compareUser(added, collector.pop(EventType.POST_PERSIST));
    }

    /**
     * Tests that the {@link jakarta.persistence.PreUpdate} and {@link jakarta.persistence.PostUpdate} methods
     * were invoked on the listener.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @InSequence(2)
    public void updateUser() throws Exception {
        final User user = new User();
        user.setId(1L);
        user.setName("Changed");
        user.setEmail("changed@jboss.org");
        final User updated = userRegistry.update(user);
        compareUser(updated, collector.pop(EventType.PRE_UPDATE));
        compareUser(updated, collector.pop(EventType.POST_UPDATE));
    }

    /**
     * Tests that the {@link jakarta.persistence.PostLoad} method was invoked in the listener.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @InSequence(3)
    public void getUser() throws Exception {
        final User user = userRegistry.getUserById(1L);
        compareUser(user, collector.pop(EventType.POST_LOAD));
    }

    /**
     * Tests that the {@link jakarta.persistence.PreRemove} and {@link jakarta.persistence.PostRemove} methods
     * were invoked on the listener.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @InSequence(4)
    public void removeUser() throws Exception {
        final User removed = userRegistry.remove(1L);
        compareUser(removed, collector.pop(EventType.PRE_REMOVE));
        compareUser(removed, collector.pop(EventType.POST_REMOVE));
        // The way the remove works is it first loads the entity, then removes it. We may as well check this was invoked.
        compareUser(removed, collector.pop(EventType.POST_LOAD));
    }

    private void compareUser(final User expected, final User actual) {
        Assert.assertNotNull("The expected user cannot be null", expected);
        Assert.assertNotNull("The actual user cannot be null", actual);
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getEmail(), actual.getEmail());
    }
}
