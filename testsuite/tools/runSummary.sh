##  Cat the file or stdin if no args,
##  filter only interesting lines - plugin executions and modules separators,
##  plus Test runs summaries,
##  and remove the boring plugins like enforcer etc.
 
cat $1 \
 | egrep ' --- |Building| ---------|Tests run: | T E S T S' \
 | grep -v 'Time elapsed' \
 | sed 's|Tests run:|                Tests run:|' \
 | grep -v maven-clean-plugin \
 | grep -v maven-enforcer-plugin \
 | grep -v buildnumber-maven-plugin \
 | grep -v maven-help-plugin \
 | grep -v properties-maven-plugin:.*:write-project-properties \
;
