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
 * Tests for SSO and Global Logout using Picketlink SAML2 authentication between Identity Provider(IdP) and
 * two Service Providers(SP) 
 * 
 * @author Filip Bogyai
 */
@RunWith(Arquillian.class)
@ServerSetup({ PicketLinkTestBase.SecurityDomainsSetup.class })
@RunAsClient
@Ignore("PLINK2-119")
public class SAML2GlobalSSOandLogoutTestCase {

	private static Logger LOGGER = Logger.getLogger(SAML2GlobalSSOandLogoutTestCase.class);

	private static final String IDP = "idp";
	private static final String SP1 = "sp1";
	private static final String SP2 = "sp2";
	private static final String LOGOUT_PARAMETER = "?GLO=true";

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
		war.addAsWebInfResource(SAML2GlobalSSOandLogoutTestCase.class.getPackage(),"web.xml", "web.xml");		
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
		war.addAsWebInfResource(SAML2GlobalSSOandLogoutTestCase.class.getPackage(),"web.xml", "web.xml");		
		war.addAsWebInfResource( Utils.getJBossWebXmlAsset("sp", "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"),
				"jboss-web.xml");		
		war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketlink"), "jboss-deployment-structure.xml");
		war.addAsWebInfResource(new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-sp.xml",SP1,"REDIRECT", IDP_CONTEXT_PATH)), "picketlink.xml");
		war.add(new StringAsset("Welcome to SP1"), "index.jsp");
		war.add(new StringAsset("Logout in progress"), "logout.jsp");

		return war;
	}

	@Deployment(name = SP2)
	public static WebArchive deploymentSP2() {
		LOGGER.info("Start deployment " + SP2);
		final WebArchive war = ShrinkWrap.create(WebArchive.class, SP2 + ".war");
		war.addAsWebInfResource(SAML2GlobalSSOandLogoutTestCase.class.getPackage(),"web.xml", "web.xml");		
		war.addAsWebInfResource( Utils.getJBossWebXmlAsset("sp", "org.picketlink.identity.federation.bindings.tomcat.sp.ServiceProviderAuthenticator"),
				"jboss-web.xml");		
		war.addAsManifestResource(Utils.getJBossDeploymentStructure("org.picketlink"),"jboss-deployment-structure.xml");
		war.addAsWebInfResource(new StringAsset(PicketLinkTestBase.propertiesReplacer("picketlink-sp.xml",SP2,"REDIRECT", IDP_CONTEXT_PATH)), "picketlink.xml");
		war.add(new StringAsset("Welcome to SP2"), "index.jsp");
		war.add(new StringAsset("Logout in progress"), "logout.jsp");

		return war;
	}
	
	/**
	 * Tests PicketLink IdP to handle Single Sing On and Global Logout
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSSOandGLO() throws Exception {

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		httpClient.setRedirectStrategy(Utils.REDIRECT_STRATEGY);
		try {

			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(PicketLinkTestBase.ANIL, PicketLinkTestBase.ANIL);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, idpUrl.getPort()), credentials);
			
			String response = PicketLinkTestBase.makeCall(sp1Url, httpClient, 200);
			assertTrue("SP1 index page was not reached",response.contains("Welcome to SP1"));
			
			//now we change credentials so when they are requested again we get 403 - Forbidden
			credentials = new UsernamePasswordCredentials(PicketLinkTestBase.MARCUS, PicketLinkTestBase.MARCUS);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, idpUrl.getPort()), credentials);
			
			response = PicketLinkTestBase.makeCall(sp2Url, httpClient, 200);
			assertTrue("SP2 index page was not reached",response.contains("Welcome to SP2"));
			
			URL logoutUrl = new URL(sp1Url.toExternalForm()+LOGOUT_PARAMETER);
			response = PicketLinkTestBase.makeCall(logoutUrl , httpClient, 200);			
			assertTrue("Logout page was not reached", response.contains("Logout"));
			
			PicketLinkTestBase.makeCall(sp1Url, httpClient, 403);
			PicketLinkTestBase.makeCall(sp2Url, httpClient, 403);
			
									
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
	
}
