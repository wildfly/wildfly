from __future__ import with_statement

import os
import re
import platform
import time
import fnmatch

from sos.utilities import ImporterHelper, import_module, get_hash_name
from sos.plugins import IndependentPlugin
from sos import _sos as _
import hashlib

def import_policy(name):
    policy_fqname = "sos.policies.%s" % name
    try:
        return import_module(policy_fqname, Policy)
    except ImportError:
        return None

def load(cache={}):
    if 'policy' in cache:
        return cache.get('policy')

    import sos.policies
    helper = ImporterHelper(sos.policies)
    for module in helper.get_modules():
        for policy in import_policy(module):
            if policy.check():
                cache['policy'] = policy()

    if 'policy' not in cache:
        cache['policy'] = GenericPolicy()

    return cache['policy']


class PackageManager(object):
    """Encapsulates a package manager. If you provide a query_command to the
    constructor it should print each package on the system in the following
    format:
        package name|package.version\n

    You may also subclass this class and provide a getPackageList method to
    build the list of packages and versions.
    """

    query_command = None

    def __init__(self, query_command=None):
        self.packages = {}
        if query_command:
            self.query_command = query_command

    def allPkgsByName(self, name):
        """
        Return a list of packages that match name.
        """
        return fnmatch.filter(self.allPkgs().keys(), name)

    def allPkgsByNameRegex(self, regex_name, flags=0):
        """
        Return a list of packages that match regex_name.
        """
        reg = re.compile(regex_name, flags)
        return [pkg for pkg in self.allPkgs().keys() if reg.match(pkg)]

    def pkgByName(self, name):
        """
        Return a single package that matches name.
        """
        try:
            self.AllPkgsByName(name)[-1]
        except Exception:
            return None

    def getPackageList(self):
        """
        returns a dictionary of packages in the following format:
        {'package_name': {'name': 'package_name', 'version': 'major.minor.version'}}
        """
        if self.query_command:
            pkg_list = shell_out(self.query_command).splitlines()
            for pkg in pkg_list:
                name, version = pkg.split("|")
                self.packages[name] = {
                    'name': name,
                    'version': version.split(".")
                }

        return self.packages

    def allPkgs(self):
        """
        Return a list of all packages.
        """
        if not self.packages:
            self.packages = self.getPackageList()
        return self.packages

    def pkgNVRA(self, pkg):
        fields = pkg.split("-")
        version, release, arch = fields[-3:]
        name = "-".join(fields[:-3])
        return (name, version, release, arch)


