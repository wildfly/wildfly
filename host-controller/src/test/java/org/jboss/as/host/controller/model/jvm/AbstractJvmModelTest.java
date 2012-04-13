/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.host.controller.model.jvm;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import junit.framework.Assert;

import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractJvmModelTest extends ManagementTestSetup {

    private final boolean server;

    protected AbstractJvmModelTest(boolean server) {
        this.server = server;
    }

    @Test
    public void testReadResourceDescription() throws Exception {
        //Just make sure we can read it all
        ModelNode op = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        op.get(RECURSIVE).set(true);
        executeForResult(op);
    }

    @Test
    public void testEmptyAddSubsystem() throws Exception {
        ModelNode resource = readModel(true);
        Assert.assertFalse(resource.get(JVM).hasDefined("test"));

        ModelNode op = createOperation(ADD);
        executeForResult(op);

        resource = readModel(true);
        Assert.assertTrue(resource.get(JVM).hasDefined("test"));
        Assert.assertTrue(resource.get(JVM, "test").keys().size() > 0);
        for (String key : resource.get(JVM, "test").keys()) {
            Assert.assertFalse(resource.get(JVM, "test").hasDefined(key));
        }
    }

    @Test
    public void testWriteType() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("IBM");
        Assert.assertEquals(value, writeTest("type", value));
    }

    @Test
    public void testWriteAgentLib() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("agent-lib", value));
    }

    @Test
    public void testWriteAgentPath() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("agent-path", value));
    }

    @Test
    public void testWriteEnvClasspathIgnored() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode(true);
        Assert.assertEquals(value, writeTest("env-classpath-ignored", value));
    }

    @Test
    public void testWriteJavaAgent() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("java-agent", value));
    }

    @Test
    public void testWriteJavaHome() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("java-home", value));
    }

    @Test
    public void testWriteStackSize() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("stack-size", value));
    }

    @Test
    public void testWriteHeapSize() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("heap-size", value));
    }

    @Test
    public void testWriteMaxHeapSize() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("max-heap-size", value));
    }

    @Test
    public void testWritePermGenSize() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("permgen-size", value));
    }

    @Test
    public void testWriteMaxPermGenSize() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest("max-permgen-size", value));
    }

    @Test
    public void testWriteBadType() throws Exception {
        testEmptyAddSubsystem();
        ModelNode op = createWriteAttributeOperation("type", new ModelNode("XXX"));
        executeForFailure(op);
    }

    @Test
    public void testWriteJvmOptions() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode().add("-Xmx100m").add("-Xms30m");
        Assert.assertEquals(value, writeTest("jvm-options", value));
    }

    @Test
    public void testWriteEnvironmentVariables() throws Exception {
        testEmptyAddSubsystem();
        ModelNode value = new ModelNode().add("ENV1", "one").add("ENV2", "two");
        Assert.assertEquals(value, writeTest("environment-variables", value));
    }

    @Test
    public void testFullAdd() throws Exception {
        ModelNode typeValue = new ModelNode("IBM");
        ModelNode agentLibValue = new ModelNode("agentLib");
        ModelNode envClasspathIgnored = new ModelNode(true);
        ModelNode javaAgentValue = new ModelNode("javaAgent");
        ModelNode javaHomeValue = new ModelNode("javaHome");
        ModelNode stackSizeValue = new ModelNode("stackSize");
        ModelNode heapSizeValue = new ModelNode("heapSize");
        ModelNode maxHeapSizeValue = new ModelNode("maxHeapSize");
        ModelNode permgenSizeValue = new ModelNode("permGenSize");
        ModelNode maxPermSizeValue = new ModelNode("maxPermSize");
        ModelNode jvmOptionsValue = new ModelNode().add("-Xmx100m").add("-Xms30m");
        ModelNode environmentVariablesValue = new ModelNode().add("ENV1", "one").add("ENV2", "two");

        ModelNode debugEnabledValue = new ModelNode(true);
        ModelNode debugOptionsValue = new ModelNode("debugOptions");

        ModelNode op = createOperation(ADD);
        op.get("type").set(typeValue);
        op.get("agent-lib").set(agentLibValue);
        op.get("env-classpath-ignored").set(envClasspathIgnored);
        op.get("java-agent").set(javaAgentValue);
        op.get("java-home").set(javaHomeValue);
        op.get("stack-size").set(stackSizeValue);
        op.get("heap-size").set(heapSizeValue);
        op.get("max-heap-size").set(maxHeapSizeValue);
        op.get("permgen-size").set(permgenSizeValue);
        op.get("max-permgen-size").set(maxPermSizeValue);
        op.get("jvm-options").set(jvmOptionsValue);
        op.get("environment-variables").set(environmentVariablesValue);
        if (server) {
            op.get("debug-enabled").set(debugEnabledValue);
            op.get("debug-options").set(debugOptionsValue);
        }

        executeForResult(op);

        ModelNode resource = readModel(true).get(JVM, "test");

        Assert.assertEquals(typeValue, resource.get("type"));
        Assert.assertEquals(agentLibValue, resource.get("agent-lib"));
        Assert.assertEquals(envClasspathIgnored, resource.get("env-classpath-ignored"));
        Assert.assertEquals(javaAgentValue, resource.get("java-agent"));
        Assert.assertEquals(javaHomeValue, resource.get("java-home"));
        Assert.assertEquals(stackSizeValue, resource.get("stack-size"));
        Assert.assertEquals(heapSizeValue, resource.get("heap-size"));
        Assert.assertEquals(maxHeapSizeValue, resource.get("max-heap-size"));
        Assert.assertEquals(permgenSizeValue, resource.get("permgen-size"));
        Assert.assertEquals(maxPermSizeValue, resource.get("max-permgen-size"));
        Assert.assertEquals(jvmOptionsValue, resource.get("jvm-options"));
        Assert.assertEquals(environmentVariablesValue, resource.get("environment-variables"));
        if (server) {
            Assert.assertEquals(debugEnabledValue, resource.get("debug-enabled"));
            Assert.assertEquals(debugOptionsValue, resource.get("debug-options"));
        }
    }

    @Test
    public void testAddSameJvmOption() throws Exception {
        testEmptyAddSubsystem();
        executeForResult(createAddJvmOptionOperation("-Xoption"));
        Assert.assertEquals(new ModelNode().add("-Xoption"), readModel(true).get(JVM, "test", "jvm-options"));
        executeForFailure(createAddJvmOptionOperation("-Xoption"));
        Assert.assertEquals(new ModelNode().add("-Xoption"), readModel(true).get(JVM, "test", "jvm-options"));
    }

    @Test
    public void testRemoveJvmOption() throws Exception {
        testEmptyAddSubsystem();
        executeForResult(createAddJvmOptionOperation("-Xoption1"));
        executeForResult(createAddJvmOptionOperation("-Xoption2"));
        executeForResult(createAddJvmOptionOperation("-Xoption3"));
        ModelNode resource = readModel(true);
        Assert.assertEquals(new ModelNode().add("-Xoption1").add("-Xoption2").add("-Xoption3"), resource.get(JVM, "test", "jvm-options"));

        executeForResult(createRemoveJvmOptionOperation("-Xoption2"));
        Assert.assertEquals(new ModelNode().add("-Xoption1").add("-Xoption3"), readModel(true).get(JVM, "test", "jvm-options"));
        executeForResult(createRemoveJvmOptionOperation("-Xoption2"));
        Assert.assertEquals(new ModelNode().add("-Xoption1").add("-Xoption3"), readModel(true).get(JVM, "test", "jvm-options"));
        executeForResult(createRemoveJvmOptionOperation("-Xoption1"));
        Assert.assertEquals(new ModelNode().add("-Xoption3"), readModel(true).get(JVM, "test", "jvm-options"));
        executeForResult(createRemoveJvmOptionOperation("-Xoption3"));
        Assert.assertEquals(new ModelNode().setEmptyList(), readModel(true).get(JVM, "test", "jvm-options"));
    }

    //AS7-4437 is scheduled for 7.2.0 so uncomment these once we have decided on the format of the operation names
    //There are some tests in AbstractJvmModelTest for these which need uncommenting as well
    /*
    @Test
    public void testAddSameEnvironmentVariable() throws Exception {
        testEmptyAddSubsystem();
        executeForResult(createAddEnvVarOperation("ONE", "uno"));
        Assert.assertEquals(new ModelNode().add("ONE", "uno"), readModel(true).get(JVM, "test", "environment-variables"));
        executeForFailure(createAddEnvVarOperation("ONE", "uno"));
        Assert.assertEquals(new ModelNode().add("ONE", "uno"), readModel(true).get(JVM, "test", "environment-variables"));
    }

    @Test
    public void testRemoveEnvironmentVariable() throws Exception {
        testEmptyAddSubsystem();
        executeForResult(createAddEnvVarOperation("ONE", "uno"));
        executeForResult(createAddEnvVarOperation("TWO", "dos"));
        executeForResult(createAddEnvVarOperation("THREE", "tres"));
        Assert.assertEquals(new ModelNode().add("ONE", "uno").add("TWO", "dos").add("THREE", "tres"), readModel(true).get(JVM, "test", "environment-variables"));
        executeForResult(createRemoveEnvVarOperation("TWO"));
        Assert.assertEquals(new ModelNode().add("ONE", "uno").add("THREE", "tres"), readModel(true).get(JVM, "test", "environment-variables"));
        executeForResult(createRemoveEnvVarOperation("ONE"));
        Assert.assertEquals(new ModelNode().add("THREE", "tres"), readModel(true).get(JVM, "test", "environment-variables"));
        executeForResult(createRemoveEnvVarOperation("THREE"));
        Assert.assertEquals(new ModelNode().setEmptyList(), readModel(true).get(JVM, "test", "environment-variables"));
    }*/


    protected ModelNode createWriteAttributeOperation(String name, ModelNode value) {
        ModelNode op = createOperation(WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set(name);
        op.get(VALUE).set(value);
        return op;
    }

    protected ModelNode createAddEnvVarOperation(String key, String value) {
        ModelNode op = createOperation(JVMEnvironmentVariableAddHandler.OPERATION_NAME);
        op.get(NAME).set(key);
        op.get(VALUE).set(value);
        return op;
    }

    protected ModelNode createRemoveEnvVarOperation(String key) {
        ModelNode op = createOperation(JVMEnvironmentVariableRemoveHandler.OPERATION_NAME);
        op.get("name").set(key);
        return op;
    }


    protected ModelNode createAddJvmOptionOperation(String option) {
        ModelNode op = createOperation("add-jvm-option");
        op.get("jvm-option").set(option);
        return op;
    }

    protected ModelNode createRemoveJvmOptionOperation(String option) {
        ModelNode op = createOperation("remove-jvm-option");
        op.get("jvm-option").set(option);
        return op;
    }

    protected ModelNode writeTest(String name, ModelNode value) throws Exception {
        executeForResult(createWriteAttributeOperation(name, value));

        ModelNode resource = readModel(true);

        Assert.assertTrue(resource.get(JVM).hasDefined("test"));
        Assert.assertTrue(resource.get(JVM, "test").keys().size() > 0);
        for (String key : resource.get(JVM, "test").keys()) {
            boolean isApartFrom = key.equals(name);
            if (!isApartFrom) {
                Assert.assertFalse(resource.get(JVM, "test").hasDefined(key));
            } else {
                Assert.assertTrue(resource.get(JVM, "test").hasDefined(key));
            }
        }

        return resource.get(JVM, "test", name);
    }

    private ModelNode createOperation(String name) {
        return super.createOperation(name, JVM, "test");
    }
}
