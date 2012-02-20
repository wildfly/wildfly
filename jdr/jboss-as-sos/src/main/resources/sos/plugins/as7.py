import os
import sys
import re
import zipfile
import urllib2
import tempfile
from xml.etree import ElementTree
from itertools import chain

from sos.plugins import Plugin, IndependentPlugin, AS7Mixin
from sos.utilities import DirTree, find, checksum

class AS7(Plugin, IndependentPlugin, AS7Mixin):
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

        self.__jbossHome = self.get_jboss_home()
        if not self.__jbossHome:
            self.addAlert("ERROR: The JBoss installation directory was not supplied.\
              The JBoss SOS plug-in cannot continue.")
            return False

        self.addAlert("INFO: The JBoss installation directory supplied to SOS is " +
                  self.__jbossHome)
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
        jar_info_list = []

        for jarFile in find("*.jar", self.__jbossHome):
            checksum = self.__getMd5(jarFile)
            manifest = self.__getManifest(jarFile)
            path = jarFile.replace(self.__jbossHome, 'JBOSSHOME')
            if manifest:
                manifest = manifest.strip()
            jar_info_list.append((path, checksum, manifest))

        if jar_info_list:
            jar_info_list.sort()
            self.addStringAsFile("\n".join([
                "%s\n%s\n%s\n" % (name, checksum, manifest)
                for (name, checksum, manifest) in jar_info_list]),
                'jarinfo.txt')
        else:
            self.addAlert("WARN: No jars found in JBoss system path (" + self.__jbossHome + ").")

    def get_online_data(self):
        """
        This function co-locates calls to the management api that gather
        information from a running system.
        """
        self.resource_to_file(resource="/",
                parameters={"recursive": "true"},
                outfile="configuration.json")
        self.resource_to_file(resource="/core-service/service-container",
                operation="dump-services",
                outfile="dump-services.json")
        self.resource_to_file(resource="/subsystem/modcluster",
                operation="read-proxies-configuration",
                outfile="cluster-proxies-configuration.json")
        self.resource_to_file(resource="/core-service/platform-mbean/type/threading",
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
                self.addForbiddenPath(os.path.join(confDir, 'application-users.properties'))

                for logFile in find("*.log", path):
                    self.addCopySpecLimit(logFile,
                            self.getOption("logsize"),
                            sub=(self.__jbossHome, 'JBOSSHOME'))

                for xml in find("*.xml", path):
                    self.addCopySpec(xml, sub=(self.__jbossHome, 'JBOSSHOME'))

                for prop in find("*.properties", path):
                    self.addCopySpec(prop, sub=(self.__jbossHome, 'JBOSSHOME'))

                deployment_info = self.__get_deployment_info(confDir)
                deployments = self.__get_deployments(path)
                for deployment in deployments:
                    self.__get_listing_from_deployment(deployment, deployment_info)

    def __get_deployment_info(self, dir_):
        """Gets the deployment name to sha1 mapping for all deployments defined
        in configs under dir_"""
        deployment_info = {}
        for config in find("*.xml", dir_):
            root = ElementTree.parse(config).getroot()
            # the namespace is harder to fetch than it should be
            ns = root.tag.rpartition("}")[0]
            ns += "}"
            for deployment in root.findall("./%sdeployments/%sdeployment" % (ns, ns)):
                name = deployment.attrib.get("name")
                sha1 = deployment.getchildren()[0].attrib.get("sha1")
                deployment_info[sha1] = name
        return deployment_info

    def __get_deployments(self, path):
        return list(chain(
            find("*", os.path.join(path, "deployments")),
            find("content", path)))

    def __get_listing_from_deployment(self, path, mapping):
        try:
            zf = zipfile.ZipFile(path)
            contents = []
            for zipinfo in zf.infolist():
                if zipinfo.filename.endswith("/"):
                    continue
                contents.append((zipinfo.filename, zipinfo.file_size))
            zf.close()
            contents.sort()
            output = "\n".join(["%s:%d" % (fn, fs) for fn, fs in contents])

            path_to = path.replace(self.__jbossHome, '')
            if 'content' in path:
                path_to = path_to.strip(os.path.sep).rstrip("content")
                path_to = os.path.join(*path_to.split(os.path.sep)[:-2])
                sha1 = "".join(path.split(os.path.sep)[-3:-1])
                name = mapping.get(sha1, sha1)
            else:
                path_to, name = os.path.split(path_to)

            self.addStringAsFile(output, os.path.join(path_to, "%s.txt" % name))
        except:
            # this is probably not a zipfile so we don't care
            pass


    def setup(self):

        if not self.__getJbossHome():
            self.exit_please()

        try:
            self.get_online_data()
        except urllib2.URLError:
            pass

        if self.getOption("stdjar"):
            self.__getStdJarInfo()

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

#           Remove PW from -ds.xml files
            tmp = os.path.join(path, "deployments")
            for dsFile in find("*-ds.xml", tmp):
                self.doRegexSub(dsFile,
                                password_xml_regex,
                                r"<password>********</password>")
