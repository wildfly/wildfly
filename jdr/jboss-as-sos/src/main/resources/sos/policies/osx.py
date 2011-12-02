from sos.policies import PackageManager, Policy
from sos.utilities import shell_out

class OSXPolicy(Policy):

    distro = "Mac OS X"

    @classmethod
    def check(class_):
        try:
            return "Mac OS X" in shell_out("sw_vers")
        except Exception, e:
            return False
