package org.jboss.as.test.integration.jca;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;

public  class JcaMgmtBase extends  ContainerResourceMgmtTestBase {
    
    
    protected static ModelNode subsystemAddress=new ModelNode().add(SUBSYSTEM, "jca");
    
    protected static ModelNode archiveValidationAddress=subsystemAddress.clone().add("archive-validation","archive-validation");
    
    
    public void reload() throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        executeOperation(operation);
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(5000);
                if (getManagementClient().isServerInRunningState())
                    reloaded = true;
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 10)
                    throw new Exception("Server reloading failed");
            }
        }
    }
    
    public ModelNode readAttribute(ModelNode address, String attributeName) throws Exception{
        ModelNode op= new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        op.get(OP_ADDR).set(address);
        return executeOperation(op);
    }
    
    public ModelNode writeAttribute(ModelNode address, String attributeName, String attributeValue) throws Exception{
        ModelNode op= new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attributeName);
        op.get(VALUE).set(attributeValue);
        op.get(OP_ADDR).set(address);
        return executeOperation(op);
    }
    
    public void setArchiveValidation(boolean enabled,boolean failOnErr,boolean failOnWarn) throws Exception{
        
        remove(archiveValidationAddress);
        ModelNode op= new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).set(archiveValidationAddress);
        op.get("enabled").set(enabled);
        op.get("fail-on-error").set(failOnErr);
        op.get("fail-on-warn").set(failOnWarn);
        executeOperation(op);
        reload();
    }
    
    public boolean getArchiveValidationAttribute(String attributeName) throws Exception{
        return readAttribute(archiveValidationAddress, attributeName).asBoolean();
    }
}
