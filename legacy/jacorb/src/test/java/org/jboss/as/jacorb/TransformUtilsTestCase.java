package org.jboss.as.jacorb;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

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
        ModelNode newModel = TransformUtils.transformModel(legacyModel);
        System.out.println(newModel);
        Assert.assertTrue(newModel.get("name").equals(new ModelNode("test")));
        Assert.assertTrue(newModel.get("giop-version").equals(new ModelNode("1.2")));
        Assert.assertTrue(newModel.get("security").equals(new ModelNode("none")));
        Assert.assertTrue(newModel.get("transactions").equals(new ModelNode("full")));
        Assert.assertTrue(newModel.get("export-corbaloc").equals(new ModelNode(true)));
        Assert.assertTrue(newModel.get("support-ssl").equals(new ModelNode(false)));
    }

    @Test
    public void testRejectedOnOffAttributeTurnedOff() throws Exception {
        ModelNode model = new ModelNode();
        model.get("iona").set("off");
        TransformUtils.checkLegacyModel(model, true);
    }

    @Test(expected = OperationFailedException.class)
    public void testRejectedOnOffAttribute() throws Exception {
        ModelNode model = new ModelNode();
        model.get("iona").set("on");
        TransformUtils.checkLegacyModel(model, true);
    }

    @Test(expected = OperationFailedException.class)
    public void testRejectedAttribute() throws Exception {
        ModelNode model = new ModelNode();
        model.get("queue-min").set(5);
        TransformUtils.checkLegacyModel(model, true);
    }

}
