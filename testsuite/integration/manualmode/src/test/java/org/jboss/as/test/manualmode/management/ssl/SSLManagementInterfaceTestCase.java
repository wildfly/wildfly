package org.jboss.as.test.manualmode.management.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.security.common.Coding;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.vfs.VFSUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing https connection to management web console with configured SSL. 
 * HTTP client uses trustore with accepted server's certificate.
 *
 * @author Filip Bogyai
 */

@Ignore("WFLY-2134")
@RunWith(Arquillian.class)
@RunAsClient
public class SSLManagementInterfaceTestCase {

	private static Logger LOGGER = Logger.getLogger(SSLManagementInterfaceTestCase.class);

	private static final String KEYSTORE_FILE_NAME = "server.keystore";
	private static final File KEYSTORE_FILE = new File(KEYSTORE_FILE_NAME);
	private static final String KEYSTORE_PASSWORD = "secret";
	private static final String TRUSTSTORE_FILE_NAME = "client.truststore";
	private static final File TRUSTSTORE_FILE = new File(TRUSTSTORE_FILE_NAME);	

	private static final String MGMT_USERS = "mgmt-users.properties";
	private static final File MGMT_USERS_FILE = new File(MGMT_USERS);	
	private static final String MANAGEMENT_REALM = "ManagementConsoleRealm";
	public static final int MGMT_PORT = 9990;
	public static final int MGMT_SECURED_PORT = 9993;
	private static final String MGMT_CTX = "/management";
	private static final String ADMIN = "admin";
	private static final String ADMIN_PASS = "secret_1";

	public static final String CONTAINER = "default-jbossas";

    @ArquillianResource
    private static ContainerController containerController;
                
    @Test
    @InSequence(-1)
    public void startAndSetupContainer() throws Exception{
    	
    	LOGGER.info("*** starting server");
    	containerController.start(CONTAINER);  
    	ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    	ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "http-remoting");
		LOGGER.info("*** will configure server now");			
		serverSetup(managementClient);		