class Policy(object):

    msg = _("""This utility will collect some detailed  information about the
hardware and setup of your %(distro)s system.
The information is collected and an archive is  packaged under
/tmp, which you can send to a support representative.
%(distro)s will use this information for diagnostic purposes ONLY
and it will be considered confidential information.

This process may take a while to complete.
No changes will be made to your system.

""")

    distro = ""

    def __init__(self):
        """Subclasses that choose to override this initializer should call
        super() to ensure that they get the required platform bits attached.
        super(SubClass, self).__init__()"""
        self._parse_uname()
        self.reportName = self.hostname
        self.ticketNumber = None
        self.package_manager = PackageManager()
        self._valid_subclasses = []

    def get_valid_subclasses(self):
        return [IndependentPlugin] + self._valid_subclasses

    def set_valid_subclasses(self, subclasses):
        self._valid_subclasses = subclasses

    def del_valid_subclasses(self):
        del self._valid_subclasses

    valid_subclasses = property(get_valid_subclasses,
            set_valid_subclasses,
            del_valid_subclasses,
            "list of subclasses that this policy can process")

    def check(self):
        """
        This function is responsible for determining if the underlying system
        is supported by this policy.
        """
        return False

    def preferedArchive(self):
        """
        Return the class object of the prefered archive format for this platform
        """
        from sos.utilities import TarFileArchive
        return TarFileArchive

    def getArchiveName(self):
        """
        This function should return the filename of the archive without the
        extension.
        """
        if self.ticketNumber:
            self.reportName += "." + self.ticketNumber
        return "sosreport-%s-%s" % (self.reportName, time.strftime("%Y%m%d%H%M%S"))

    def validatePlugin(self, plugin_class):
        """
        Verifies that the plugin_class should execute under this policy
        """
        valid_subclasses = [IndependentPlugin] + self.valid_subclasses
        return any(issubclass(plugin_class, class_) for class_ in valid_subclasses)

    def preWork(self):
        """
        This function is called prior to collection.
        """
        pass

    def packageResults(self, package_name):
        """
        This function is called prior to packaging.
        """
        pass

    def postWork(self):
        """
        This function is called after the sosreport has been generated.
        """
        pass

    def pkgByName(self, pkg):
        return self.package_manager.pkgByName(pkg)

    def _parse_uname(self):
        (system, node, release,
         version, machine, processor) = platform.uname()
        self.system = system
        self.hostname = node
        self.release = release
        self.smp = version.split()[1] == "SMP"
        self.machine = machine

    def setCommons(self, commons):
        self.commons = commons

    def is_root(self):
        """This method should return true if the user calling the script is
        considered to be a superuser"""
        return (os.getuid() == 0)

    def _create_checksum(self, final_filename=None):
        if not final_filename:
            return False

        archive_fp = open(final_filename, 'rb')
        digest = hashlib.new(get_hash_name())
        digest.update(archive_fp.read())
        archive_fp.close()
        return digest.hexdigest()

    def getPreferredHashAlgorithm(self):
        """Returns the string name of the hashlib-supported checksum algorithm
        to use"""
        return "md5"

    def displayResults(self, final_filename=None):

        # make sure a report exists
        if not final_filename:
           return False

        # store checksum into file
        fp = open(final_filename + "." + get_hash_name(), "w")
        checksum = self._create_checksum(final_filename)
        if checksum:
            fp.write(checksum + "\n")
        fp.close()

        self._print()
        self._print(_("Your sosreport has been generated and saved in:\n  %s") % final_filename)
        self._print()
        if checksum:
            self._print(_("The checksum is: ") + checksum)
            self._print()
        self._print(_("Please send this file to your support representative."))
        self._print()

    def uploadResults(self, final_filename):

        # make sure a report exists
        if not final_filename:
            return False

        self._print()
        # make sure it's readable
        try:
            fp = open(final_filename, "r")
        except:
            return False

        # read ftp URL from configuration
        if self.commons['cmdlineopts'].upload:
            upload_url = self.commons['cmdlineopts'].upload
        else:
            try:
               upload_url = self.commons['config'].get("general", "ftp_upload_url")
            except:
               self._print(_("No URL defined in config file."))
               return

        from urlparse import urlparse
        url = urlparse(upload_url)

        if url[0] != "ftp":
            self._print(_("Cannot upload to specified URL."))
            return

        # extract username and password from URL, if present
        if url[1].find("@") > 0:
            username, host = url[1].split("@", 1)
            if username.find(":") > 0:
                username, passwd = username.split(":", 1)
            else:
                passwd = None
        else:
            username, passwd, host = None, None, url[1]

        # extract port, if present
        if host.find(":") > 0:
            host, port = host.split(":", 1)
            port = int(port)
        else:
            port = 21

        path = url[2]

        try:
            from ftplib import FTP
            upload_name = os.path.basename(final_filename)

            ftp = FTP()
            ftp.connect(host, port)
            if username and passwd:
                ftp.login(username, passwd)
            else:
                ftp.login()
            ftp.cwd(path)
            ftp.set_pasv(True)
            ftp.storbinary('STOR %s' % upload_name, fp)
            ftp.quit()
        except Exception, e:
            self._print(_("There was a problem uploading your report to Red Hat support. " + str(e)))
        else:
            self._print(_("Your report was successfully uploaded to %s with name:" % (upload_url,)))
            self._print("  " + upload_name)
            self._print()
            self._print(_("Please communicate this name to your support representative."))
            self._print()

        fp.close()

    def _print(self, msg=None):
        """A wrapper around print that only prints if we are not running in
        silent mode"""
        if not self.commons['cmdlineopts'].silent:
            if msg:
                print msg
            else:
                print


    def get_msg(self):
        """This method is used to prepare the preamble text to display to
        the user in non-batch mode. If your policy sets self.distro that
        text will be substituted accordingly. You can also override this
        method to do something more complicated."""
        return self.msg % {'distro': self.distro}


class GenericPolicy(Policy):
    """This Policy will be returned if no other policy can be loaded. This
    should allow for IndependentPlugins to be executed on any system"""

    def get_msg(self):
        return self.msg % {'distro': self.system}


class LinuxPolicy(Policy):
    """This policy is meant to be an abc class that provides common implementations used
       in Linux distros"""

    def __init__(self):
        super(LinuxPolicy, self).__init__()

    def getPreferredHashAlgorithm(self):
        checksum = "md5"
        try:
            fp = open("/proc/sys/crypto/fips_enabled", "r")
        except:
            return checksum

        fips_enabled = fp.read()
        if fips_enabled.find("1") >= 0:
            checksum = "sha256"
        fp.close()
        return checksum

    def runlevelDefault(self):
        try:
            with open("/etc/inittab") as fp:
                pattern = r"id:(\d{1}):initdefault:"
                text = fp.read()
                return int(re.findall(pattern, text)[0])
        except:
            return 3

    def kernelVersion(self):
        return self.release

    def hostName(self):
        return self.hostname

    def isKernelSMP(self):
        return self.smp

    def getArch(self):
        return self.machine

    def getLocalName(self):
        """Returns the name usd in the preWork step"""
        return self.hostName()

    def preWork(self):
        # this method will be called before the gathering begins

        localname = self.getLocalName()

        if not self.commons['cmdlineopts'].batch and not self.commons['cmdlineopts'].silent:
            try:
                self.reportName = raw_input(_("Please enter your first initial and last name [%s]: ") % localname)
                self.reportName = re.sub(r"[^a-zA-Z.0-9]", "", self.reportName)

                self.ticketNumber = raw_input(_("Please enter the case number that you are generating this report for: "))
                self.ticketNumber = re.sub(r"[^0-9]", "", self.ticketNumber)
                self._print()
            except:
                self._print()
                sys.exit(0)

        if len(self.reportName) == 0:
            self.reportName = localname

        if self.commons['cmdlineopts'].customerName:
            self.reportName = self.commons['cmdlineopts'].customerName
            self.reportName = re.sub(r"[^a-zA-Z.0-9]", "", self.reportName)

        if self.commons['cmdlineopts'].ticketNumber:
            self.ticketNumber = self.commons['cmdlineopts'].ticketNumber
            self.ticketNumber = re.sub(r"[^0-9]", "", self.ticketNumber)

        return

    def packageResults(self, archive_filename):
        self._print(_("Creating compressed archive..."))
