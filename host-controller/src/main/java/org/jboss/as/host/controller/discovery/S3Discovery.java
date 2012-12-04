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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.host.controller.discovery.S3Util.AWSAuthConnection;
import org.jboss.as.host.controller.discovery.S3Util.Bucket;
import org.jboss.as.host.controller.discovery.S3Util.GetResponse;
import org.jboss.as.host.controller.discovery.S3Util.ListAllMyBucketsResponse;
import org.jboss.as.host.controller.discovery.S3Util.ListBucketResponse;
import org.jboss.as.host.controller.discovery.S3Util.ListEntry;
import org.jboss.as.host.controller.discovery.S3Util.PreSignedUrlParser;
import org.jboss.as.host.controller.discovery.S3Util.S3Object;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handle domain controller discovery via Amazon's S3 storage.
 * The S3 access code reuses the example shipped by Amazon.
 *
 * @author Farah Juma
 */
public class S3Discovery implements DiscoveryOption {

    // The name of the S3 file that will store the domain controller's host and port
    private static final String DC_FILE_NAME="jboss-domain-master-data";

    private String access_key = null;
    private String secret_access_key = null;
    private String location = null;
    private String prefix = null;
    private String pre_signed_put_url = null;
    private String pre_signed_delete_url = null;
    private AWSAuthConnection conn = null;
    private String remoteDcHost;
    private int remoteDcPort;

    /**
     * Create the S3Discovery option.
     *
     * @param access_key the access key to AWS (S3)
     * @param secret_access_key the secret access key to AWS (S3)
     * @param location the name of the S3 bucket
     * @param prefix the name of the S3 bucket prefix
     * @param pre_signed_put_url the pre-signed URL for PUTs
     * @param pre_signed_delete_url the pre-signed URL for DELETEs
     */
    public S3Discovery(String access_key, String secret_access_key, String location, String prefix,
            String pre_signed_put_url, String pre_signed_delete_url) {
        this.access_key = access_key;
        this.secret_access_key = secret_access_key;
        this.location = location;
        this.prefix = prefix;
        this.pre_signed_put_url = pre_signed_put_url;
        this.pre_signed_delete_url = pre_signed_delete_url;
    }

    @Override
    public void allowDiscovery(String host, int port) {
        try {
            // Write the domain controller data to an S3 file
            writeToFile(new DomainControllerData(host, port), MASTER);
        } catch (Exception e) {
            ROOT_LOGGER.cannotWriteDomainControllerData(e);
        }
    }

    @Override
    public void discover() {
        // Read the domain controller data from an S3 file
        DomainControllerData data = readFromFile(MASTER);

        if (data != null) {
            // Validate and set the host and port
            String host = data.getHost();
            int port = data.getPort();
            try {
                RemoteDomainControllerAddHandler.HOST.getValidator()
                    .validateParameter(RemoteDomainControllerAddHandler.HOST.getName(), new ModelNode().set(host));
                RemoteDomainControllerAddHandler.PORT.getValidator()
                    .validateParameter(RemoteDomainControllerAddHandler.PORT.getName(), new ModelNode().set(port));
            } catch (OperationFailedException e){
                throw new IllegalStateException(e.getFailureDescription().asString());
            }
            setRemoteDomainControllerHost(host);
            setRemoteDomainControllerPort(port);
        } else {
            throw MESSAGES.failedMarshallingDomainControllerData();
        }
    }

    @Override
    public void cleanUp() {
        // Remove the S3 file
        remove(MASTER);
    }

    @Override
    public String getRemoteDomainControllerHost() {
        return remoteDcHost;
    }

    @Override
    public int getRemoteDomainControllerPort() {
        return remoteDcPort;
    }

    /**
     * Determine whether or not pre-signed URLs will be used.
     */
    private boolean usingPreSignedUrls() {
        return pre_signed_put_url != null;
    }

    /**
     * Make sure {@code pre_signed_put_url} and {@code pre_signed_delete_url} are valid.
     */
    private void validatePreSignedUrls() {
        if (pre_signed_put_url != null && pre_signed_delete_url != null) {
            PreSignedUrlParser parsedPut = new PreSignedUrlParser(pre_signed_put_url);
            PreSignedUrlParser parsedDelete = new PreSignedUrlParser(pre_signed_delete_url);
            if (!parsedPut.getBucket().equals(parsedDelete.getBucket()) ||
                    !parsedPut.getPrefix().equals(parsedDelete.getPrefix())) {
                throw MESSAGES.preSignedUrlsMustHaveSamePath();
            }
        } else if (pre_signed_put_url != null || pre_signed_delete_url != null) {
            throw MESSAGES.preSignedUrlsMustBeSetOrUnset();
        }
    }

