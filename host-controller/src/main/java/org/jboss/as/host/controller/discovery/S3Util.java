/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.host.controller.discovery;

import static java.lang.String.valueOf;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jboss.util.Base64;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Collection of utility methods required for S3 discovery.
 * Some methods here are based on similar ones from JGroups' S3_PING.java.
 * The S3 access code reuses the example shipped by Amazon, like S3_PING.java does.
 *
 * @author Farah Juma
 */
public class S3Util {

    /**
     * Get the domain controller data from the given byte buffer.
     *
     * @param buffer the byte buffer
     * @return the domain controller data
     * @throws Exception
     */
    public static DomainControllerData domainControllerDataFromByteBuffer(byte[] buffer) throws Exception {
        if(buffer == null) {
            return null;
        }
        DomainControllerData retval = null;
        ByteArrayInputStream in_stream = new ByteArrayInputStream(buffer);
        DataInputStream in = new DataInputStream(in_stream);
        retval = new DomainControllerData();
        retval.readFrom(in);
        in.close();
        return retval;
    }

    /**
     * Write the domain controller data to a byte buffer.
     *
     * @param dcData the domain controller data
     * @return the byte buffer
     * @throws Exception
     */
    public static byte[] domainControllerDataToByteBuffer(DomainControllerData dcData) throws Exception {
        byte[] result=null;
        final ByteArrayOutputStream out_stream=new ByteArrayOutputStream(512);
        DataOutputStream out=new DataOutputStream(out_stream);
        dcData.writeTo(out);
        result=out_stream.toByteArray();
        out.close();
        return result;
    }

    /**
     * Sanitize bucket and folder names according to AWS guidelines.
     */
    protected static String sanitize(final String name) {
        String retval=name;
        retval=retval.replace('/', '-');
        retval=retval.replace('\\', '-');
        return retval;
    }

    /**
     * Use this helper method to generate pre-signed S3 urls.
     * You'll need to generate urls for both the put and delete http methods.
     * Example:
     * Your AWS Access Key is "abcd".
     * Your AWS Secret Access Key is "efgh".
     * You want this node to write its information to "/S3/master/jboss-domain-master-data".
     * So, your bucket is "S3" and your key is "master/jboss-domain-master-data".
     * You want this to expire one year from now, or
     *   (System.currentTimeMillis / 1000) + (60 * 60 * 24 * 365)
     *   Let's assume that this equals 1316286684
     *
     * Here's how to generate the value for the pre_signed_put_url property:
     * String putUrl = S3Util.generatePreSignedUrl("abcd", "efgh", "put",
     *                                              "S3", "master/jboss-domain-master-data",
     *                                              1316286684);
     *
     * Here's how to generate the value for the pre_signed_delete_url property:
     * String deleteUrl = S3Util.generatePreSignedUrl("abcd", "efgh", "delete",
     *                                                 "S3", "master/jboss-domain-master-data",
     *                                                 1316286684);
     *
     * @param awsAccessKey Your AWS Access Key
     * @param awsSecretAccessKey Your AWS Secret Access Key
     * @param method The HTTP method - use "put" or "delete" for use with S3_PING
     * @param bucket The S3 bucket you want to write to
     * @param key The key within the bucket to write to
     * @param expirationDate The date this pre-signed url should expire, in seconds since epoch
     * @return The pre-signed url to be used in pre_signed_put_url or pre_signed_delete_url properties
     */
    public static String generatePreSignedUrl(String awsAccessKey, String awsSecretAccessKey, String method,
                                       String bucket, String key, long expirationDate) {
        Map headers = new HashMap();
        if (method.equalsIgnoreCase("PUT")) {
            headers.put("x-amz-acl", Arrays.asList("public-read"));
        }
        return Utils.generateQueryStringAuthentication(awsAccessKey, awsSecretAccessKey, method,
                                                       bucket, key, new HashMap(), headers,
                                                       expirationDate);
    }

    public static String readString(DataInput in) throws Exception {
        int b=in.readByte();
        if(b == 1)
            return in.readUTF();
        return null;
    }

    public static void writeString(String s, DataOutput out) throws Exception {
        if(s != null) {
            out.write(1);
            out.writeUTF(s);
        }
        else {
            out.write(0);
        }
    }

    /**
     * Class that manipulates pre-signed urls. This has been copied from S3_PING.java since
     * it is not declared with public access in S3_PING.java.
     */
    static class PreSignedUrlParser {
        String bucket = "";
        String prefix = "";

        public PreSignedUrlParser(String preSignedUrl) {
            try {
                URL url = new URL(preSignedUrl);
                String path = url.getPath();
                String[] pathParts = path.split("/");

                if (pathParts.length < 3) {
                    throw MESSAGES.preSignedUrlMustPointToFile(preSignedUrl);
                }
                if (pathParts.length > 4) {
                    throw MESSAGES.invalidPreSignedUrlLength(preSignedUrl);
                }
                this.bucket = pathParts[1];
                if (pathParts.length > 3) {
                    this.prefix = pathParts[2];
                }
            } catch (MalformedURLException ex) {
                throw MESSAGES.invalidPreSignedUrl(preSignedUrl);
            }
        }

        public String getBucket() {
            return bucket;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    /*****************************************************************************************
     *
     * The remaining classes have been copied from Amazon's sample code.
     * Note: These nested classes are also defined in S3_PING.java. However, they
     * are not declared with public access in S3_PING.java and so we have copied
     * them here and added i18n for the error messages.
     *
     *****************************************************************************************/
    static class AWSAuthConnection {
        public static final String LOCATION_DEFAULT=null;
        public static final String LOCATION_EU="EU";

        private String awsAccessKeyId;
        private String awsSecretAccessKey;
        private boolean isSecure;
        private String server;
        private int port;
        private CallingFormat callingFormat;

        public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey) {
            this(awsAccessKeyId, awsSecretAccessKey, true);
        }

        public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure) {
            this(awsAccessKeyId, awsSecretAccessKey, isSecure, Utils.DEFAULT_HOST);
        }

        public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure,
                                 String server) {
            this(awsAccessKeyId, awsSecretAccessKey, isSecure, server,
                 isSecure? Utils.SECURE_PORT : Utils.INSECURE_PORT);
        }

