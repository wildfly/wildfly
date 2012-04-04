package org.jboss.as.controller.transform;

import junit.framework.Assert;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.LegacyResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModelDescriptionTestCase {

    private static ManagementResourceRegistration registration;

    @BeforeClass
    public static void setup() {
        registration = ManagementResourceRegistration.Factory.create(RootSubsystemResource.INSTANCE);
        registration.registerSubModel(SessionDefinition.INSTANCE);

    }

    @AfterClass
    public static void tearDown() {
        registration = null;
    }

    @Test
    public void testManagementResourceSerialization() {
        ModelNode model = SubsystemDescriptionDump.readFullModelDescription(PathAddress.EMPTY_ADDRESS, registration);
        ResourceDefinition definition = new LegacyResourceDefinition(model);
        ManagementResourceRegistration loaded = ManagementResourceRegistration.Factory.create(definition);
        validate(registration, loaded);


        List<TransformRule> rules1 = ModelMatcher.getRules(registration, loaded);
        List<TransformRule> rules2 = ModelMatcher.getRules(loaded, loaded);
        Assert.assertEquals(rules1, rules2);

    }

    private static void validate(ManagementResourceRegistration orig, ManagementResourceRegistration loaded) {
        Assert.assertEquals(orig.getChildAddresses(PathAddress.EMPTY_ADDRESS).size(), loaded.getChildAddresses(PathAddress.EMPTY_ADDRESS).size());

        Assert.assertEquals(orig.getAttributeNames(PathAddress.EMPTY_ADDRESS).size(), loaded.getAttributeNames(PathAddress.EMPTY_ADDRESS).size());
        for (String name : orig.getAttributeNames(PathAddress.EMPTY_ADDRESS)) {
            AttributeDefinition attr1 = orig.getAttributeAccess(PathAddress.EMPTY_ADDRESS, name).getAttributeDefinition();
            AttributeDefinition attr2 = loaded.getAttributeAccess(PathAddress.EMPTY_ADDRESS, name).getAttributeDefinition();
            Assert.assertEquals(1d, SimilarityIndex.compareAttributes(attr1, attr2));
        }
        for (PathElement pe : orig.getChildAddresses(PathAddress.EMPTY_ADDRESS)) {
            ManagementResourceRegistration origSub = orig.getSubModel(PathAddress.pathAddress(pe));
            ManagementResourceRegistration loadedSub = loaded.getSubModel(PathAddress.pathAddress(pe));
            validate(origSub, loadedSub);
        }
    }

    @Test
    public void testSimilarity() {
        double factor = SimilarityIndex.compareStrings("test", "testa");
        double factor2 = SimilarityIndex.compareStrings("modelController", "model-controller");

    }
}
