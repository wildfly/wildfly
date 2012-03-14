from sos.plugins import DebianPlugin
from sos.policies import PackageManager, LinuxPolicy

import os

class DebianPolicy(LinuxPolicy):
    def __init__(self):
        super(DebianPolicy, self).__init__()
        self.reportName = ""
        self.ticketNumber = ""
        self.package_manager = PackageManager("dpkg-query -W -f='${Package}|${Version}\\n' \*")
        self.valid_subclasses = [DebianPlugin]
        self.distro = "Debian"

    @classmethod
    def check(self):
        """This method checks to see if we are running on Debian.
           It returns True or False."""
        return os.path.isfile('/etc/debian_version')

    def debianVersion(self):
        try:
            fp = open("/etc/debian_version").read()
            if "wheezy/sid" in fp:
                return 6
            fp.close()
        except:
            pass
        return False
