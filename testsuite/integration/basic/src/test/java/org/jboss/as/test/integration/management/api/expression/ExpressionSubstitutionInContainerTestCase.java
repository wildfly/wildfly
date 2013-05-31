/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.api.expression;

import javax.ejb.EJB;

import org.junit.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validation of the system property substitution for expressions handling. Test for AS7-6120.
 * Global parameters testing could be found in domain module: ExpressionSupportSmokeTestCase
 * 
 * The expression substitution test runs the evaluation of expressions in bean deployed in container. 
 * The managementClient injected by arquillian is taken via remote interface.
 * We need to operate directly with management client controller residing in container. 
 * It's provided by management service hack - {@link ExpressionTestManagementService} 
 * Maybe there will be an api for this in future: AS7-4657 
 * 
 * @author <a href="ochaloup@jboss.com">Ondrej Chaloupka</a> 
 */
@RunWith(Arquillian.class)
public class ExpressionSubstitutionInContainerTestCase {
    private static final Logger log = Logger.getLogger(ExpressionSubstitutionInContainerTestCase.class);
    
    private static final String ARCHIVE_NAME = "expression-substitution-test";
    
    private static final String PROP_NAME = "qa.test.property";
    private static final String PROP_DEFAULT_VALUE = "defaultValue";
    private static final String EXPRESSION_PROP_NAME = "qa.test.exp";
    private static final String EXPRESSION_PROP_VALUE = "expression.value";
    private static final String INNER_PROP_NAME = "qa.test.inner.property";
    private static final String INNER_PROP_DEFAULT_VALUE = "inner.value";
        
    @EJB(mappedName = "java:global/expression-substitution-test/StatelessBean")
    private IStatelessBean bean;
    
    @ArquillianResource
    private ManagementClient managementClient;
    
    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(ExpressionTestManagementService.class, Utils.class, ModelUtil.class, 
                IStatelessBean.class, StatelessBean.class);
        
        jar.addAsManifestResource(new StringAsset(ExpressionTestManagementService.class.getName()),
                "services/org.jboss.msc.service.ServiceActivator");
        jar.addAsManifestResource(new StringAsset(
                "Manifest-Version: 1.0\n" +
                "Class-Path: \n" +  // there has to be a spacer - otherwise you meet "java.io.IOException: invalid header field"     
                "Dependencies: org.jboss.msc,org.jboss.as.controller-client,org.jboss.as.controller,org.jboss.as.server, org.jboss.dmr\n"),
                "MANIFEST.MF");
        