        public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure,
                                 String server, int port) {
            this(awsAccessKeyId, awsSecretAccessKey, isSecure, server, port, CallingFormat.getSubdomainCallingFormat());

        }

        public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure,
                                 String server, CallingFormat format) {
            this(awsAccessKeyId, awsSecretAccessKey, isSecure, server,
                 isSecure? Utils.SECURE_PORT : Utils.INSECURE_PORT,
                 format);
        }

        /**
         * Create a new interface to interact with S3 with the given credential and connection
         * parameters
         * @param awsAccessKeyId     Your user key into AWS
         * @param awsSecretAccessKey The secret string used to generate signatures for authentication.
         * @param isSecure           use SSL encryption
         * @param server             Which host to connect to.  Usually, this will be s3.amazonaws.com
         * @param port               Which port to use.
         * @param format             Type of request Regular/Vanity or Pure Vanity domain
         */
        public AWSAuthConnection(String awsAccessKeyId, String awsSecretAccessKey, boolean isSecure,
                                 String server, int port, CallingFormat format) {
            this.awsAccessKeyId=awsAccessKeyId;
            this.awsSecretAccessKey=awsSecretAccessKey;
            this.isSecure=isSecure;
            this.server=server;
            this.port=port;
            this.callingFormat=format;
        }

        /**
         * Creates a new bucket.
         * @param bucket   The name of the bucket to create.
         * @param headers  A Map of String to List of Strings representing the http headers to pass (can be null).
         */
        public Response createBucket(String bucket, Map headers) throws IOException {
            return createBucket(bucket, null, headers);
        }

        /**
         * Creates a new bucket.
         * @param bucket   The name of the bucket to create.
         * @param location Desired location ("EU") (or null for default).
         * @param headers  A Map of String to List of Strings representing the http
         *                 headers to pass (can be null).
         * @throws IllegalArgumentException on invalid location
         */
        public Response createBucket(String bucket, String location, Map headers) throws IOException {
            String body;
            if(location == null) {
                body=null;
            }
            else if(LOCATION_EU.equals(location)) {
                if(!callingFormat.supportsLocatedBuckets())
                    throw MESSAGES.creatingBucketWithUnsupportedCallingFormat();
                body="<CreateBucketConstraint><LocationConstraint>" + location + "</LocationConstraint></CreateBucketConstraint>";
            }
            else
                throw MESSAGES.invalidS3Location(location);

            // validate bucket name
            if(!Utils.validateBucketName(bucket, callingFormat))
                throw MESSAGES.invalidS3Bucket(bucket);

            HttpURLConnection request=makeRequest("PUT", bucket, "", null, headers);
            if(body != null) {
                request.setDoOutput(true);
                request.getOutputStream().write(body.getBytes("UTF-8"));
            }
            return new Response(request);
        }

        /**
         * Check if the specified bucket exists (via a HEAD request)
         * @param bucket The name of the bucket to check
         * @return true if HEAD access returned success
         */
        public boolean checkBucketExists(String bucket) throws IOException {
            HttpURLConnection response=makeRequest("HEAD", bucket, "", null, null);
            int httpCode=response.getResponseCode();

            if(httpCode >= 200 && httpCode < 300)
                return true;
            if(httpCode == HttpURLConnection.HTTP_NOT_FOUND) // bucket doesn't exist
                return false;
            throw MESSAGES.bucketAuthenticationFailure(bucket, httpCode, response.getResponseMessage());
        }

        /**
         * Lists the contents of a bucket.
         * @param bucket  The name of the bucket to create.
         * @param prefix  All returned keys will start with this string (can be null).
         * @param marker  All returned keys will be lexographically greater than
         *                this string (can be null).
         * @param maxKeys The maximum number of keys to return (can be null).
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public ListBucketResponse listBucket(String bucket, String prefix, String marker,
                                             Integer maxKeys, Map headers) throws IOException {
            return listBucket(bucket, prefix, marker, maxKeys, null, headers);
        }

        /**
         * Lists the contents of a bucket.
         * @param bucket    The name of the bucket to list.
         * @param prefix    All returned keys will start with this string (can be null).
         * @param marker    All returned keys will be lexographically greater than
         *                  this string (can be null).
         * @param maxKeys   The maximum number of keys to return (can be null).
         * @param delimiter Keys that contain a string between the prefix and the first
         *                  occurrence of the delimiter will be rolled up into a single element.
         * @param headers   A Map of String to List of Strings representing the http
         *                  headers to pass (can be null).
         */
        public ListBucketResponse listBucket(String bucket, String prefix, String marker,
                                             Integer maxKeys, String delimiter, Map headers) throws IOException {

            Map pathArgs=Utils.paramsForListOptions(prefix, marker, maxKeys, delimiter);
            return new ListBucketResponse(makeRequest("GET", bucket, "", pathArgs, headers));
        }

        /**
         * Deletes a bucket.
         * @param bucket  The name of the bucket to delete.
         * @param headers A Map of String to List of Strings representing the http headers to pass (can be null).
         */
        public Response deleteBucket(String bucket, Map headers) throws IOException {
            return new Response(makeRequest("DELETE", bucket, "", null, headers));
        }

        /**
         * Writes an object to S3.
         * @param bucket  The name of the bucket to which the object will be added.
         * @param key     The name of the key to use.
         * @param object  An S3Object containing the data to write.
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public Response put(String bucket, String key, S3Object object, Map headers) throws IOException {
            HttpURLConnection request=
                    makeRequest("PUT", bucket, Utils.urlencode(key), null, headers, object);

            request.setDoOutput(true);
            request.getOutputStream().write(object.data == null? new byte[]{} : object.data);

            return new Response(request);
        }

        public Response put(String preSignedUrl, S3Object object, Map headers) throws IOException {
            HttpURLConnection request = makePreSignedRequest("PUT", preSignedUrl, headers);
            request.setDoOutput(true);
            request.getOutputStream().write(object.data == null? new byte[]{} : object.data);

            return new Response(request);
        }

        /**
         * Creates a copy of an existing S3 Object.  In this signature, we will copy the
         * existing metadata.  The default access control policy is private; if you want
         * to override it, please use x-amz-acl in the headers.
         * @param sourceBucket      The name of the bucket where the source object lives.
         * @param sourceKey         The name of the key to copy.
         * @param destinationBucket The name of the bucket to which the object will be added.
         * @param destinationKey    The name of the key to use.
         * @param headers           A Map of String to List of Strings representing the http
         *                          headers to pass (can be null).  You may wish to set the x-amz-acl header appropriately.
         */
        public Response copy(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey, Map headers)
                throws IOException {
            S3Object object=new S3Object(new byte[]{}, new HashMap());
            headers=headers == null? new HashMap() : new HashMap(headers);
            headers.put("x-amz-copy-source", Arrays.asList(sourceBucket + "/" + sourceKey));
            headers.put("x-amz-metadata-directive", Arrays.asList("COPY"));
            return verifyCopy(put(destinationBucket, destinationKey, object, headers));
        }

        /**
         * Creates a copy of an existing S3 Object.  In this signature, we will replace the
         * existing metadata.  The default access control policy is private; if you want
         * to override it, please use x-amz-acl in the headers.
         * @param sourceBucket      The name of the bucket where the source object lives.
         * @param sourceKey         The name of the key to copy.
         * @param destinationBucket The name of the bucket to which the object will be added.
         * @param destinationKey    The name of the key to use.
         * @param metadata          A Map of String to List of Strings representing the S3 metadata
         *                          for the new object.
         * @param headers           A Map of String to List of Strings representing the http
         *                          headers to pass (can be null).  You may wish to set the x-amz-acl header appropriately.
         */
        public Response copy(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey, Map metadata, Map headers)
                throws IOException {
            S3Object object=new S3Object(new byte[]{}, metadata);
            headers=headers == null? new HashMap() : new HashMap(headers);
            headers.put("x-amz-copy-source", Arrays.asList(sourceBucket + "/" + sourceKey));
            headers.put("x-amz-metadata-directive", Arrays.asList("REPLACE"));
            return verifyCopy(put(destinationBucket, destinationKey, object, headers));
        }

        /**
         * Copy sometimes returns a successful response and starts to send whitespace
         * characters to us.  This method processes those whitespace characters and
         * will throw an exception if the response is either unknown or an error.
         * @param response Response object from the PUT request.
         * @return The response with the input stream drained.
         * @throws IOException If anything goes wrong.
         */
        private static Response verifyCopy(Response response) throws IOException {
            if(response.connection.getResponseCode() < 400) {
                byte[] body=GetResponse.slurpInputStream(response.connection.getInputStream());
                String message=new String(body);
                if(message.contains("<Error")) {
                    throw new IOException(message.substring(message.indexOf("<Error")));
                }
                else if(message.contains("</CopyObjectResult>")) {
                    // It worked!
                }
                else {
                    throw MESSAGES.unexpectedResponse(message);
                }
            }
            return response;
        }

        /**
         * Reads an object from S3.
         * @param bucket  The name of the bucket where the object lives.
         * @param key     The name of the key to use.
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public GetResponse get(String bucket, String key, Map headers) throws IOException {
            return new GetResponse(makeRequest("GET", bucket, Utils.urlencode(key), null, headers));
        }

        /**
         * Deletes an object from S3.
         * @param bucket  The name of the bucket where the object lives.
         * @param key     The name of the key to use.
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public Response delete(String bucket, String key, Map headers) throws IOException {
            return new Response(makeRequest("DELETE", bucket, Utils.urlencode(key), null, headers));
        }

        public Response delete(String preSignedUrl) throws IOException {
            return new Response(makePreSignedRequest("DELETE", preSignedUrl, null));
        }

        /**
         * Get the requestPayment xml document for a given bucket
         * @param bucket  The name of the bucket
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public GetResponse getBucketRequestPayment(String bucket, Map headers) throws IOException {
            Map pathArgs=new HashMap();
            pathArgs.put("requestPayment", null);
            return new GetResponse(makeRequest("GET", bucket, "", pathArgs, headers));
        }

        /**
         * Write a new requestPayment xml document for a given bucket
         * @param bucket        The name of the bucket
         * @param requestPaymentXMLDoc
         * @param headers       A Map of String to List of Strings representing the http
         *                      headers to pass (can be null).
         */
        public Response putBucketRequestPayment(String bucket, String requestPaymentXMLDoc, Map headers)
                throws IOException {
            Map pathArgs=new HashMap();
            pathArgs.put("requestPayment", null);
            S3Object object=new S3Object(requestPaymentXMLDoc.getBytes(), null);
            HttpURLConnection request=makeRequest("PUT", bucket, "", pathArgs, headers, object);

            request.setDoOutput(true);
            request.getOutputStream().write(object.data == null? new byte[]{} : object.data);

            return new Response(request);
        }

        /**
         * Get the logging xml document for a given bucket
         * @param bucket  The name of the bucket
         * @param headers A Map of String to List of Strings representing the http headers to pass (can be null).
         */
        public GetResponse getBucketLogging(String bucket, Map headers) throws IOException {
            Map pathArgs=new HashMap();
            pathArgs.put("logging", null);
            return new GetResponse(makeRequest("GET", bucket, "", pathArgs, headers));
        }

        /**
         * Write a new logging xml document for a given bucket
         * @param loggingXMLDoc The xml representation of the logging configuration as a String
         * @param bucket        The name of the bucket
         * @param headers       A Map of String to List of Strings representing the http
         *                      headers to pass (can be null).
         */
        public Response putBucketLogging(String bucket, String loggingXMLDoc, Map headers) throws IOException {
            Map pathArgs=new HashMap();
            pathArgs.put("logging", null);
            S3Object object=new S3Object(loggingXMLDoc.getBytes(), null);
            HttpURLConnection request=makeRequest("PUT", bucket, "", pathArgs, headers, object);

            request.setDoOutput(true);
            request.getOutputStream().write(object.data == null? new byte[]{} : object.data);

            return new Response(request);
        }

        /**
         * Get the ACL for a given bucket
         * @param bucket  The name of the bucket where the object lives.
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public GetResponse getBucketACL(String bucket, Map headers) throws IOException {
            return getACL(bucket, "", headers);
        }

        /**
         * Get the ACL for a given object (or bucket, if key is null).
         * @param bucket  The name of the bucket where the object lives.
         * @param key     The name of the key to use.
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public GetResponse getACL(String bucket, String key, Map headers) throws IOException {
            if(key == null) key="";

            Map pathArgs=new HashMap();
            pathArgs.put("acl", null);

            return new GetResponse(
                    makeRequest("GET", bucket, Utils.urlencode(key), pathArgs, headers)
            );
        }

        /**
         * Write a new ACL for a given bucket
         * @param aclXMLDoc The xml representation of the ACL as a String
         * @param bucket    The name of the bucket where the object lives.
         * @param headers   A Map of String to List of Strings representing the http headers to pass (can be null).
         */
        public Response putBucketACL(String bucket, String aclXMLDoc, Map headers) throws IOException {
            return putACL(bucket, "", aclXMLDoc, headers);
        }

        /**
         * Write a new ACL for a given object
         * @param aclXMLDoc The xml representation of the ACL as a String
         * @param bucket    The name of the bucket where the object lives.
         * @param key       The name of the key to use.
         * @param headers   A Map of String to List of Strings representing the http
         *                  headers to pass (can be null).
         */
        public Response putACL(String bucket, String key, String aclXMLDoc, Map headers)
                throws IOException {
            S3Object object=new S3Object(aclXMLDoc.getBytes(), null);

            Map pathArgs=new HashMap();
            pathArgs.put("acl", null);

            HttpURLConnection request=
                    makeRequest("PUT", bucket, Utils.urlencode(key), pathArgs, headers, object);

            request.setDoOutput(true);
            request.getOutputStream().write(object.data == null? new byte[]{} : object.data);

            return new Response(request);
        }

        public LocationResponse getBucketLocation(String bucket)
                throws IOException {
            Map pathArgs=new HashMap();
            pathArgs.put("location", null);
            return new LocationResponse(makeRequest("GET", bucket, "", pathArgs, null));
        }


        /**
         * List all the buckets created by this account.
         * @param headers A Map of String to List of Strings representing the http
         *                headers to pass (can be null).
         */
        public ListAllMyBucketsResponse listAllMyBuckets(Map headers)
                throws IOException {
            return new ListAllMyBucketsResponse(makeRequest("GET", "", "", null, headers));
        }


        /**
         * Make a new HttpURLConnection without passing an S3Object parameter.
         * Use this method for key operations that do require arguments
         * @param method     The method to invoke
         * @param bucketName the bucket this request is for
         * @param key        the key this request is for
         * @param pathArgs   the
         * @param headers
         * @return
         * @throws MalformedURLException
         * @throws IOException
         */
        private HttpURLConnection makeRequest(String method, String bucketName, String key, Map pathArgs, Map headers)
                throws IOException {
            return makeRequest(method, bucketName, key, pathArgs, headers, null);
        }


        /**
         * Make a new HttpURLConnection.
         * @param method     The HTTP method to use (GET, PUT, DELETE)
         * @param bucket     The bucket name this request affects
         * @param key        The key this request is for
         * @param pathArgs   parameters if any to be sent along this request
         * @param headers    A Map of String to List of Strings representing the http
         *                   headers to pass (can be null).
         * @param object     The S3Object that is to be written (can be null).
         */
        private HttpURLConnection makeRequest(String method, String bucket, String key, Map pathArgs, Map headers,
                                              S3Object object)
                throws IOException {
            CallingFormat format=Utils.getCallingFormatForBucket(this.callingFormat, bucket);
            if(isSecure && format != CallingFormat.getPathCallingFormat() && bucket.contains(".")) {
                System.err.println("You are making an SSL connection, however, the bucket contains periods and the wildcard certificate will not match by default.  Please consider using HTTP.");
            }

            // build the domain based on the calling format
            URL url=format.getURL(isSecure, server, this.port, bucket, key, pathArgs);

            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setRequestMethod(method);

            // subdomain-style urls may encounter http redirects.
            // Ensure that redirects are supported.
            if(!connection.getInstanceFollowRedirects()
                    && format.supportsLocatedBuckets())
                throw MESSAGES.httpRedirectSupportRequired();

            addHeaders(connection, headers);
            if(object != null) addMetadataHeaders(connection, object.metadata);
            addAuthHeader(connection, method, bucket, key, pathArgs);

            return connection;
        }

        private HttpURLConnection makePreSignedRequest(String method, String preSignedUrl, Map headers) throws IOException {
            URL url = new URL(preSignedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);

            addHeaders(connection, headers);

            return connection;
        }

        /**
         * Add the given headers to the HttpURLConnection.
         * @param connection The HttpURLConnection to which the headers will be added.
         * @param headers    A Map of String to List of Strings representing the http
         *                   headers to pass (can be null).
         */
        private static void addHeaders(HttpURLConnection connection, Map headers) {
            addHeaders(connection, headers, "");
        }

        /**
         * Add the given metadata fields to the HttpURLConnection.
         * @param connection The HttpURLConnection to which the headers will be added.
         * @param metadata   A Map of String to List of Strings representing the s3
         *                   metadata for this resource.
         */
        private static void addMetadataHeaders(HttpURLConnection connection, Map metadata) {
            addHeaders(connection, metadata, Utils.METADATA_PREFIX);
        }

        /**
         * Add the given headers to the HttpURLConnection with a prefix before the keys.
         * @param connection The HttpURLConnection to which the headers will be added.
         * @param headers    A Map of String to List of Strings representing the http
         *                   headers to pass (can be null).
         * @param prefix     The string to prepend to each key before adding it to the connection.
         */
        private static void addHeaders(HttpURLConnection connection, Map headers, String prefix) {
            if(headers != null) {
                for(Iterator i=headers.keySet().iterator(); i.hasNext();) {
                    String key=(String)i.next();
                    for(Iterator j=((List)headers.get(key)).iterator(); j.hasNext();) {
                        String value=(String)j.next();
                        connection.addRequestProperty(prefix + key, value);
                    }
                }
            }
        }

        /**
         * Add the appropriate Authorization header to the HttpURLConnection.
         * @param connection The HttpURLConnection to which the header will be added.
         * @param method     The HTTP method to use (GET, PUT, DELETE)
         * @param bucket     the bucket name this request is for
         * @param key        the key this request is for
         * @param pathArgs   path arguments which are part of this request
         */
        private void addAuthHeader(HttpURLConnection connection, String method, String bucket, String key, Map pathArgs) {
            if(connection.getRequestProperty("Date") == null) {
                connection.setRequestProperty("Date", httpDate());
            }
            if(connection.getRequestProperty("Content-Type") == null) {
                connection.setRequestProperty("Content-Type", "");
            }

            if(this.awsAccessKeyId != null && this.awsSecretAccessKey != null) {
                String canonicalString=
                        Utils.makeCanonicalString(method, bucket, key, pathArgs, connection.getRequestProperties());
                String encodedCanonical=Utils.encode(this.awsSecretAccessKey, canonicalString, false);
                connection.setRequestProperty("Authorization",
                                              "AWS " + this.awsAccessKeyId + ":" + encodedCanonical);
            }
        }


        /**
         * Generate an rfc822 date for use in the Date HTTP header.
         */
        public static String httpDate() {
            final String DateFormat="EEE, dd MMM yyyy HH:mm:ss ";
            SimpleDateFormat format=new SimpleDateFormat(DateFormat, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format.format(new Date()) + "GMT";
        }
    }

    static class ListEntry {
        /**
         * The name of the object
         */
        public String key;

        /**
         * The date at which the object was last modified.
         */
        public Date lastModified;

        /**
         * The object's ETag, which can be used for conditional GETs.
         */
        public String eTag;

        /**
         * The size of the object in bytes.
         */
        public long size;

        /**
         * The object's storage class
         */
        public String storageClass;

        /**
         * The object's owner
         */
        public Owner owner;

        public String toString() {
            return key;
        }
    }

    static class Owner {
        public String id;
        public String displayName;
    }


    static class Response {
        public HttpURLConnection connection;

        public Response(HttpURLConnection connection) throws IOException {
            this.connection=connection;
        }
    }


    static class GetResponse extends Response {
        public S3Object object;

        /**
         * Pulls a representation of an S3Object out of the HttpURLConnection response.
         */
        public GetResponse(HttpURLConnection connection) throws IOException {
            super(connection);
            if(connection.getResponseCode() < 400) {
                Map metadata=extractMetadata(connection);
                byte[] body=slurpInputStream(connection.getInputStream());
                this.object=new S3Object(body, metadata);
            }
        }

        /**
         * Examines the response's header fields and returns a Map from String to List of Strings
         * representing the object's metadata.
         */
        private static Map extractMetadata(HttpURLConnection connection) {
            TreeMap metadata=new TreeMap();
            Map headers=connection.getHeaderFields();
            for(Iterator i=headers.keySet().iterator(); i.hasNext();) {
                String key=(String)i.next();
                if(key == null) continue;
                if(key.startsWith(Utils.METADATA_PREFIX)) {
                    metadata.put(key.substring(Utils.METADATA_PREFIX.length()), headers.get(key));
                }
            }

            return metadata;
        }

        /**
         * Read the input stream and dump it all into a big byte array
         */
        static byte[] slurpInputStream(InputStream stream) throws IOException {
            final int chunkSize=2048;
            byte[] buf=new byte[chunkSize];
            ByteArrayOutputStream byteStream=new ByteArrayOutputStream(chunkSize);
            int count;

            while((count=stream.read(buf)) != -1) byteStream.write(buf, 0, count);

            return byteStream.toByteArray();
        }
    }

    static class LocationResponse extends Response {
        String location;

        /**
         * Parse the response to a ?location query.
         */
        public LocationResponse(HttpURLConnection connection) throws IOException {
            super(connection);
            if(connection.getResponseCode() < 400) {
                try {
                    XMLReader xr=Utils.createXMLReader();
                    LocationResponseHandler handler=new LocationResponseHandler();
                    xr.setContentHandler(handler);
                    xr.setErrorHandler(handler);

                    xr.parse(new InputSource(connection.getInputStream()));
                    this.location=handler.loc;
                }
                catch(SAXException e) {
                    throw MESSAGES.errorParsingBucketListings(e);
                }
            }
            else {
                this.location="<error>";
            }
        }

        /**
         * Report the location-constraint for a bucket.
         * A value of null indicates an error;
         * the empty string indicates no constraint;
         * and any other value is an actual location constraint value.
         */
        public String getLocation() {
            return location;
        }

        /**
         * Helper class to parse LocationConstraint response XML
         */
        static class LocationResponseHandler extends DefaultHandler {
            String loc=null;
            private StringBuffer currText=null;

            public void startDocument() {
            }

            public void startElement(String uri, String name, String qName, Attributes attrs) {
                if(name.equals("LocationConstraint")) {
                    this.currText=new StringBuffer();
                }
            }

            public void endElement(String uri, String name, String qName) {
                if(name.equals("LocationConstraint")) {
                    loc=this.currText.toString();
                    this.currText=null;
                }
            }

            public void characters(char[] ch, int start, int length) {
                if(currText != null)
                    this.currText.append(ch, start, length);
            }
        }
    }


    static class Bucket {
        /**
         * The name of the bucket.
         */
        public String name;

        /**
         * The bucket's creation date.
         */
        public Date creationDate;

        public Bucket() {
            this.name=null;
            this.creationDate=null;
        }

        public Bucket(String name, Date creationDate) {
            this.name=name;
            this.creationDate=creationDate;
        }

        public String toString() {
            return this.name;
        }
    }

    static class ListBucketResponse extends Response {

        /**
         * The name of the bucket being listed.  Null if request fails.
         */
        public String name=null;

        /**
         * The prefix echoed back from the request.  Null if request fails.
         */
        public String prefix=null;

        /**
         * The marker echoed back from the request.  Null if request fails.
         */
        public String marker=null;

        /**
         * The delimiter echoed back from the request.  Null if not specified in
         * the request, or if it fails.
         */
        public String delimiter=null;

        /**
         * The maxKeys echoed back from the request if specified.  0 if request fails.
         */
        public int maxKeys=0;

        /**
         * Indicates if there are more results to the list.  True if the current
         * list results have been truncated.  false if request fails.
         */
        public boolean isTruncated=false;

        /**
         * Indicates what to use as a marker for subsequent list requests in the event
         * that the results are truncated.  Present only when a delimiter is specified.
         * Null if request fails.
         */
        public String nextMarker=null;

        /**
         * A List of ListEntry objects representing the objects in the given bucket.
         * Null if the request fails.
         */
        public List entries=null;

        /**
         * A List of CommonPrefixEntry objects representing the common prefixes of the
         * keys that matched up to the delimiter.  Null if the request fails.
         */
        public List commonPrefixEntries=null;

        public ListBucketResponse(HttpURLConnection connection) throws IOException {
            super(connection);
            if(connection.getResponseCode() < 400) {
                try {
                    XMLReader xr=Utils.createXMLReader();
                    ListBucketHandler handler=new ListBucketHandler();
                    xr.setContentHandler(handler);
                    xr.setErrorHandler(handler);

                    xr.parse(new InputSource(connection.getInputStream()));

                    this.name=handler.getName();
                    this.prefix=handler.getPrefix();
                    this.marker=handler.getMarker();
                    this.delimiter=handler.getDelimiter();
                    this.maxKeys=handler.getMaxKeys();
                    this.isTruncated=handler.getIsTruncated();
                    this.nextMarker=handler.getNextMarker();
                    this.entries=handler.getKeyEntries();
                    this.commonPrefixEntries=handler.getCommonPrefixEntries();

                }
                catch(SAXException e) {
                    throw MESSAGES.errorParsingBucketListings(e);
                }
            }
        }

        static class ListBucketHandler extends DefaultHandler {

            private String name=null;
            private String prefix=null;
            private String marker=null;
            private String delimiter=null;
            private int maxKeys=0;
            private boolean isTruncated=false;
            private String nextMarker=null;
            private boolean isEchoedPrefix=false;
            private List keyEntries=null;
            private ListEntry keyEntry=null;
            private List commonPrefixEntries=null;
            private CommonPrefixEntry commonPrefixEntry=null;
            private StringBuffer currText=null;
            private SimpleDateFormat iso8601Parser=null;

            public ListBucketHandler() {
                super();
                keyEntries=new ArrayList();
                commonPrefixEntries=new ArrayList();
                this.iso8601Parser=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                this.iso8601Parser.setTimeZone(new SimpleTimeZone(0, "GMT"));
                this.currText=new StringBuffer();
            }

            public void startDocument() {
                this.isEchoedPrefix=true;
            }

            public void endDocument() {
                // ignore
            }

            public void startElement(String uri, String name, String qName, Attributes attrs) {
                if(name.equals("Contents")) {
                    this.keyEntry=new ListEntry();
                }
                else if(name.equals("Owner")) {
                    this.keyEntry.owner=new Owner();
                }
                else if(name.equals("CommonPrefixes")) {
                    this.commonPrefixEntry=new CommonPrefixEntry();
                }
            }

            public void endElement(String uri, String name, String qName) {
                if(name.equals("Name")) {
                    this.name=this.currText.toString();
                }
                // this prefix is the one we echo back from the request
                else if(name.equals("Prefix") && this.isEchoedPrefix) {
                    this.prefix=this.currText.toString();
                    this.isEchoedPrefix=false;
                }
                else if(name.equals("Marker")) {
                    this.marker=this.currText.toString();
                }
                else if(name.equals("MaxKeys")) {
                    this.maxKeys=Integer.parseInt(this.currText.toString());
                }
                else if(name.equals("Delimiter")) {
                    this.delimiter=this.currText.toString();
                }
                else if(name.equals("IsTruncated")) {
                    this.isTruncated=Boolean.valueOf(this.currText.toString());
                }
                else if(name.equals("NextMarker")) {
                    this.nextMarker=this.currText.toString();
                }
                else if(name.equals("Contents")) {
                    this.keyEntries.add(this.keyEntry);
                }
                else if(name.equals("Key")) {
                    this.keyEntry.key=this.currText.toString();
                }
                else if(name.equals("LastModified")) {
                    try {
                        this.keyEntry.lastModified=this.iso8601Parser.parse(this.currText.toString());
                    }
                    catch(ParseException e) {
                        throw MESSAGES.errorParsingBucketListings(e);
                    }
                }
                else if(name.equals("ETag")) {
                    this.keyEntry.eTag=this.currText.toString();
                }
                else if(name.equals("Size")) {
                    this.keyEntry.size=Long.parseLong(this.currText.toString());
                }
                else if(name.equals("StorageClass")) {
                    this.keyEntry.storageClass=this.currText.toString();
                }
                else if(name.equals("ID")) {
                    this.keyEntry.owner.id=this.currText.toString();
                }
                else if(name.equals("DisplayName")) {
                    this.keyEntry.owner.displayName=this.currText.toString();
                }
                else if(name.equals("CommonPrefixes")) {
                    this.commonPrefixEntries.add(this.commonPrefixEntry);
                }
                // this is the common prefix for keys that match up to the delimiter
                else if(name.equals("Prefix")) {
                    this.commonPrefixEntry.prefix=this.currText.toString();
                }
                if(this.currText.length() != 0)
                    this.currText=new StringBuffer();
            }

            public void characters(char[] ch, int start, int length) {
                this.currText.append(ch, start, length);
            }

            public String getName() {
                return this.name;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public String getMarker() {
                return this.marker;
            }

            public String getDelimiter() {
                return this.delimiter;
            }

            public int getMaxKeys() {
                return this.maxKeys;
            }

            public boolean getIsTruncated() {
                return this.isTruncated;
            }

            public String getNextMarker() {
                return this.nextMarker;
            }

            public List getKeyEntries() {
                return this.keyEntries;
            }

            public List getCommonPrefixEntries() {
                return this.commonPrefixEntries;
            }
        }
    }


    static class CommonPrefixEntry {
        /**
         * The prefix common to the delimited keys it represents
         */
        public String prefix;
    }


    static class ListAllMyBucketsResponse extends Response {
        /**
         * A list of Bucket objects, one for each of this account's buckets.  Will be null if
         * the request fails.
         */
        public List entries;

        public ListAllMyBucketsResponse(HttpURLConnection connection) throws IOException {
            super(connection);
            if(connection.getResponseCode() < 400) {
                try {
                    XMLReader xr=Utils.createXMLReader();
                    ListAllMyBucketsHandler handler=new ListAllMyBucketsHandler();
                    xr.setContentHandler(handler);
                    xr.setErrorHandler(handler);

                    xr.parse(new InputSource(connection.getInputStream()));
                    this.entries=handler.getEntries();
                }
                catch(SAXException e) {
                    throw MESSAGES.errorParsingBucketListings(e);
                }
            }
        }

        static class ListAllMyBucketsHandler extends DefaultHandler {

            private List entries=null;
            private Bucket currBucket=null;
            private StringBuffer currText=null;
            private SimpleDateFormat iso8601Parser=null;

            public ListAllMyBucketsHandler() {
                super();
                entries=new ArrayList();
                this.iso8601Parser=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                this.iso8601Parser.setTimeZone(new SimpleTimeZone(0, "GMT"));
                this.currText=new StringBuffer();
            }

            public void startDocument() {
                // ignore
            }

            public void endDocument() {
                // ignore
            }

            public void startElement(String uri, String name, String qName, Attributes attrs) {
                if(name.equals("Bucket")) {
                    this.currBucket=new Bucket();
                }
            }

            public void endElement(String uri, String name, String qName) {
                if(name.equals("Bucket")) {
                    this.entries.add(this.currBucket);
                }
                else if(name.equals("Name")) {
                    this.currBucket.name=this.currText.toString();
                }
                else if(name.equals("CreationDate")) {
                    try {
                        this.currBucket.creationDate=this.iso8601Parser.parse(this.currText.toString());
                    }
                    catch(ParseException e) {
                        throw MESSAGES.errorParsingBucketListings(e);
                    }
                }
                this.currText=new StringBuffer();
            }

            public void characters(char[] ch, int start, int length) {
                this.currText.append(ch, start, length);
            }

            public List getEntries() {
                return this.entries;
            }
        }
    }


    static class S3Object {

        public byte[] data;

        /**
         * A Map from String to List of Strings representing the object's metadata
         */
        public Map metadata;

        public S3Object(byte[] data, Map metadata) {
            this.data=data;
            this.metadata=metadata;
        }
    }


    abstract static class CallingFormat {

        protected static CallingFormat pathCallingFormat=new PathCallingFormat();
        protected static CallingFormat subdomainCallingFormat=new SubdomainCallingFormat();
        protected static CallingFormat vanityCallingFormat=new VanityCallingFormat();

        public abstract boolean supportsLocatedBuckets();

        public abstract String getEndpoint(String server, int port, String bucket);

        public abstract String getPathBase(String bucket, String key);

        public abstract URL getURL(boolean isSecure, String server, int port, String bucket, String key, Map pathArgs)
                throws MalformedURLException;

        public static CallingFormat getPathCallingFormat() {
            return pathCallingFormat;
        }

        public static CallingFormat getSubdomainCallingFormat() {
            return subdomainCallingFormat;
        }

        public static CallingFormat getVanityCallingFormat() {
            return vanityCallingFormat;
        }

        private static class PathCallingFormat extends CallingFormat {
            public boolean supportsLocatedBuckets() {
                return false;
            }

            public String getPathBase(String bucket, String key) {
                return isBucketSpecified(bucket)? "/" + bucket + "/" + key : "/";
            }

            public String getEndpoint(String server, int port, String bucket) {
                return server + ":" + port;
            }

            public URL getURL(boolean isSecure, String server, int port, String bucket, String key, Map pathArgs)
                    throws MalformedURLException {
                String pathBase=isBucketSpecified(bucket)? "/" + bucket + "/" + key : "/";
                String pathArguments=Utils.convertPathArgsHashToString(pathArgs);
                return new URL(isSecure? "https" : "http", server, port, pathBase + pathArguments);
            }

            private static boolean isBucketSpecified(String bucket) {
                return bucket != null && bucket.length() != 0;
            }
        }

        private static class SubdomainCallingFormat extends CallingFormat {
            public boolean supportsLocatedBuckets() {
                return true;
            }

            public String getServer(String server, String bucket) {
                return bucket + "." + server;
            }

            public String getEndpoint(String server, int port, String bucket) {
                return getServer(server, bucket) + ":" + port;
            }

            public String getPathBase(String bucket, String key) {
                return "/" + key;
            }

            public URL getURL(boolean isSecure, String server, int port, String bucket, String key, Map pathArgs)
                    throws MalformedURLException {
                if(bucket == null || bucket.length() == 0) {
                    //The bucket is null, this is listAllBuckets request
                    String pathArguments=Utils.convertPathArgsHashToString(pathArgs);
                    return new URL(isSecure? "https" : "http", server, port, "/" + pathArguments);
                }
                else {
                    String serverToUse=getServer(server, bucket);
                    String pathBase=getPathBase(bucket, key);
                    String pathArguments=Utils.convertPathArgsHashToString(pathArgs);
                    return new URL(isSecure? "https" : "http", serverToUse, port, pathBase + pathArguments);
                }
            }
        }

        private static class VanityCallingFormat extends SubdomainCallingFormat {
            public String getServer(String server, String bucket) {
                return bucket;
            }
        }
    }

    static class Utils {
        static final String METADATA_PREFIX="x-amz-meta-";
        static final String AMAZON_HEADER_PREFIX="x-amz-";
        static final String ALTERNATIVE_DATE_HEADER="x-amz-date";
        public static final String DEFAULT_HOST="s3.amazonaws.com";

        public static final int SECURE_PORT=443;
        public static final int INSECURE_PORT=80;


        /**
         * HMAC/SHA1 Algorithm per RFC 2104.
         */
        private static final String HMAC_SHA1_ALGORITHM="HmacSHA1";

        static String makeCanonicalString(String method, String bucket, String key, Map pathArgs, Map headers) {
            return makeCanonicalString(method, bucket, key, pathArgs, headers, null);
        }

        /**
         * Calculate the canonical string.  When expires is non-null, it will be
         * used instead of the Date header.
         */
        static String makeCanonicalString(String method, String bucketName, String key, Map pathArgs,
                                          Map headers, String expires) {
            StringBuilder buf=new StringBuilder();
            buf.append(method + "\n");

            // Add all interesting headers to a list, then sort them.  "Interesting"
            // is defined as Content-MD5, Content-Type, Date, and x-amz-
            SortedMap interestingHeaders=new TreeMap();
            if(headers != null) {
                for(Iterator i=headers.keySet().iterator(); i.hasNext();) {
                    String hashKey=(String)i.next();
                    if(hashKey == null) continue;
                    String lk=hashKey.toLowerCase();

                    // Ignore any headers that are not particularly interesting.
                    if(lk.equals("content-type") || lk.equals("content-md5") || lk.equals("date") ||
                            lk.startsWith(AMAZON_HEADER_PREFIX)) {
                        List s=(List)headers.get(hashKey);
                        interestingHeaders.put(lk, concatenateList(s));
                    }
                }
            }

            if(interestingHeaders.containsKey(ALTERNATIVE_DATE_HEADER)) {
                interestingHeaders.put("date", "");
            }

            // if the expires is non-null, use that for the date field.  this
            // trumps the x-amz-date behavior.
            if(expires != null) {
                interestingHeaders.put("date", expires);
            }

            // these headers require that we still put a new line in after them,
            // even if they don't exist.
            if(!interestingHeaders.containsKey("content-type")) {
                interestingHeaders.put("content-type", "");
            }
            if(!interestingHeaders.containsKey("content-md5")) {
                interestingHeaders.put("content-md5", "");
            }

            // Finally, add all the interesting headers (i.e.: all that startwith x-amz- ;-))
            for(Iterator i=interestingHeaders.keySet().iterator(); i.hasNext();) {
                String headerKey=(String)i.next();
                if(headerKey.startsWith(AMAZON_HEADER_PREFIX)) {
                    buf.append(headerKey).append(':').append(interestingHeaders.get(headerKey));
                }
                else {
                    buf.append(interestingHeaders.get(headerKey));
                }
                buf.append("\n");
            }

            // build the path using the bucket and key
            if(bucketName != null && bucketName.length() != 0) {
                buf.append("/" + bucketName);
            }

            // append the key (it might be an empty string)
            // append a slash regardless
            buf.append("/");
            if(key != null) {
                buf.append(key);
            }

            // if there is an acl, logging or torrent parameter
            // add them to the string
            if(pathArgs != null) {
                if(pathArgs.containsKey("acl")) {
                    buf.append("?acl");
                }
                else if(pathArgs.containsKey("torrent")) {
                    buf.append("?torrent");
                }
                else if(pathArgs.containsKey("logging")) {
                    buf.append("?logging");
                }
                else if(pathArgs.containsKey("location")) {
                    buf.append("?location");
                }
            }

            return buf.toString();

        }

        /**
         * Calculate the HMAC/SHA1 on a string.
         * @return Signature
         * @throws java.security.NoSuchAlgorithmException
         *          If the algorithm does not exist.  Unlikely
         * @throws java.security.InvalidKeyException
         *          If the key is invalid.
         */
        static String encode(String awsSecretAccessKey, String canonicalString,
                             boolean urlencode) {
            // The following HMAC/SHA1 code for the signature is taken from the
            // AWS Platform's implementation of RFC2104 (amazon.webservices.common.Signature)
            //
            // Acquire an HMAC/SHA1 from the raw key bytes.
            SecretKeySpec signingKey=
                    new SecretKeySpec(awsSecretAccessKey.getBytes(), HMAC_SHA1_ALGORITHM);

            // Acquire the MAC instance and initialize with the signing key.
            Mac mac=null;
            try {
                mac=Mac.getInstance(HMAC_SHA1_ALGORITHM);
            }
            catch(NoSuchAlgorithmException e) {
                // should not happen
                throw new RuntimeException(e.getLocalizedMessage());
            }
            try {
                mac.init(signingKey);
            }
            catch(InvalidKeyException e) {
                // also should not happen
                throw new RuntimeException(e.getLocalizedMessage());
            }

            // Compute the HMAC on the digest, and set it.
            String b64=Base64.encodeBytes(mac.doFinal(canonicalString.getBytes()));

            if(urlencode) {
                return urlencode(b64);
            }
            else {
                return b64;
            }
        }

        static Map paramsForListOptions(String prefix, String marker, Integer maxKeys) {
            return paramsForListOptions(prefix, marker, maxKeys, null);
        }

        static Map paramsForListOptions(String prefix, String marker, Integer maxKeys, String delimiter) {

            Map argParams=new HashMap();
            // these three params must be url encoded
            if(prefix != null)
                argParams.put("prefix", urlencode(prefix));
            if(marker != null)
                argParams.put("marker", urlencode(marker));
            if(delimiter != null)
                argParams.put("delimiter", urlencode(delimiter));

            if(maxKeys != null)
                argParams.put("max-keys", Integer.toString(maxKeys.intValue()));

            return argParams;

        }

        /**
         * Converts the Path Arguments from a map to String which can be used in url construction
         * @param pathArgs a map of arguments
         * @return a string representation of pathArgs
         */
        public static String convertPathArgsHashToString(Map pathArgs) {
            StringBuilder pathArgsString=new StringBuilder();
            String argumentValue;
            boolean firstRun=true;
            if(pathArgs != null) {
                for(Iterator argumentIterator=pathArgs.keySet().iterator(); argumentIterator.hasNext();) {
                    String argument=(String)argumentIterator.next();
                    if(firstRun) {
                        firstRun=false;
                        pathArgsString.append("?");
                    }
                    else {
                        pathArgsString.append("&");
                    }

                    argumentValue=(String)pathArgs.get(argument);
                    pathArgsString.append(argument);
                    if(argumentValue != null) {
                        pathArgsString.append("=");
                        pathArgsString.append(argumentValue);
                    }
                }
            }

            return pathArgsString.toString();
        }


        static String urlencode(String unencoded) {
            try {
                return URLEncoder.encode(unencoded, "UTF-8");
            }
            catch(UnsupportedEncodingException e) {
                // should never happen
                throw new RuntimeException(e.getLocalizedMessage());
            }
        }

        static XMLReader createXMLReader() {
            try {
                return XMLReaderFactory.createXMLReader();
            }
            catch(SAXException e) {
                // oops, lets try doing this (needed in 1.4)
                System.setProperty("org.xml.sax.driver", "org.apache.crimson.parser.XMLReaderImpl");
            }
            try {
                // try once more
                return XMLReaderFactory.createXMLReader();
            }
            catch(SAXException e) {
                throw MESSAGES.cannotInitializeSaxDriver();
            }
        }

        /**
         * Concatenates a bunch of header values, seperating them with a comma.
         * @param values List of header values.
         * @return String of all headers, with commas.
         */
        private static String concatenateList(List values) {
            StringBuilder buf=new StringBuilder();
            for(int i=0, size=values.size(); i < size; ++i) {
                buf.append(((String)values.get(i)).replaceAll("\n", "").trim());
                if(i != (size - 1)) {
                    buf.append(",");
                }
            }
            return buf.toString();
        }

        /**
         * Validate bucket-name
         */
        static boolean validateBucketName(String bucketName, CallingFormat callingFormat) {
            if(callingFormat == CallingFormat.getPathCallingFormat()) {
                final int MIN_BUCKET_LENGTH=3;
                final int MAX_BUCKET_LENGTH=255;
                final String BUCKET_NAME_REGEX="^[0-9A-Za-z\\.\\-_]*$";

                return null != bucketName &&
                        bucketName.length() >= MIN_BUCKET_LENGTH &&
                        bucketName.length() <= MAX_BUCKET_LENGTH &&
                        bucketName.matches(BUCKET_NAME_REGEX);
            }
            else {
                return isValidSubdomainBucketName(bucketName);
            }
        }

        static boolean isValidSubdomainBucketName(String bucketName) {
            final int MIN_BUCKET_LENGTH=3;
            final int MAX_BUCKET_LENGTH=63;
            // don't allow names that look like 127.0.0.1
            final String IPv4_REGEX="^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$";
            // dns sub-name restrictions
            final String BUCKET_NAME_REGEX="^[a-z0-9]([a-z0-9\\-\\_]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9\\-\\_]*[a-z0-9])?)*$";

            // If there wasn't a location-constraint, then the current actual
            // restriction is just that no 'part' of the name (i.e. sequence
            // of characters between any 2 '.'s has to be 63) but the recommendation
            // is to keep the entire bucket name under 63.
            return null != bucketName &&
                    bucketName.length() >= MIN_BUCKET_LENGTH &&
                    bucketName.length() <= MAX_BUCKET_LENGTH &&
                    !bucketName.matches(IPv4_REGEX) &&
                    bucketName.matches(BUCKET_NAME_REGEX);
        }

        static CallingFormat getCallingFormatForBucket(CallingFormat desiredFormat, String bucketName) {
            CallingFormat callingFormat=desiredFormat;
            if(callingFormat == CallingFormat.getSubdomainCallingFormat() && !Utils.isValidSubdomainBucketName(bucketName)) {
                callingFormat=CallingFormat.getPathCallingFormat();
            }
            return callingFormat;
        }

        public static String generateQueryStringAuthentication(String awsAccessKey, String awsSecretAccessKey,
                                                               String method, String bucket, String key,
                                                               Map pathArgs, Map headers) {
            int defaultExpiresIn = 300; // 5 minutes
            long expirationDate = (System.currentTimeMillis() / 1000) + defaultExpiresIn;
            return generateQueryStringAuthentication(awsAccessKey, awsSecretAccessKey,
                                                     method, bucket, key,
                                                     pathArgs, headers, expirationDate);
        }

        public static String generateQueryStringAuthentication(String awsAccessKey, String awsSecretAccessKey,
                                                               String method, String bucket, String key,
                                                               Map pathArgs, Map headers, long expirationDate) {
            method = method.toUpperCase(); // Method should always be uppercase
            String canonicalString =
                makeCanonicalString(method, bucket, key, pathArgs, headers, "" + expirationDate);
            String encodedCanonical = encode(awsSecretAccessKey, canonicalString, true);
            return "http://" + DEFAULT_HOST + "/" + bucket + "/" + key + "?" +
                "AWSAccessKeyId=" + awsAccessKey + "&Expires=" + expirationDate +
                "&Signature=" + encodedCanonical;
        }
    }
}
