package org.jboss.as.test.integration.security.picketlink;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.logging.Logger;

/**
 * Base class with common utilities for PicketLink integration tests
 * 
 * @author Filip Bogyai
 */
public class PicketLinkTestBase {

	public static final String ANIL = "anil";
	public static final String MARCUS = "marcus";

	public static final String USERS = ANIL + "=" + ANIL + "\n" + MARCUS + "=" + MARCUS;
	public static final String ROLES = ANIL + "=" + "gooduser" + "\n" + MARCUS	+ "=baduser";
	
	private static final Logger LOGGER = Logger.getLogger(PicketLinkTestBase.class);
	
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

	
	/**
	 * Requests given URL and returns redirect location URL from response header.
	 * If response is not redirected then returns the same URL which was
	 * requested
	 * 
	 * @param URL url to which the request should be made
	 * @param DefaultHttpClient httpClient to test multiple access
	 * @return URL redirect location
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static URL makeCallWithoutRedirect(URL url, DefaultHttpClient httpClient)
			throws ClientProtocolException, IOException, URISyntaxException {

		HttpParams params = new BasicHttpParams();
		params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
		String redirectLocation = url.toExternalForm();

		HttpGet httpGet = new HttpGet(url.toURI());
		httpGet.setParams(params);
		HttpResponse response = httpClient.execute(httpGet);
		int statusCode = response.getStatusLine().getStatusCode();
		LOGGER.info("Request to: " + url + " responds: " + statusCode);

		Header locationHeader = response.getFirstHeader("location");
		if (locationHeader != null) {
			redirectLocation = locationHeader.getValue();
		}

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			EntityUtils.consume(entity);
		}
		return new URL(redirectLocation);
	}
	
	/**
	 * Requests given SP and post SAMLRequest to IdP, then post back SAMLResponse. 
	 * Returns HTTP response body
	 * 
	 * @param URL spURL of requested Service Provider
	 * @param URL idpURL of Identity Provider
	 * @param DefaultHttpClient httpClient to test multiple access  
	 * @return HTTP response body
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String postSAML2Assertions(URL spURL, URL idpURL, DefaultHttpClient httpClient)
			throws ClientProtocolException, IOException, URISyntaxException{
		
		String httpResponseBody = makeCall(spURL, httpClient, 200);
		
		//parse SAMLRequest and post it to IdP
		String[] splitted = httpResponseBody.split("NAME=\"SAMLRequest\" VALUE=\"");
		String samlRequest = splitted[1].substring(0,	splitted[1].indexOf("\""));
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("SAMLRequest", samlRequest));

		HttpPost httpPost = new HttpPost(idpURL.toURI());
		httpPost.setEntity(new UrlEncodedFormEntity(pairs));
		HttpResponse httpResponse = httpClient.execute(httpPost);

		HttpEntity entity = httpResponse.getEntity();
		if (entity != null) {
			httpResponseBody = EntityUtils.toString(httpResponse.getEntity());
			EntityUtils.consume(entity);
		}
		
		//parse SAMLResponse and post it back to SP
		splitted = httpResponseBody.split("NAME=\"SAMLResponse\" VALUE=\"");
		String samlResponse = splitted[1].substring(0, splitted[1].indexOf("\""));
		pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("SAMLResponse", samlResponse));

		httpPost = new HttpPost(spURL.toURI());
		httpPost.setEntity(new UrlEncodedFormEntity(pairs));			
		httpResponse = httpClient.execute(httpPost);

		entity = httpResponse.getEntity();
		if (entity != null) {
			httpResponseBody = EntityUtils.toString(httpResponse.getEntity());
			EntityUtils.consume(entity);
		}
		
		return httpResponseBody;
	}

	
	/**
	 * Replace variables in PicketLink configurations files with given values
	 * and set ${hostname} variable from system property: node0
	 * 
	 * @param String resourceFile 
	 * @param String deploymentName 
	 * @param String bindingType 
	 * @return String content
	 */
	public static String propertiesReplacer(String resourceFile, String deploymentName, String bindingType, String idpContextPath) {

		String hostname = System.getProperty("node0");		
		
		//expand possible IPv6 address
		try{
			hostname = NetworkUtils.formatPossibleIpv6Address(InetAddress.getByName(hostname).getHostAddress());
		}catch(UnknownHostException ex){
			String message = "Cannot resolve host address: "+ hostname + " , error : " + ex.getMessage();
			LOGGER.error(message);
			throw new RuntimeException(ex);			
		}		
		
		final Map<String, String> map = new HashMap<String, String>();
		String content = "";
		map.put("hostname", hostname);
		map.put("deployment", deploymentName);
		map.put("bindingType", bindingType);
		map.put("idpContextPath", idpContextPath);
		
		try {
			content = StrSubstitutor.replace(IOUtils.toString(SAML2BasicAuthenticationTestCase.class.getResourceAsStream(resourceFile), "UTF-8"), map);
		} catch (IOException ex) {
			String message = "Cannot find or modify configuration file "+ resourceFile + " , error : " + ex.getMessage();
			LOGGER.error(message);
			throw new RuntimeException(ex);
		}
		return content;
	}

	/**
	 * A {@link ServerSetupTask} instance which creates security domains for Identity Provider(IdP)
	 * and Service Provider(SP)
	 * 
	 * @author Filip Bogyai
	 */
	static class SecurityDomainsSetup extends
			AbstractSecurityDomainsServerSetupTask {

		/**
		 * Returns SecurityDomains configuration for this testcase.
		 * 
		 * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
		 */
		@Override
		protected SecurityDomain[] getSecurityDomains() {

			final SecurityDomain idp = new SecurityDomain.Builder()
					.name("idp")
					.cacheType("default")
					.loginModules(
							new SecurityModule.Builder()
									.name("UsersRoles")
									.flag("required")
									.putOption("usersProperties","users.properties")
									.putOption("rolesProperties","roles.properties").build()) //
					.build();
			final SecurityDomain sp = new SecurityDomain.Builder()
					.name("sp")
					.cacheType("default")
					.loginModules(
							new SecurityModule.Builder()
									.name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule")
									.flag("required").build()) //
					.build();
			return new SecurityDomain[] { idp, sp };
		}
	}

}