        return jar;
    }
    
   
    /**
     *  <system-properties>
     *    <property name="qa.test.exp" value="expression.value"/>
     *    <property name="qa.test.property" value="${qa.test.exp:defaultValue}"/>
     * </system-properties>
     */
    @Test
    @InSequence(1)
    public void testPropertyDefinedFirst() {
        Utils.setProperty(EXPRESSION_PROP_NAME, EXPRESSION_PROP_VALUE, managementClient.getControllerClient());
        Utils.setProperty(PROP_NAME, "${" + EXPRESSION_PROP_NAME + ":" + PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
        try {
            expresionEvaluation();
        } finally {
            // removing tested properties
            Utils.removeProperty(EXPRESSION_PROP_NAME, managementClient.getControllerClient());
            Utils.removeProperty(PROP_NAME, managementClient.getControllerClient());
        }
    }

    /**
     *  <system-properties>
     *    <property name="qa.test.property" value="${qa.test.exp:defaultValue}"/>
     *    <property name="qa.test.exp" value="expression.value"/>
     * </system-properties>
     */
    @Ignore("AS7-6431")
    @Test
    @InSequence(2)
    public void testExpressionDefinedFirst() {
        Utils.setProperty(PROP_NAME, "${" + EXPRESSION_PROP_NAME + ":" + PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
        Utils.setProperty(EXPRESSION_PROP_NAME, EXPRESSION_PROP_VALUE, managementClient.getControllerClient());
        try {
            expresionEvaluation();
        } finally {
            // removing tested properties
            Utils.removeProperty(EXPRESSION_PROP_NAME, managementClient.getControllerClient());
            Utils.removeProperty(PROP_NAME, managementClient.getControllerClient()); 
        }
    }
    
    /**
     *  <system-properties>
     *    <property name="qa.test.property" value="${qa.test.exp:defaultValue}"/>
     * </system-properties>
     */
    @Test
    @InSequence(3)
    public void testSystemPropertyEvaluation() {
        // the system property has to be defined in the same VM as the container resides
        bean.addSystemProperty(EXPRESSION_PROP_NAME, EXPRESSION_PROP_VALUE);
        Utils.setProperty(PROP_NAME, "${" + EXPRESSION_PROP_NAME + ":" + PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
        
        try {
            systemPropertyEvaluation();
        } finally {
            // removing tested properties
            Utils.removeProperty(PROP_NAME, managementClient.getControllerClient());
        }
    }
    
    
    /**
     *  <system-properties>
     *    <property name="qa.test.property" value="${qa.test.exp:defaultValue}"/>
     * </system-properties>
     */
    @Test
    @InSequence(4)
    public void testSystemPropertyEvaluationSetAfterExpression() {
        Utils.setProperty(PROP_NAME, "${" + EXPRESSION_PROP_NAME + ":" + PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
        // the system property has to be defined in the same VM as the container resides
        bean.addSystemProperty(EXPRESSION_PROP_NAME, EXPRESSION_PROP_VALUE);
        
        try {
            systemPropertyEvaluation();
        } finally {
            // removing tested properties
            Utils.removeProperty(PROP_NAME, managementClient.getControllerClient());
        }
    }
    
    private void systemPropertyEvaluation() {
        // test resolution of expressions        
        String result = bean.getJBossProperty(PROP_NAME);
        log.infof("systemPropertyEvaluation: JBoss property %s was resolved to %s", PROP_NAME, result);
        Assert.assertEquals("jboss property " + PROP_NAME + " evaluation - value should be taken from system property", EXPRESSION_PROP_VALUE, result);
        

        result = bean.getSystemProperty(EXPRESSION_PROP_NAME);
        log.infof("systemPropertyEvaluationsystemPropertyEvaluation: System property %s has value %s", EXPRESSION_PROP_NAME, result);
        Assert.assertEquals("system property " + EXPRESSION_PROP_NAME + " from directly defined system property", EXPRESSION_PROP_VALUE, result);
        
        result = bean.getSystemProperty(PROP_NAME);
        log.infof("systemPropertyEvaluation:  System property %s has value %s", PROP_NAME, result);
        Assert.assertEquals("system property " + PROP_NAME + " from evaluated jboss property", EXPRESSION_PROP_VALUE, result);
    }
    
    /**
      *  <system-properties>
      *     <property name="qa.test.exp" value="expression.value"/>
      *     <property name="qa.test.inner.property" value="${qa.test.exp:inner.value}"/>
      *     <property name="qa.test.property" value="${qa.test.inner.property:defaultValue}"/>
      *  </system-properties>
     */
    @Test
    @InSequence(5)
    public void testMultipleLevelExpression() {
        Utils.setProperty(EXPRESSION_PROP_NAME, EXPRESSION_PROP_VALUE, managementClient.getControllerClient());
        Utils.setProperty(INNER_PROP_NAME, "${" + EXPRESSION_PROP_NAME + ":" + INNER_PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
        Utils.setProperty(PROP_NAME, "${" + INNER_PROP_NAME + ":" + PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
                
        try {
            // evaluation the inner prop name in addition
            String result = bean.getJBossProperty(INNER_PROP_NAME);
            log.infof("expressionEvaluation: JBoss property %s was resolved to %s", INNER_PROP_NAME, result);
            Assert.assertEquals("jboss property " + INNER_PROP_NAME + " substitution evaluation expected", EXPRESSION_PROP_VALUE, result);
    
            result = bean.getSystemProperty(INNER_PROP_NAME);
            log.infof("expressionEvaluation: System property %s has value %s", INNER_PROP_NAME, result);
            Assert.assertEquals("system property " + INNER_PROP_NAME + " from substitued jboss property", EXPRESSION_PROP_VALUE, result);
            
            // then evaluation of the rest
            expresionEvaluation();
        } finally {
            // removing tested properties
            Utils.removeProperty(EXPRESSION_PROP_NAME, managementClient.getControllerClient());
            Utils.removeProperty(PROP_NAME, managementClient.getControllerClient()); 
            Utils.removeProperty(INNER_PROP_NAME, managementClient.getControllerClient());
        }
    }
    
    /**
     *  <system-properties>
     *    <property name="first.defined.value" value="expression.value"/>
     *    <property name="qa.test.property" value="${qa.test.exp:defaultValue}"/>
     * </system-properties>
     * 
     * Write attribute set:
     * <property name="qa.test.exp" value="expression.value"/>
     */
    @Ignore("AS7-6431")  // for this test works there will be :reload after redefinition
    @Test
    @InSequence(6)
    public void testRedefinitionExpressionValue() {
        Utils.setProperty(EXPRESSION_PROP_NAME, "firstly.defined.value.", managementClient.getControllerClient());
        Utils.setProperty(PROP_NAME, "${" + EXPRESSION_PROP_NAME + ":" + PROP_DEFAULT_VALUE + "}", managementClient.getControllerClient());
        Utils.redefineProperty(EXPRESSION_PROP_NAME, EXPRESSION_PROP_VALUE, managementClient.getControllerClient());
        
        try {
            expresionEvaluation();
        } finally {
            // removing tested properties
            Utils.removeProperty(EXPRESSION_PROP_NAME, managementClient.getControllerClient());
            Utils.removeProperty(PROP_NAME, managementClient.getControllerClient());
        }
    }
    
    private void expresionEvaluation() {
        String result = bean.getJBossProperty(EXPRESSION_PROP_NAME);
        log.infof("expressionEvaluation: JBoss property %s was resolved to %s", EXPRESSION_PROP_NAME, result);
        Assert.assertEquals("jboss property " + EXPRESSION_PROP_NAME + " defined directly", EXPRESSION_PROP_VALUE, result);
        
        
        result = bean.getJBossProperty(PROP_NAME);
        log.infof("expressionEvaluation: JBoss property %s was resolved to %s", PROP_NAME, result);
        Assert.assertEquals("jboss property " + PROP_NAME + " substitution evaluation expected", EXPRESSION_PROP_VALUE, result);
        

        result = bean.getSystemProperty(EXPRESSION_PROP_NAME);
        log.infof("expressionEvaluation: System property %s has value %s", EXPRESSION_PROP_NAME, result);
        Assert.assertEquals("system property " + EXPRESSION_PROP_NAME + " from directly defined jboss property", EXPRESSION_PROP_VALUE, result);
        
        result = bean.getSystemProperty(PROP_NAME);
        log.infof("expressionEvaluation:  System property %s has value %s", PROP_NAME, result);
        Assert.assertEquals("system property " + PROP_NAME + " from evaluated jboss property", EXPRESSION_PROP_VALUE, result);
    }
}
