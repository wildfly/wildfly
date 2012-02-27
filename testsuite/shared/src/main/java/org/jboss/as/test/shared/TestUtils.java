/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.shared;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import javax.security.auth.callback.CallbackHandler;
import org.jboss.as.arquillian.container.Authentication;
import org.jboss.as.controller.client.ModelControllerClient;

/**
 *
 * @author ondra
 */
public class TestUtils {
    
    public static ModelControllerClient getModelControllerClient( CallbackHandler callbackHandler ) throws UnknownHostException {
        return ModelControllerClient.Factory.create(
            InetAddress.getByName( TestSuiteEnvironment.getServerAddress() ), 
            TestSuiteEnvironment.getServerPort(),
            callbackHandler
        );
    }
    
    public static ModelControllerClient getModelControllerClient() throws UnknownHostException {
        return getModelControllerClient( Authentication.getCallbackHandler() );
    }

    public static int getEJBPort() {
        return 4447;
    }
    
    public static URI getEjbURI() throws URISyntaxException {
        return new URI("remote://" + TestSuiteEnvironment.getServerAddress() + ":" + TestUtils.getEJBPort());
    }
    
}
