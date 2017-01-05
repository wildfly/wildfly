package org.jboss.as.test.integration.management.deploy.runtime;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.deployment.EJBComponentType;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleStatefulHome;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleInterface;
import org.jboss.as.test.integration.ejb.home.remotehome.annotation.SimpleStatefulBean;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class StatefulEJBRemoteHomeRuntimeNameTestCase extends AbstractRuntimeTestCase {

    private static Logger log = Logger.getLogger(StatefulEJBRemoteHomeRuntimeNameTestCase.class);

    private static final String EJB_TYPE = EJBComponentType.STATEFUL.getResourceType();
    private static final Package BEAN_PACKAGE = SimpleStatefulHome.class.getPackage();
    private static final Class<?> BEAN_CLASS = SimpleStatefulBean.class;

    private static final String RT_MODULE_NAME = "nooma-nooma1-" + EJB_TYPE;
    private static final String RT_NAME = RT_MODULE_NAME + ".ear";
    private static final String DEPLOYMENT_MODULE_NAME = "test1-" + EJB_TYPE + "-test";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_MODULE_NAME + ".ear";
    private static final String SUB_DEPLOYMENT_MODULE_NAME = "ejb";
    private static final String SUB_DEPLOYMENT_NAME = SUB_DEPLOYMENT_MODULE_NAME + ".jar";
    private static ModelControllerClient controllerClient = TestSuiteEnvironment.getModelControllerClient();

    private static InitialContext context;

    @BeforeClass
    public static void setup() throws Exception {
        context = getInitialContext();
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, SUB_DEPLOYMENT_NAME);
        ejbJar.addPackage(BEAN_PACKAGE);
        ejbJar.addClass(BEAN_CLASS);

        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_NAME);
        earArchive.addAsModule(ejbJar);

        ModelNode addDeploymentOp = new ModelNode();
        addDeploymentOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        addDeploymentOp.get(ModelDescriptionConstants.CONTENT).get(0).get(ModelDescriptionConstants.INPUT_STREAM_INDEX).set(0);
        addDeploymentOp.get(ModelDescriptionConstants.RUNTIME_NAME).set(RT_NAME);
        addDeploymentOp.get(ModelDescriptionConstants.AUTO_START).set(true);
        ModelNode deployOp = new ModelNode();
        deployOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.DEPLOY);
        deployOp.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        deployOp.get(ModelDescriptionConstants.ENABLED).set(true);
        ModelNode[] steps = new ModelNode[2];
        steps[0] = addDeploymentOp;
        steps[1] = deployOp;
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(earArchive.as(ZipExporter.class).exportAsInputStream());

        ModelNode result = controllerClient.execute(ob.build());

        // just to blow up
        Assert.assertTrue("Failed to deploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        safeClose(context);
        ModelNode result = controllerClient.execute(composite(
                undeploy(DEPLOYMENT_NAME),
                remove(DEPLOYMENT_NAME)
        ));

        // just to blow up
        Assert.assertTrue("Failed to undeploy: " + result, Operations.isSuccessfulOutcome(result));
    }

    private static InitialContext getInitialContext() throws NamingException {
        final Hashtable env = new Hashtable();
        env.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote+http://" + TestSuiteEnvironment.getServerAddress() + ":" + 8080);
        return new InitialContext(env);
    }

    private static void safeClose(InitialContext context) {
        if (context == null) {
            return;
        }
        try {
            context.close();
        } catch (Throwable t) {
            // just log
            log.trace("Ignoring a problem which occurred while closing: " + context, t);
        }
        context = null;
    }

    private String getEJBHomeJNDIBinding() {

        final String appName = RT_MODULE_NAME;
        final String moduleName = SUB_DEPLOYMENT_MODULE_NAME;
        final String distinctName = "";
        final String beanName = BEAN_CLASS.getSimpleName();
        final String viewClassName = SimpleStatefulHome.class.getName();
        return "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName;
    }

    @Test
    @InSequence(value = 1)
    public void testGetEjbHome() throws Exception {
        Object home = context.lookup(getEJBHomeJNDIBinding());
        Assert.assertTrue(home instanceof SimpleStatefulHome);

    }

    @Test
    @InSequence(value = 2)
    public void testStatefulLocalHome() throws Exception {
        SimpleStatefulHome home = (SimpleStatefulHome) context.lookup(getEJBHomeJNDIBinding());
        SimpleInterface ejbInstance = home.createSimple("Hello World");
        Assert.assertEquals("Hello World", ejbInstance.sayHello());
        home = (SimpleStatefulHome) ejbInstance.getEJBHome();
        ejbInstance = home.createSimple("Hello World");
        Assert.assertEquals("Hello World", ejbInstance.sayHello());
    }

    @Test
    @InSequence(value = 3)
    public void testStepByStep() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        ModelNode result = controllerClient.execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBDEPLOYMENT, SUB_DEPLOYMENT_NAME);
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));

        readResource.get(ModelDescriptionConstants.ADDRESS).add(EJB_TYPE, BEAN_CLASS.getSimpleName());
        result = controllerClient.execute(readResource);
        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }

    @Test
    @InSequence(value = 4)
    public void testRecursive() throws Exception {

        ModelNode readResource = new ModelNode();
        readResource.get(ModelDescriptionConstants.ADDRESS).add(ModelDescriptionConstants.DEPLOYMENT, DEPLOYMENT_NAME);
        readResource.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);
        readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
        ModelNode result = controllerClient.execute(readResource);

        // just to blow up
        Assert.assertTrue("Failed to list resources: " + result, Operations.isSuccessfulOutcome(result));
    }
}
