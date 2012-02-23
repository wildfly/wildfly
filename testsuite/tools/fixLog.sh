

##  Fixes the letter-per-line bug, see https://issues.jboss.org/browse/ARQ-775 .
##
## Usage:
##     cat .../FooTestCase.txt | fixLog.sh
##   or
##     fixLog.sh .../FooTestCase.txt

sed ':a;N;$!ba;s/\(.\)\n/\1/g' $1

