package org.jboss.as.test.integration.picketlink.core;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.Identity;
import org.picketlink.annotations.PicketLink;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
public class PartitionManagerProducerTestCase {

    public static final String CONFIGURATION_NAME = "partition.manager.producer";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                   .create(WebArchive.class, "test.war")
                   .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                   .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.core meta-inf,org.picketlink.core.api meta-inf, org.picketlink.idm meta-inf\n"), "MANIFEST.MF")
                   .addClass(PartitionManagerProducerTestCase.class);
    }

    @Inject
    private PartitionManager partitionManager;

    @Inject
    private IdentityManager identityManager;

    @Inject
    private RelationshipManager relationshipManager;

    @Inject
    private Identity identity;

    @Inject
    private DefaultLoginCredentials credentials;

    @Test
    @InSequence(1)
    public void testProducedPartitionManagerInstance() {
        assertEquals(1, this.partitionManager.getConfigurations().size());

        IdentityConfiguration configuration = this.partitionManager.getConfigurations().iterator().next();

        assertEquals(CONFIGURATION_NAME, configuration.getName());
    }

    @Test
    @InSequence(2)
    public void testAuthentication() {
        User user = new User("johny");

        this.identityManager.add(user);

        Password password = new Password("abcd1234");

        this.identityManager.updateCredential(user, password);

        Role role = new Role("admin");

        this.identityManager.add(role);

        BasicModel.grantRole(this.relationshipManager, user, role);

        this.credentials.setUserId("johny");
        this.credentials.setPassword("abcd1234");

        this.identity.login();

        assertTrue(this.identity.isLoggedIn());

        user = BasicModel.getUser(this.identityManager, "johny");
        role = BasicModel.getRole(this.identityManager, "admin");

        assertTrue(BasicModel.hasRole(this.relationshipManager, user, role));
    }

    static class PartitionManagerProducer {

        @Produces
        @PicketLink
        public PartitionManager getPartitionManager() {
            IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

            builder
                .named(CONFIGURATION_NAME)
                    .stores()
                        .file()
                            .preserveState(false)
                            .supportAllFeatures();

            DefaultPartitionManager partitionManager = new DefaultPartitionManager(builder.buildAll());

            partitionManager.add(new Realm(Realm.DEFAULT_REALM));

            return partitionManager;
        }
    }
}
