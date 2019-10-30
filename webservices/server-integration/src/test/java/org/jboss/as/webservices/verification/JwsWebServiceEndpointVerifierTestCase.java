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
package org.jboss.as.webservices.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.verification.JwsWebServiceEndpointVerifier.ImplementationHasFinalize;
import org.jboss.as.webservices.verification.JwsWebServiceEndpointVerifier.WebMethodIsNotPublic;
import org.jboss.as.webservices.verification.JwsWebServiceEndpointVerifier.WebMethodIsStaticOrFinal;
import org.junit.Before;
import org.junit.Test;

/**
 * @author sfcoy
 *
 */
public class JwsWebServiceEndpointVerifierTestCase {

    private DeploymentReflectionIndex deploymentReflectionIndex;

    @Before
    public void setup() {
        deploymentReflectionIndex = DeploymentReflectionIndex.create();
    }

    @Test
    public void testVerifySucceeds() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(GoodSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verify();
        assertFalse(sut.failed());
    }

    @Test
    public void testVerifySimpleWSSucceeds() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(SimpleWSImpl.class, null,
                deploymentReflectionIndex);
        sut.verify();
        assertFalse(sut.failed());
    }

    @Test
    public void testVerifyAnnotatedSampleWSSucceeds() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(AnnotatedSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verify();
        assertFalse(sut.failed());
    }

    @Test
    public void testVerifyAnnotatedSampleWSWithExclusionSucceeds() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(AnnotatedSampleWSImplWithExclusion.class, null,
                deploymentReflectionIndex);
        sut.verify();
        assertFalse(sut.failed());
    }

    @Test
    public void testVerifyExtendedSampleWSSucceeds() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(ConcreteSampleWSImpl.class, null,
                deploymentReflectionIndex);
        sut.verify();
        assertFalse(sut.failed());
    }

    @Test
    public void testVerifyFails() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(BrokenSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verify();
        assertTrue(sut.failed());
        assertEquals(5, sut.verificationFailures.size());
    }

    @Test
    public void testLogFailures() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(BrokenSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verify();
        sut.logFailures();
    }

    @Test
    public void testVerifyWebMethodSucceeds() throws NoSuchMethodException, SecurityException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(GoodSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verifyWebMethod(SampleWS.class.getMethod("performWork"));
        assertFalse(sut.failed());
        assertEquals(0, sut.verificationFailures.size());
    }

    @Test
    public void testVerifyNonPublicWebMethodFails() throws NoSuchMethodException, SecurityException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(BrokenSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verifyWebMethod(SampleWS.class.getMethod("performWork"));
        assertTrue(sut.failed());
        assertEquals(1, sut.verificationFailures.size());
        assertTrue(sut.verificationFailures.get(0) instanceof WebMethodIsNotPublic);
    }

    @Test
    public void testVerifyStaticWebMethodFails() throws NoSuchMethodException, SecurityException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(BrokenSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verifyWebMethod(SampleWS.class.getMethod("discoverNewLands"));
        assertTrue(sut.failed());
        assertEquals(1, sut.verificationFailures.size());
        assertTrue(sut.verificationFailures.get(0) instanceof WebMethodIsStaticOrFinal);
    }

    @Test
    public void testVerifyFinalWebMethodFails() throws NoSuchMethodException, SecurityException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(BrokenSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verifyWebMethod(SampleWS.class.getMethod("triggerReport"));
        assertTrue(sut.failed());
        assertEquals(1, sut.verificationFailures.size());
        assertTrue(sut.verificationFailures.get(0) instanceof WebMethodIsStaticOrFinal);
    }

    @Test
    public void testVerifyFinalizeMethod() {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(BrokenSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        sut.verifyFinalizeMethod();
        assertTrue(sut.failed());
        assertEquals(1, sut.verificationFailures.size());
        assertTrue(sut.verificationFailures.get(0) instanceof ImplementationHasFinalize);
    }

    @Test
    public void testLoadEndpointInterfaceDefinedWebMethods() throws DeploymentUnitProcessingException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(GoodSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        Collection<Method> endpointInterfaceMethods = sut.endpointInterfaceDefinedWebMethods();
        Set<String> methodNames = new HashSet<>();
        for (Method endpointInterfaceMethod : endpointInterfaceMethods)
            methodNames.add(endpointInterfaceMethod.getName());
        assertTrue(methodNames.contains("performWork"));
        assertTrue(methodNames.contains("discoverNewLands"));
        assertTrue(methodNames.contains("isWorking"));
        assertTrue(methodNames.contains("triggerReport"));
    }

    @Test
    public void testFindEndpointImplMethodMatching() throws NoSuchMethodException, SecurityException {
        JwsWebServiceEndpointVerifier sut = new JwsWebServiceEndpointVerifier(GoodSampleWSImpl.class, SampleWS.class,
                deploymentReflectionIndex);
        Method endpointInterfaceMethod = SampleWS.class.getMethod("performWork");
        Method seiMethod = sut.findEndpointImplMethodMatching(endpointInterfaceMethod);
        assertNotNull(seiMethod);
        assertEquals("performWork", seiMethod.getName());
    }

}
