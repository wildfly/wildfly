package org.jboss.as.jacorb;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ValueExpression;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 * */
public class TransformUtilsTestCase {

    @Test
    public void testTransformation() {
        ModelNode legacyModel = new ModelNode();
        legacyModel.get("name").set("test");
        legacyModel.get("giop-minor-version").set(2);
        legacyModel.get("transactions").set("on");
        legacyModel.get("security").set("off");
        legacyModel.get("export-corbaloc").set("on");
        legacyModel.get("support-ssl").set("off");
        legacyModel.get("sun").set("on");
        legacyModel.get("comet").set("off");
        legacyModel.get("iona").set("on");
        ModelNode newModel = TransformUtils.transformModel(legacyModel);
        Assert.assertTrue(newModel.get("name").equals(new ModelNode("test")));
        Assert.assertTrue(newModel.get("giop-version").equals(new ModelNode("1.2")));
        Assert.assertTrue(newModel.get("security").equals(new ModelNode("none")));
        Assert.assertTrue(newModel.get("transactions").equals(new ModelNode("full")));
        Assert.assertTrue(newModel.get("export-corbaloc").equals(new ModelNode(true)));
        Assert.assertTrue(newModel.get("support-ssl").equals(new ModelNode(false)));
        Assert.assertTrue(newModel.get("iona").equals(new ModelNode(true)));
    }

    @Test
    public void testExpressions() {
        ModelNode legacyModel = new ModelNode();
        legacyModel.get("name").set(new ValueExpression("${name}"));
        legacyModel.get("giop-minor-version").set(new ValueExpression("${giop.minor.version:2}"));
        ModelNode newModel = TransformUtils.transformModel(legacyModel);
        Assert.assertTrue(newModel.get("name").equals(new ModelNode(new ValueExpression("${name}"))));
        Assert.assertTrue(newModel.get("giop-version").equals(new ModelNode(new ValueExpression("${giop.minor.version:1.2}"))));
    }

    @Test
    public void testRejectedOnOffAttributeTurnedOff() throws Exception {
        ModelNode model = new ModelNode();
        model.get("monitoring").set("off");
        List<String> result =TransformUtils.validateDeprecatedProperites(model);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testRejectedOnOffAttribute() throws Exception {
        ModelNode model = new ModelNode();
        model.get("monitoring").set("on");
        List<String> result = TransformUtils.validateDeprecatedProperites(model);
        Assert.assertFalse(result.isEmpty());
    }

    @Test
    public void testRejectedAttribute() throws Exception {
        ModelNode model = new ModelNode();
        model.get("queue-min").set(5);
        List<String> result = TransformUtils.validateDeprecatedProperites(model);
        Assert.assertFalse(result.isEmpty());
    }

}
