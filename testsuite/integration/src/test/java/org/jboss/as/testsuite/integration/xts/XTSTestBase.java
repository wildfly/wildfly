/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.testsuite.integration.xts;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Base class for XTS tests.
 *
 * @author istudens@redhat.com
 */
public abstract class XTSTestBase {
    private static final Logger log = Logger.getLogger(XTSTestBase.class);

    protected boolean repeatable = true;
    protected int waitForRetryInSecs = 10;
    protected int loopRetryMax = 30;

    protected String headerRunName = "run";
    protected String headerRunValue = "run";

    public boolean callTestServlet(String url, List<NameValuePair> params, String outfile) throws Throwable {
        boolean result = true;
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            // run tests by calling a servlet
            HttpPost httppost = new HttpPost(url);
            if (params == null) {
                params = new ArrayList<NameValuePair>(1);
            }
            params.add(new BasicNameValuePair(headerRunName, headerRunValue));
            httppost.setEntity(new UrlEncodedFormEntity(params));

            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = httpclient.execute(httppost, responseHandler);

            if (repeatable) {
                int index = 0;
                do {
                    System.err.println("_____________ " + (index++) + "th round");
                    // we have to give some time to the tests to finish
                    Thread.sleep(waitForRetryInSecs * 1000);

                    // let's try to get results
                    HttpGet httpget = new HttpGet(url);
//                    responseHandler = new BasicResponseHandler();
                    response = httpclient.execute(httpget, responseHandler);
                }
                while (response != null && response.indexOf("finished") == -1 && index++ < loopRetryMax);
            }

            log.info("======================================================");
            log.info("====================   RESULT    =====================");
            log.info("======================================================");
            log.info(response);

            // write response (which is a junit xml file) to the given outfile
            BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
            writer.write(response);
            writer.close();

            // check the test output for errors and failures
            int errors = getInt(Pattern.compile("^.*errors=\"(\\d+)\".*$", Pattern.DOTALL).matcher(response).replaceFirst("$1"), -1);
            int failures = getInt(Pattern.compile("^.*failures=\"(\\d+)\".*$", Pattern.DOTALL).matcher(response).replaceFirst("$1"), -1);

            result = (response.indexOf("finished") != -1) && (failures == 0);

            if (errors > 0)
                throw new Throwable("The tests hit " + errors + " errors!");

        } catch (Exception e) {
            log.error("======================================================");
            log.error("====================  EXCEPTION  =====================");
            log.error("======================================================");
            log.error(e);
            result = false;
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return result;
    }

    private int getInt(String value, int defaultValue) {
        int ret;
        try {
            ret = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            ret = defaultValue;
        }
        return ret;
    }

}