		LOGGER.info("*** restarting server");
		containerController.stop(CONTAINER);
		containerController.start(CONTAINER);    	
    } 
    
    @Test
    @InSequence(1)
    public void testHTTP() throws ClientProtocolException, IOException, URISyntaxException{
    	
    	final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    	ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "http-remoting");
        DefaultHttpClient httpClient = new DefaultHttpClient();        
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(ADMIN, ADMIN_PASS);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),credentials);
                    	
    	URL mgmtURL = new URL("http", managementClient.getMgmtAddress() , MGMT_PORT, MGMT_CTX);    	
    	String responseBody = makeCall(mgmtURL, httpClient, 200);
    	assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));    	
    }
        
    @Test
    @InSequence(2)
    public void testHTTPS() throws Exception{
    	
    	final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    	ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "http-remoting");
        DefaultHttpClient httpClient = SSLTruststoreUtil.getHttpClientWithTrustStore(TRUSTSTORE_FILE); 
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(ADMIN, ADMIN_PASS);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),credentials);
                    	
    	URL mgmtURL = new URL("https", managementClient.getMgmtAddress() , MGMT_SECURED_PORT , MGMT_CTX);  
    	String responseBody = makeCall(mgmtURL, httpClient, 200);
    	assertTrue("Management index page was not reached", responseBody.contains("management-major-version"));    	
    	
    }
    
    @Test
    @InSequence(3)
    public void stopContainer() throws Exception {
    	
    	final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    	ManagementClient managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "http-remoting");
        LOGGER.info("*** reseting test configuration");
        serverTearDown(managementClient);    	
    	
    	LOGGER.info("*** stopping container");
        containerController.stop(CONTAINER);
    }
   
    /**
	 * Requests given URL and checks if the returned HTTP status code is the
	 * expected one. Returns HTTP response body
	 * 
	 * @param URL url to which the request should be made
	 * @param DefaultHttpClient httpClient to test multiple access            
	 * @param expectedStatusCode expected status code returned from the requested server
	 * @return HTTP response body
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String makeCall(URL url, DefaultHttpClient httpClient, int expectedStatusCode) 
			throws ClientProtocolException,	IOException, URISyntaxException {

		String httpResponseBody = null;
		HttpGet httpGet = new HttpGet(url.toURI());
		HttpResponse response = httpClient.execute(httpGet);		
		int statusCode = response.getStatusLine().getStatusCode();
		LOGGER.info("Request to: " + url + " responds: " + statusCode);

		assertEquals("Unexpected status code", expectedStatusCode, statusCode);

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			httpResponseBody = EntityUtils.toString(response.getEntity());
			EntityUtils.consume(entity);
		}
		return httpResponseBody;
	}

    private void createTempKS(final String keystoreFilename, final File keystoreFile) throws IOException {
        InputStream is = getClass().getResourceAsStream(keystoreFilename);
        FileOutputStream fos = new FileOutputStream(keystoreFile);
        try {
            IOUtils.copy(is, fos);
        } finally {
            VFSUtils.safeClose(is);
            VFSUtils.safeClose(fos);
        }
    }
    
    private void serverSetup(ManagementClient managementClient) throws Exception{
    	
    	createTempKS(KEYSTORE_FILE_NAME, KEYSTORE_FILE);
    	createTempKS(TRUSTSTORE_FILE_NAME, TRUSTSTORE_FILE);
    	    	
    	String managementUser = ADMIN +"=" + Utils.hashMD5(ADMIN + ":" + MANAGEMENT_REALM + ":" + ADMIN_PASS, Coding.HEX);    	
    	FileUtils.writeStringToFile(MGMT_USERS_FILE, managementUser);
    	
    	// add new ManagementConsoleRealm with SSL keystore
    	ModelNode operation = createOpNode("core-service=management/security-realm=ManagementConsoleRealm", ModelDescriptionConstants.ADD);
    	managementClient.getControllerClient().execute(operation);
    	
    	operation = createOpNode("core-service=management/security-realm=ManagementConsoleRealm/authentication=properties", ModelDescriptionConstants.ADD);
    	operation.get(PATH).set(MGMT_USERS_FILE.getAbsolutePath());    	
    	managementClient.getControllerClient().execute(operation);    	
    	
		operation = createOpNode("core-service=management/security-realm=ManagementConsoleRealm/server-identity=ssl",	ModelDescriptionConstants.ADD);
		operation.get(PROTOCOL).set("TLSv1");
		operation.get("keystore-path").set(KEYSTORE_FILE.getAbsolutePath());		
		operation.get("keystore-password").set(KEYSTORE_PASSWORD);
		operation.get("alias").set("management");
		managementClient.getControllerClient().execute(operation);

		// change security-realm for management interface
		operation = createOpNode("core-service=management/management-interface=http-interface",	ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
		operation.get(NAME).set("security-realm");
		operation.get(VALUE).set("ManagementConsoleRealm");
		managementClient.getControllerClient().execute(operation);

		// add https connector to management interface
		operation = createOpNode("core-service=management/management-interface=http-interface",	ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
		operation.get(NAME).set("secure-socket-binding");
		operation.get(VALUE).set("management-https");
		managementClient.getControllerClient().execute(operation);

    }    
    
    private void serverTearDown(ManagementClient managementClient) throws Exception{
    	   

    	ModelNode operation = createOpNode("core-service=management/management-interface=http-interface",	ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
		operation.get(NAME).set("secure-socket-binding");		
		managementClient.getControllerClient().execute(operation);	

    	operation = createOpNode("core-service=management/management-interface=http-interface",	ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
		operation.get(NAME).set("security-realm");
		operation.get(VALUE).set("ManagementRealm");
		managementClient.getControllerClient().execute(operation);
    	
		operation = createOpNode("core-service=management/security-realm=ManagementConsoleRealm",	ModelDescriptionConstants.REMOVE);		
		managementClient.getControllerClient().execute(operation);

		KEYSTORE_FILE.delete();
		TRUSTSTORE_FILE.delete();		
		MGMT_USERS_FILE.delete();

    }    
    
}