import os
import re
import zipfile
import urllib2
import tempfile

try:
    import json
except ImportError:
    import simplejson as json

from sos.plugins import Plugin, IndependentPlugin
from sos.utilities import DirTree, find, checksum

class Request(object):

    def __init__(self, resource, operation="read-resource", parameters=None):
        self.resource = resource
        self.operation = operation
        if parameters:
            self.parameters = parameters
        else:
            self.parameters = {}

    def url_parts(self):
        """Generator function to split a url into (key, value) tuples. The url
        should contain an even number of pairs.  In the case of / the generator
        will immediately stop iteration."""
        parts = self.resource.strip("/").split("/")

        if parts == ['']:
            raise StopIteration

        while parts:
            yield (parts.pop(0), parts.pop(0))


class AS7(Plugin, IndependentPlugin):
    """JBoss related information
    """

    requires_root = False

    version = "1.0"

    optionList = [
          ("home",  "JBoss's installation dir (i.e. JBOSS_HOME)", '', False),
          ("logsize", 'max size (MiB) to collect per log file', '', 15),
          ("stdjar",  'Collect jar statistics for standard jars.', '', True),
          ("host", 'hostname of the management api for jboss', '', 'localhost'),
          ("port", 'port of the management api for jboss', '', '9990'),
          ("user", 'username for management console', '', None),
          ("pass", 'password for management console', '', None),
          ("appxml",  "comma separated list of application's whose XML descriptors you want. The keyword 'all' will collect all descriptors in the designated profile(s).", '', False),
    ]

    __MD5_CHUNK_SIZE=128
    __jbossHome=None
    __haveJava=False
    __twiddleCmd=None
    __jbossServerConfigDirs = ["standalone", "domain"]
    __jbossHTMLBody=None

    def __alert(self, msg):
        self.soslog.error(msg)
        self.addAlert(msg)

    def __getJbossHome(self):
        """
        Will attempt to locate the JBoss installation dir in either jboss.home or
        scrape it from the environment variable JBOSS_HOME.
        Returns:
            True JBOSS_HOME is set and the path exists.  False otherwise.
        """

        if self.getOption("home"):
            ## Prefer this value first over the ENV
            self.__jbossHome=self.getOption(("home", "as7_home"))
            self.addAlert("INFO: The JBoss installation directory supplied to SOS is " +
                          self.__jbossHome)
        elif os.environ.get("JBOSS_HOME"):
            self.__jbossHome=os.environ.get("JBOSS_HOME")
            self.addAlert("INFO: The JBoss installation directory (i.e. JBOSS_HOME) from the environment is " +
                          self.__jbossHome)
        else:
            self.addAlert("ERROR: The JBoss installation directory was not supplied.\
              The JBoss SOS plug-in cannot continue.")
            return False

        return True

    def __getMd5(self, file):
        """Returns the MD5 sum of the specified file."""

        retVal = "?" * 32

        try:
            retVal = checksum(file, self.__MD5_CHUNK_SIZE)
        except IOError, ioe:
            self.__alert("ERROR: Unable to open %s for reading.  Error: %s" % (file,ioe))

        return retVal

    def __getManifest(self, jarFile):
        """
        Given a jar file, this function will extract the Manifest and return it's contents
        as a string.
        """
        manifest = None
        try:
            zf = zipfile.ZipFile(jarFile)
            try:
                manifest = zf.read("META-INF/MANIFEST.MF")
            except Exception, e:
                self.__alert("ERROR: reading manifest from %s.  Error: %s" % (jarFile, e))
            zf.close()
        except Exception, e:
                self.__alert("ERROR: reading contents of %s.  Error: %s" % (jarFile, e))
        return manifest

    def __getStdJarInfo(self):
        found = False
        jar_info_list = []
        for jarFile in find("*.jar", self.__jbossHome):
            checksum = self.__getMd5(jarFile)
            manifest = self.__getManifest(jarFile)
            path = jarFile.replace(self.__jbossHome, 'JBOSSHOME')
            if manifest:
                manifest = manifest.strip()
            jar_info_list.append((path, checksum, manifest))
            found = True
        if found:
            jar_info_list.sort()
            self.addStringAsFile("\n".join([
                "%s\n%s\n%s\n" % (name, checksum, manifest)
                for (name, checksum, manifest) in jar_info_list]),
                'jarinfo.txt')
        else:
            self.addAlert("WARN: No jars found in JBoss system path (" + self.__jbossHome + ").")


    def query(self, request_obj):
        try:
            return self.query_java(request_obj)
        except Exception, e:
            self.addAlert("JBOSS API call failed, falling back to HTTP: %s" % e)
            return self.query_http(request_obj)

    def query_java(self, request_obj):
        from org.jboss.dmr import ModelNode
        controller_client = self.getOption('controller_client_proxy')
        if not controller_client:
            raise AttributeError("Controller Client is not available")

        request = ModelNode()
        request.get("operation").set(request_obj.operation)

        for key, val in request_obj.url_parts():
            request.get('address').add(key,val)

        if request_obj.parameters:
            for key, value in request_obj.parameters.iteritems():
                request.get(key).set(value)

        return controller_client.execute(request).toJSONString(True)

    def query_http(self, request_obj, postdata=None):
        host = self.getOption(('host', 'as7_host'))
        port = self.getOption(('port', 'as7_port'))

        username = self.getOption(('user', 'as7_user'), None)
        password = self.getOption(('pass', 'as7_pass'), None)

        uri = "http://%s:%s/management" % (host,port)

        json_data = {'operation': request_obj.operation,
                     'address': []}

        for key, val in request_obj.url_parts():
            json_data['address'].append({key:val})

        for key, val in request_obj.parameters.iteritems():
            json_data[key] = val

        postdata = json.dumps(json_data)
        headers = {'Content-Type': 'application/json',
                   'Accept': 'application/json'}

        opener = urllib2.build_opener()

        if username and password:
            passwd_manager = urllib2.HTTPPasswordMgrWithDefaultRealm()
            passwd_manager.add_password(realm="ManagementRealm",
                                        uri=uri,
                                        user=username,
                                        passwd=password)
            digest_auth_handler = urllib2.HTTPDigestAuthHandler(passwd_manager)
            basic_auth_handler = urllib2.HTTPBasicAuthHandler(passwd_manager)

            opener.add_handler(digest_auth_handler)
            opener.add_handler(basic_auth_handler)

        req = urllib2.Request(uri, data=postdata, headers=headers)

        try:
            resp = opener.open(req)
            return resp.read()
        except Exception, e:
            err_msg = "Could not query url: %s; error: %s" % (uri, e)
            self.addAlert(err_msg)
            return err_msg

    def _set_domain_info(self, parameters=None):
        """This function will add host controller and server instance
        name data if it is present to the desired resource. This is to support
        domain-mode operation in AS7"""
        host_controller_name = self.getOption("as7_host_controller_name")
        server_name = self.getOption("as7_server_name")

        if host_controller_name and server_name:
            if not parameters:
                parameters = {}

            parameters['host'] = host_controller_name
            parameters['server'] = server_name

        return parameters


    def _resource_to_file(self, resource=None, parameters=None, operation='read-resource', outfile=None):
        parameters = self._set_domain_info(parameters)

        r = Request(resource=resource,
                    parameters=parameters,
                    operation=operation)
        self.addStringAsFile(self.query(r), filename=outfile)


    def get_online_data(self):
        """
        This function co-locates calls to the management api that gather
        information from a running system.
        """
        self._resource_to_file(resource="/",
                parameters={"recursive": "true"},
                outfile="configuration.json")
        self._resource_to_file(resource="/core-service/service-container",
                operation="dump-services",
                outfile="dump-services.json")
        self._resource_to_file(resource="/subsystem/modcluster",
                operation="read-proxies-configuration",
                outfile="cluster-proxies-configuration.json")
        self._resource_to_file(resource="/core-service/platform-mbean/type/threading",
                operation="dump-all-threads",
                parameters={"locked-synchronizers": "true",
                            "locked-monitors": "true"},
                outfile="threaddump.json")

    def __getFiles(self, configDirAry):
        """
        This function will collect files from JBOSS_HOME for analysis.  The scope of files to
        be collected are determined by options to this SOS plug-in.
        """

        for dir_ in configDirAry:
            path = os.path.join(self.__jbossHome, dir_)
            ## First add forbidden files
            self.addForbiddenPath(os.path.join(path, "tmp"))
            self.addForbiddenPath(os.path.join(path, "work"))
            self.addForbiddenPath(os.path.join(path, "data"))

            if os.path.exists(path):
                ## First get everything in the conf dir
                confDir = os.path.join(path, "configuration")
                self.addForbiddenPath(os.path.join(confDir, 'mgmt-users.properties'))

                self.doCopyFileOrDir(confDir, sub=(self.__jbossHome, 'JBOSSHOME'))
                ## Log dir next
                logDir = os.path.join(path, "log")

                for logFile in find("*", logDir):
                    self.addCopySpecLimit(logFile,
                            self.getOption("logsize"),
                            sub=(self.__jbossHome, 'JBOSSHOME'))

    def setup(self):

        ## We need to know where JBoss is installed and if we can't find it we
        ## must exit immediately.
        if not self.__getJbossHome():
            self.exit_please()

        try:
            self.get_online_data()
        except urllib2.URLError:
            pass

        ## Generate hashes of the stock Jar files for the report.
        if self.getOption("stdjar"):
            self.__getStdJarInfo()

        ## Generate a Tree for JBOSS_HOME
        tree = DirTree(self.__jbossHome).as_string()
        self.addStringAsFile(tree, "jboss_home_tree.txt")

        self.__getFiles(self.__jbossServerConfigDirs)

    def postproc(self):
        """
        Obfuscate passwords.
        """

        password_xml_regex = re.compile(r'<password>.*</password>', re.IGNORECASE)

        for dir_ in self.__jbossServerConfigDirs:
            path = os.path.join(self.__jbossHome, dir_)

            self.doRegexSub(os.path.join(path,"configuration","*.xml"),
                            password_xml_regex,
                            r'<password>********</password>')

            tmp = os.path.join(path,"configuration")
            for propFile in find("*-users.properties", tmp):
                self.doRegexSub(propFile,
                                r"=(.*)",
                                r'=********')

#             Remove PW from -ds.xml files
            tmp = os.path.join(path, "deployments")
            for dsFile in find("*-ds.xml", tmp):
                self.doRegexSub(dsFile,
                                password_xml_regex,
                                r"<password>********</password>")
