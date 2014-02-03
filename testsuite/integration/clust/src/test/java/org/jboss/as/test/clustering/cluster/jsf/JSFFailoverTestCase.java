/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.jsf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.web.ClusteredWebSimpleTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Weld numberguess example converted to a test
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JSFFailoverTestCase extends ClusterAbstractTestCase {

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "numberguess-jsf.war");
        war.addClasses(Game.class, Generator.class, MaxNumber.class, Random.class);
        war.setWebXML(ClusteredWebSimpleTestCase.class.getPackage(), "web.xml");
        war.addAsWebResource(JSFFailoverTestCase.class.getPackage(), "home.xhtml", "home.xhtml");
        war.addAsWebInfResource(JSFFailoverTestCase.class.getPackage(), "faces-config.xml", "faces-config.xml");
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * Parses the response page and headers for a cookie, JSF view state and the numberguess game status.
     *
     * @param response
     * @param sessionId
     * @return
     * @throws IllegalStateException
     * @throws IOException
     */
    private static NumberGuessState parseState(HttpResponse response, String sessionId) throws IllegalStateException, IOException {
        Pattern smallestPattern = Pattern.compile("<span id=\"numberGuess:smallest\">([^<]+)</span>");
        Pattern biggestPattern = Pattern.compile("<span id=\"numberGuess:biggest\">([^<]+)</span>");
        Pattern remainingPattern = Pattern.compile("You have (\\d+) guesses remaining.");
        Pattern viewStatePattern = Pattern.compile("id=\".*javax.faces.ViewState.*\" value=\"([^\"]*)\"");

        Matcher matcher;

        NumberGuessState state = new NumberGuessState();
        String responseString = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

        Header setCookie = response.getFirstHeader("Set-Cookie");
        if (setCookie != null) {
            String setCookieValue = setCookie.getValue();
            String encodedSessionId = setCookieValue.substring(setCookieValue.indexOf('=') + 1, setCookieValue.indexOf(';'));
            int index = encodedSessionId.indexOf('.');
            state.sessionId = (index > 0) ? encodedSessionId.substring(0, index) : encodedSessionId;
        } else if (sessionId != null) {
            // We don't get a cookie back if we have sent it, so just set it to whatever we had before
            state.sessionId = sessionId;
        }

        matcher = smallestPattern.matcher(responseString);
        if (matcher.find()) {
            state.smallest = matcher.group(1);
        }

        matcher = biggestPattern.matcher(responseString);
        if (matcher.find()) {
            state.biggest = matcher.group(1);
        }

        matcher = remainingPattern.matcher(responseString);
        if (matcher.find()) {
            state.remainingGuesses = matcher.group(1);
        }

        matcher = viewStatePattern.matcher(responseString);
        if (matcher.find()) {
            state.jsfViewState = matcher.group(1);
        }

        return state;
    }

    /**
     * Creates an HTTP POST request with a number guess.
     *
     * @param url
     * @param sessionId
     * @param viewState
     * @param guess
     * @return
     * @throws UnsupportedEncodingException
     */
    private static HttpUriRequest buildPostRequest(String url, String sessionId, String viewState, String guess) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost(url);

        List<NameValuePair> list = new LinkedList<NameValuePair>();

        list.add(new BasicNameValuePair("javax.faces.ViewState", viewState));
        list.add(new BasicNameValuePair("numberGuess", "numberGuess"));
        list.add(new BasicNameValuePair("numberGuess:guessButton", "Guess"));
        list.add(new BasicNameValuePair("numberGuess:inputGuess", guess));

        post.setEntity(new StringEntity(URLEncodedUtils.format(list, "UTF-8"), ContentType.APPLICATION_FORM_URLENCODED));
        if (sessionId != null) {
            post.setHeader("Cookie", "JSESSIONID=" + sessionId);
        }

        return post;
    }

    /**
     * Creates an HTTP GET request, with a potential JSESSIONID cookie.
     *
     * @param url
     * @param sessionId
     * @return
     */
    private static HttpUriRequest buildGetRequest(String url, String sessionId) {
        HttpGet request = new HttpGet(url);
        if (sessionId != null) {
            request.addHeader("Cookie", "JSESSIONID=" + sessionId);
        }

        return request;
    }

    /**
     * Test simple graceful shutdown failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Shutdown first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Bring up the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws java.io.IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    public void testGracefulSimpleFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {

        DefaultHttpClient client = org.jboss.as.test.http.util.HttpClientUtils.relaxedCookieHttpClient();

        String url1 = baseURL1.toString() + "home.jsf";
        String url2 = baseURL2.toString() + "home.jsf";

        log.info("URLs are: " + url1 + ", " + url2);

        try {
            HttpResponse response;
            NumberGuessState state;

            // First non-JSF request to the home page
            response = client.execute(buildGetRequest(url1, null));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, null);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // We get a cookie!
            String sessionId = state.sessionId;

            Assert.assertNotNull(sessionId);
            Assert.assertEquals("0", state.smallest);
            Assert.assertEquals("100", state.biggest);
            Assert.assertEquals("10", state.remainingGuesses);

            // We do a JSF POST request, guessing "1"
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "1"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals("2", state.smallest);
            Assert.assertEquals("100", state.biggest);
            Assert.assertEquals("9", state.remainingGuesses);

            // Gracefully shutdown the 1st container.
            stop(CONTAINER_1);

            // Now we do a JSF POST request with a cookie on to the second node, guessing 100, expecting to find a replicated state.
            response = client.execute(buildPostRequest(url2, state.sessionId, state.jsfViewState, "100"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // If the state would not be replicated, we would have 9 remaining guesses.
            Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", "8", state.remainingGuesses);

            // The server should accept our cookie and not try to set a different one
            Assert.assertEquals(sessionId, state.sessionId);
            Assert.assertEquals("2", state.smallest);
            Assert.assertEquals("99", state.biggest);

            // Now we do a JSF POST request on the second node again, guessing "99"
            response = client.execute(buildPostRequest(url2, sessionId, state.jsfViewState, "99"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals("7", state.remainingGuesses);
            Assert.assertEquals("2", state.smallest);
            Assert.assertEquals("98", state.biggest);

            start(CONTAINER_1);

            // And now we go back to the first node, guessing 2
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "2"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals("Session failed to replicate after container 1 was brought up.", "6", state.remainingGuesses);
            Assert.assertEquals(sessionId, state.sessionId);
            Assert.assertEquals("3", state.smallest);
            Assert.assertEquals("98", state.biggest);

            // One final guess on the first node, guess 50
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "50"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals(sessionId, state.sessionId);
            Assert.assertEquals("5", state.remainingGuesses);
            Assert.assertEquals("3", state.smallest);
            Assert.assertEquals("49", state.biggest);
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

        // Assert.fail("Show me the logs please!");
    }

    /**
     * Test simple undeploy failover:
     * <p/>
     * 1/ Start 2 containers and deploy <distributable/> webapp.
     * 2/ Query first container creating a web session.
     * 3/ Undeploy application from the first container.
     * 4/ Query second container verifying sessions got replicated.
     * 5/ Redeploy application to the first container.
     * 6/ Query first container verifying that updated sessions replicated back.
     *
     * @throws java.io.IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     */
    @Test
    public void testGracefulUndeployFailover(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2)
            throws IOException, InterruptedException, URISyntaxException {

        DefaultHttpClient client = new DefaultHttpClient();

        String url1 = baseURL1.toString() + "home.jsf";
        String url2 = baseURL2.toString() + "home.jsf";

        try {
            HttpResponse response;
            NumberGuessState state;

            // First non-JSF request to the home page
            response = client.execute(buildGetRequest(url1, null));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, null);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // We get a cookie!
            String sessionId = state.sessionId;

            Assert.assertNotNull(sessionId);
            Assert.assertEquals("0", state.smallest);
            Assert.assertEquals("100", state.biggest);
            Assert.assertEquals("10", state.remainingGuesses);

            // We do a JSF POST request, guessing "1"
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "1"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals("2", state.smallest);
            Assert.assertEquals("100", state.biggest);
            Assert.assertEquals("9", state.remainingGuesses);

            // Gracefully undeploy from the 1st container.
            undeploy(DEPLOYMENT_1);

            // Now we do a JSF POST request with a cookie on to the second node, guessing 100, expecting to find a replicated state.
            response = client.execute(buildPostRequest(url2, state.sessionId, state.jsfViewState, "100"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            // If the state would not be replicated, we would have 9 remaining guesses.
            Assert.assertEquals("Session failed to replicate after container 1 was shutdown.", "8", state.remainingGuesses);

            // The server should accept our cookie and not try to set a different one
            Assert.assertEquals(sessionId, state.sessionId);
            Assert.assertEquals("2", state.smallest);
            Assert.assertEquals("99", state.biggest);

            // Now we do a JSF POST request on the second node again, guessing "99"
            response = client.execute(buildPostRequest(url2, sessionId, state.jsfViewState, "99"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals("7", state.remainingGuesses);
            Assert.assertEquals("2", state.smallest);
            Assert.assertEquals("98", state.biggest);

            // Redeploy
            deploy(DEPLOYMENT_1);

            // And now we go back to the first node, guessing 2
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "2"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals("Session failed to replicate after container 1 was brought up.", "6", state.remainingGuesses);
            Assert.assertEquals(sessionId, state.sessionId);
            Assert.assertEquals("3", state.smallest);
            Assert.assertEquals("98", state.biggest);

            // One final guess on the first node, guess 50
            response = client.execute(buildPostRequest(url1, state.sessionId, state.jsfViewState, "50"));
            try {
                Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                state = parseState(response, sessionId);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }

            Assert.assertEquals(sessionId, state.sessionId);
            Assert.assertEquals("5", state.remainingGuesses);
            Assert.assertEquals("3", state.smallest);
            Assert.assertEquals("49", state.biggest);
        } finally {
            HttpClientUtils.closeQuietly(client);
        }

        // Assert.fail("Show me the logs please!");
    }

    /**
     * A simple class representing the client state.
     */
    private static class NumberGuessState {
        String smallest;
        String biggest;
        String sessionId;
        String remainingGuesses;
        String jsfViewState;
    }
}
