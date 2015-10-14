package org.jboss.as.test.integration.security.picketlink;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for Picketlink SAML2 authentication between Identity Provider(IdP) and
 * Service Providers(SP) SP-1 has POST binding, SP-2 has REDIRECT binding
 * 
 * @author Filip Bogyai
 */
@RunWith(Arquillian.class)
@ServerSetup({ PicketLinkTestBase.SecurityDomainsSetup.class })
@RunAsClient
@Ignore("PLINK2-119")
public class SAML2BasicAuthenticationTestCase {

	private static Logger LOGGER = Logger.getLogger(SAML2BasicAuthenticationTestCase.class);

	private static final String IDP = "idp";
	private static final String SP1 = "sp1";
	private static final String SP2 = "sp2";
    
	private static final String IDP_CONTEXT_PATH = "idp";

	@ArquillianResource
	@OperateOnDeployment(IDP)
	private URL idpUrl;

	@ArquillianResource
	@OperateOnDeployment(SP1)
	private URL sp1Url;

	@ArquillianResource
	@OperateOnDeployment(SP2)
	private URL sp2Url;

	@Deployment(name = IDP)
	public static WebArchive deploymentIdP() {
		LOGGER.info("Start deployment " + IDP);
		final WebArchive war = ShrinkWrap.create(WebArchive.class, IDP + ".war");
		war.addAsResource(new StringAsset(PicketLinkTestBase.USERS), "users.properties");
		war.addAsResource(new StringAsset(PicketLinkTestBase.ROLES), "roles.properties");
		war.addAsWebInfResource(SAML2BasicAuthenticationTestCase.class.getPackage(),"web.xml", "web.xml");		
		war.addAsWebInfResource(Utils.getJBossWebXmlAsset("idp", "org.picketlink.identity.federation.bindings.tomcat.idp.IDPWebBrowserSSOValve"),
				"jboss-web.xml");		
		war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketlink"),"jboss-deployment-structure.xml");
		war.addAsWebInfResource(new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-idp.xml",IDP,"", IDP_CONTEXT_PATH)), "picketlink.xml");
		war.add(new StringAsset("Welcome to IdP"), "index.jsp");
		war.add(new StringAsset("Welcome to IdP hosted"), "hosted/index.jsp");

		return war;
	}

	@Deployment(name = SP1)
	public static WebArchive deploymentSP1() {
		LOGGER.info("Start deployment " + SP1);
		final WebArchive war = ShrinkWrap.create(WebArchive.class, SP1 + ".war");
		war.addAsWebInfResource(SAML2BasicAuthenticationTestCase.class.getPackage(),"web.xml", "web.xml");		
		war.addAsWebInfResource( Utils.getJBossWebXmlAsset("sp", "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"),
				"jboss-web.xml");		
		war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
		war.addAsWebInfResource(new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-sp.xml",SP1,"POST", IDP_CONTEXT_PATH)), "picketlink.xml");
		war.add(new StringAsset("Welcome to SP1"), "index.jsp");

		return war;
	}

	@Deployment(name = SP2)
	public static WebArchive deploymentSP2() {
		LOGGER.info("Start deployment " + SP2);
		final WebArchive war = ShrinkWrap.create(WebArchive.class, SP2 + ".war");
		war.addAsWebInfResource(SAML2BasicAuthenticationTestCase.class.getPackage(),"web.xml", "web.xml");		
		war.addAsWebInfResource( Utils.getJBossWebXmlAsset("sp", "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"),
				"jboss-web.xml");		
		war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketlink"),"jboss-deployment-structure.xml");
		war.addAsWebInfResource(new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-sp.xml",SP2,"REDIRECT", IDP_CONTEXT_PATH)), "picketlink.xml");
		war.add(new StringAsset("Welcome to SP2"), "index.jsp");

		return war;
	}

	/**
	 * Tests access to protected service provider with manual handling of
	 * redirections
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRedirectSP() throws Exception {

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		
		try {

			final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(PicketLinkTestBase.ANIL, PicketLinkTestBase.ANIL);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, idpUrl.getPort()), credentials);

			// login to IdP
			String response = PicketLinkTestBase.makeCall(idpUrl, httpClient, 200);
			assertTrue("IdP index page was not reached",response.contains("Welcome to IdP"));

			// request SP2
			URL redirectLocation = PicketLinkTestBase.makeCallWithoutRedirect(sp2Url, httpClient);

			// redirected to IdP
			redirectLocation = PicketLinkTestBase.makeCallWithoutRedirect(redirectLocation,httpClient);

			// redirected back to SP2
			redirectLocation = PicketLinkTestBase.makeCallWithoutRedirect(redirectLocation,httpClient);

			// successfully authorized in SP2
			response = PicketLinkTestBase.makeCall(redirectLocation, httpClient, 200);
			assertTrue("SP2 index page was not reached", response.contains("Welcome to SP2"));

		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	/**
	 * Tests access to protected service provider with automatic handling of
	 * redirections
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAutoRedirectSP() throws Exception {

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.setRedirectStrategy(Utils.REDIRECT_STRATEGY);
		try {

			final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(PicketLinkTestBase.ANIL, PicketLinkTestBase.ANIL);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, idpUrl.getPort()), credentials);

			String response = PicketLinkTestBase.makeCall(sp2Url, httpClient, 200);
			assertTrue("SP2 index page was not reached",response.contains("Welcome to SP2"));
			
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/**
	 * Tests access to protected service provider with post binding
	 * 
	 * @throws Exception
	 */

	@Test
	public void testPostSP() throws Exception {

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.setRedirectStrategy(Utils.REDIRECT_STRATEGY);
		
		try {

			final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(PicketLinkTestBase.ANIL, PicketLinkTestBase.ANIL);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, idpUrl.getPort()), credentials);

			String response = PicketLinkTestBase.makeCall(idpUrl, httpClient, 200);
			assertTrue("IdP index page was not reached",response.contains("Welcome to IdP"));
			
			response = PicketLinkTestBase.postSAML2Assertions(sp1Url, idpUrl , httpClient);
			assertTrue("SP1 index page was not reached",response.contains("Welcome to SP1"));
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
	/**
	 * Tests access to protected service provider without credentials or with bad credentials
	 * which must be denied
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUnauthorizedAccess() throws Exception {

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.setRedirectStrategy(Utils.REDIRECT_STRATEGY);
		try {
		    
			PicketLinkTestBase.makeCall(sp2Url, httpClient, 401);
			
			final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(PicketLinkTestBase.MARCUS, PicketLinkTestBase.MARCUS);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, idpUrl.getPort()), credentials);

			PicketLinkTestBase.makeCall(sp2Url, httpClient, 403);
			
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
}