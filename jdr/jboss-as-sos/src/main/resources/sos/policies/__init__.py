import os
import platform
from sos.utilities import ImporterHelper, import_module

def import_policy(name):
    policy_fqname = "sos.policies.%s" % name
    try:
        return import_module(policy_fqname, Policy)
    except ImportError:
        return None

def load():
    helper = ImporterHelper(os.path.join('sos', 'policies'))
    for module in helper.get_modules():
        for policy in import_policy(module):
            if policy.check():
                return policy()
    raise Exception("No policy could be loaded.")


class PackageManager(object):

    def allPkgsByName(self, name):
        """
        Return a list of packages that match name.
        """
        return []

    def allPkgsByNameRegex(self, regex_name, flags=None):
        """
        Return a list of packages that match regex_name.
        """
        return []

    def pkgByName(self, name):
        """
        Return a single package that matches name.
        """
        return None

    def allPkgs(self):
        """
        Return a list of all packages.
        """
        return []


class Policy(object):

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
        return "unset"

    def validatePlugin(self, plugin_class):
        """
        Verifies that the plugin_class should execute under this policy
        """
        return False

    def preWork(self):
        """
        This function is called prior to collection.
        """
        pass

    def postWork(self):
        """
        This function is called after the sosreport has been generated.
        """
        pass

    def _parse_uname(self):
        (system, node, release,
         version, machine, processor) = platform.uname()
        self.hostname = node
        self.release = release
        self.smp = version.split()[1] == "SMP"
        self.machine = machine
