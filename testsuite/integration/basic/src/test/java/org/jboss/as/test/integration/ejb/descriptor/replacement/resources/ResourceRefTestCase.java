/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.resources;

import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rhatlapa
 */
@RunWith(Arquillian.class)
@ServerSetup(CreateQueueSetupTask.class)
public class ResourceRefTestCase {

    private static final Logger log = Logger.getLogger(ResourceRefTestCase.class);
    
    @ArquillianResource
    private InitialContext ctx;
    
    private static final String DEPLOYMENT_JBOSS_SPEC_ONLY = "jboss-spec";
    private static final String DEPLOYMENT_WITH_REDEFINITION = "ejb3-specVsJboss-spec";

    /**
     * deploys with both EJB specific descriptor and JBoss specific descriptor
     *
     * @return
     */
    @Deployment(name = DEPLOYMENT_WITH_REDEFINITION)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "resource-ref-test.jar");
        jar.addClasses(ResourceDrivenBean.class, CreateQueueSetupTask.class, ResourceRefTestCase.class);
        jar.addAsManifestResource(ResourceRefTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(ResourceRefTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    /**
     * deploys only with JBoss specific descriptor
     *
     * @return
     */
    @Deployment(name = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public static Archive<?> deploymentJbossSpec() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "resource-ref-test-jboss-spec.jar");
        jar.addClasses(ResourceDrivenBean.class, CreateQueueSetupTask.class, ResourceRefTestCase.class);
        jar.addAsManifestResource(ResourceRefTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testDescriptorSetOfEntriesJbossSpec() throws NamingException {
        testDescriptorSetOfEntries();
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
    public void testDescriptorSetOfEntriesWithJbossSpecRedefinition() throws NamingException {
        testDescriptorSetOfEntries();
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_JBOSS_SPEC_ONLY)
    public void testDescriptorSetOfResourceJbossSpec() throws NamingException, JMSException {
        testDescriptorSetOfResource();
    }

    @Test
    @OperateOnDeployment(value = DEPLOYMENT_WITH_REDEFINITION)
    public void testDescriptorSetOfResourceWithJbossSpecRedefinition() throws NamingException, JMSException {
        testDescriptorSetOfResource();
    }

    /**
     * Tests which value is set in textResource by descriptor (ejb-jar sets the text resource to:
     * Hello ejb3-spec; jboss-ejb3 sets it to: Hello jboss-spec)
     *
     * @throws NamingException
     */
    private void testDescriptorSetOfEntries() throws NamingException {
        final ResourceDrivenBean bean = (ResourceDrivenBean) ctx.lookup("java:module/ResourceDrivenBean");
        Assert.assertEquals("Hello jboss-spec", bean.getTextResource());
    }

    /**
     * Tests which queue as resource is set by descriptor (ejb-jar sets it to myAwesomeQueue2;
     * jboss-ejb3 sets it to myAwesomeQueue)
     *
     * @throws NamingException
     * @throws JMSException
     */
    private void testDescriptorSetOfResource() throws NamingException, JMSException {
        final ResourceDrivenBean bean = (ResourceDrivenBean) ctx.lookup("java:module/ResourceDrivenBean");
        Assert.assertEquals("myAwesomeQueue", bean.getQueueName());
    }
}
