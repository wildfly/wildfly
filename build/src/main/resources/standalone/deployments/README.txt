The standalone/deployments directory in the JBoss Application Server 
distribution is the location end users can place their deployment content 
(e.g. war, ear, jar, sar files) to have it automically deployed into the server 
runtime.

Users, particularly those running production systems, are encouraged to use the 
JBoss AS management APIs to upload and deploy deployment content instead of 
relying on the deployment scanner subsystem that periodically scans this 
directory.  See the JBoss AS documentation for details.

The filesystem deployment scanner in AS 7 and later works differently from 
previous JBoss AS releases. The scanner will no longer attempt to directly 
monitor the deployment content and decide if or when the end user wishes 
the content to be deployed. Instead, the scanner relies on a system of marker 
files, with the user's addition or removal of a marker file serving as a sort
of command telling the scanner to deploy, undeploy or redeploy content.

The marker files always have the same name as the deployment content to which
they relate, but with an additional file suffix appended. For example, the 
marker file to indicate the example.war should be deployed is named 
example.war.dodeploy. Different marker file suffixes have different meanings.

The relevant marker file types are:

.dodeploy     -- Placed by the user to indicate that the given content should 
                 be deployed into the runtime (or redeployed if already 
                 deployed in the runtime.)

.deploying    -- Placed by the deployment scanner service to indicate that it 
                 has noticed a .dodeploy file and is in the process of 
                 deploying the content. This marker file will be deleted when 
                 the deployment process completes.
              
.isdeployed   -- Placed by the deployment scanner service to indicate that the 
                 given content has been deployed into the runtime. If an end 
                 user deletes this file, the content will be undeployed.
               
.faileddeploy -- Placed by the deployment scanner service to indicate that the 
                 given content failed to deploy into the runtime. The content 
                 of the file will include some information about the cause of 
                 the failure.

.undeploying  -- Placed by the deployment scanner service to indicate that it 
                 has noticed a .isdeployed file has been deleted and the 
                 content is being undeployed. This marker file will be deleted 
                 when the undeployment process completes.
              
.undeployed   -- Placed by the deployment scanner service to indicate that the 
                 given content has been undeployed from the runtime. If an end 
                 user deletes this file, it has no impact.
                 
Basic workflows:

All examples assume variable $AS points to the root of the JBoss AS distribution.

A) Add new zipped content and deploy it:

1. cp target/example.war/ $AS/standalone/deployment
2. touch $AS/standalone/deployment/example.war.dodeploy

B) Add new unzipped content and deploy it:

1. cp -r target/example.war/ $AS/standalone/deployment
2. touch $AS/standalone/deployment/example.war.dodeploy

C) Undeploy currently deployed content:

1. rm $AS/standalone/deployment/example.war.isdeployed

D) Replace currently deployed zipped content with a new version and deploy it:

1. cp target/example.war/ $AS/standalone/deployment
2. touch $AS/standalone/deployment/example.war.dodeploy

E) Replace currently deployed unzipped content with a new version and deploy 
   it:

1. rm $AS/standalone/deployment/example.war.isdeployed
2. wait for $AS/standalone/deployment/example.war.undeployed file to appear
3. cp -r target/example.war/ $AS/standalone/deployment
4. touch $AS/standalone/deployment/example.war.dodeploy

F) Redeploy currently deployed content (i.e. bounce it with no content 
   change):

1. touch $AS/standalone/deployment/example.war.dodeploy