    /**
     * Do the set-up that's needed to access Amazon S3.
     */
    private void init() {
        validatePreSignedUrls();

        try {
            conn = new AWSAuthConnection(access_key, secret_access_key);
            // Determine the bucket name if prefix is set or if pre-signed URLs are being used
            if (prefix != null && prefix.length() > 0) {
                ListAllMyBucketsResponse bucket_list = conn.listAllMyBuckets(null);
                List buckets = bucket_list.entries;
                if (buckets != null) {
                    boolean found = false;
                    for (Object tmp : buckets) {
                        if (tmp instanceof Bucket) {
                            Bucket bucket = (Bucket) tmp;
                            if (bucket.name.startsWith(prefix)) {
                                location = bucket.name;
                                found = true;
                            }
                        }
                    }
                    if (!found) {
                        location = prefix + "-" + java.util.UUID.randomUUID().toString();
                    }
                }
            }
            if (usingPreSignedUrls()) {
                PreSignedUrlParser parsedPut = new PreSignedUrlParser(pre_signed_put_url);
                location = parsedPut.getBucket();
            }
            if (!conn.checkBucketExists(location)) {
                conn.createBucket(location, AWSAuthConnection.LOCATION_DEFAULT, null).connection.getResponseMessage();
            }
        } catch (Exception e) {
            throw MESSAGES.cannotAccessS3Bucket(location, e.getLocalizedMessage());
        }
    }

    /**
     * Read the domain controller data from an S3 file.
     *
     * @param directoryName the name of the directory in the bucket that contains the S3 file
     * @return the domain controller data
     */
    private DomainControllerData readFromFile(String directoryName) {
        if(directoryName == null) {
            return null;
        }

        if (conn == null) {
            init();
        }

        DomainControllerData data = null;
        try {
            if (usingPreSignedUrls()) {
                PreSignedUrlParser parsedPut = new PreSignedUrlParser(pre_signed_put_url);
                directoryName = parsedPut.getPrefix();
            }
            String key = S3Util.sanitize(directoryName) + "/" + S3Util.sanitize(DC_FILE_NAME);
            GetResponse val = conn.get(location, key, null);
            if (val.object != null) {
                byte[] buf = val.object.data;
                if (buf != null && buf.length > 0) {
                    try {
                        data = S3Util.domainControllerDataFromByteBuffer(buf);
                    } catch (Exception e) {
                        throw MESSAGES.failedMarshallingDomainControllerData();
                    }
                }
            }
            return data;
        } catch (IOException e) {
            throw MESSAGES.cannotAccessS3File(e.getLocalizedMessage());
        }
    }

    /**
     * Write the domain controller data to an S3 file.
     *
     * @param data the domain controller data
     * @param domainName the name of the directory in the bucket to write the S3 file to
     * @throws IOException
     */
    private void writeToFile(DomainControllerData data, String domainName) throws IOException {
        if(domainName == null || data == null) {
            return;
        }

        if (conn == null) {
            init();
        }

        try {
            String key = S3Util.sanitize(domainName) + "/" + S3Util.sanitize(DC_FILE_NAME);
            byte[] buf = S3Util.domainControllerDataToByteBuffer(data);
            S3Object val = new S3Object(buf, null);
            if (usingPreSignedUrls()) {
                Map headers = new TreeMap();
                headers.put("x-amz-acl", Arrays.asList("public-read"));
                conn.put(pre_signed_put_url, val, headers).connection.getResponseMessage();
            } else {
                Map headers = new TreeMap();
                headers.put("Content-Type", Arrays.asList("text/plain"));
                conn.put(location, key, val, headers).connection.getResponseMessage();
            }
        }
        catch(Exception e) {
            throw MESSAGES.cannotWriteToS3File(e.getLocalizedMessage());
        }
    }

    /**
     * Remove the S3 file that contains the domain controller data.
     *
     * @param directoryName the name of the directory that contains the S3 file
     */
    private void remove(String directoryName) {
        if ((directoryName == null) || (conn == null))
            return;

        String key = S3Util.sanitize(directoryName) + "/" + S3Util.sanitize(DC_FILE_NAME);
        try {
            Map headers = new TreeMap();
            headers.put("Content-Type", Arrays.asList("text/plain"));
            if (usingPreSignedUrls()) {
                conn.delete(pre_signed_delete_url).connection.getResponseMessage();
            } else {
                conn.delete(location, key, headers).connection.getResponseMessage();
            }
        }
        catch(Exception e) {
            ROOT_LOGGER.cannotRemoveS3File(e);
        }
    }

    private void setRemoteDomainControllerHost(String host) {
        remoteDcHost = host;
    }

    private void setRemoteDomainControllerPort(int port) {
        remoteDcPort = port;
    }
}
