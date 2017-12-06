
# Rule Engine written using Play, Slick and Scala.JS

This web application allows to create, edit and execute sets of business rules and expose them using web-services.
Python was chosen as rule language. Web interface was written using CoffeeScript and JQuery, but later CoffeeScript was replaced with Scala.JS. 
For each set of business rules will be creates web service automatically, according to input and data formats specified for each rule set.

## Editor of business rules

This screenshot displays business rule editor in view mode.

[image]

Rule set properties are listed, on the top, for example, name of business rule set and version.
Then you can see code of rules, where each rule has optional name, it makes debugging easier,
 required condition which specifies should body of the rule executed or not and rule body which generates result.
Each rule set has some links it's WSDL service description with it's own input and output type XSD schemas.

####  Automatically generated WSDL this rule set:

[image]

rulesRequest contains definition of rule set parameters. In current rule set, "argument1" has type "String".
rulesResponse contains list of type. for example type "rule", which define result of each rule execution, has property "result1" with type "String", rule attributes and condition execution result.
rulesFault describe format of an error.

#### Rule set editor in editing mode

To change rules, new version of rule set should be created, where rule engine allow to edit code of rules and rule set properties
During editing user can save changes on server, which are visible only to him. 
Although uncommitted rule sets can be executed using version unique id, they can be removed and are not the latest version of the rules.
If rule sets are executed using rule set identifier (not version identifier), which can have multiple versions, last committed version will be executed. 

Since rule sets can have multiple versions, they can be chosen using next filters. For example:
https://localhost/interface/rules/id:7/soap?wsdl - Definition of rule set version 7 service.
https://localhost/interface/rules/seq:CMe3/soap?wsdl – Definition of latest committed rule set "CMe3"
https://localhost/interface/rules/seq:CMe3;api:4a75056b2f7396d1aa2b49343d6281a5/soap?wsdl – Definition of last version
 of rule set "CMe3", which interface corresponds to 4a75056b2f7396d1aa2b49343d6281a5.
 Interface identifier was calculated from input and output types definition.

### Repository

For versioning rules there is a Repository, where all available rule sets versions can be seen, and also history of their changes.
 Previously committed rule sets can't be removed.

##### Rule Set history

Rule Set history can be seen on the next screenshot:

[image]

### Data types

In addition to the rules, you can define complex data types that represent structures with fields
primitive types or lists. The editor of one complex type is shown below in the mode of viewing and editing.

##### View mode

[image]

##### Editing mode

[image]

##### Data types can be selected in the dialog box:

### Arguments of Rule Sets.

Arguments are available like regular variables. For example, on the next picture in the python expression
 an argument has type list[list[list[int]]] and defines type of nested lists.
 Result type, is defined with 2 fields "field1" and "field2" which is defined as Dictionary.
 Rule Set arguments are converted into python code and available in rule set conditions and bodies as python variables. 

##### On the next screenshot there is SOAP-UI and example of Rule Set execution.

[image]

### Load Monitoring

Rule Set Engine can display chars of CPU usage by all processes and current JVM.
 On the right is a graph of used and available memory.
 To create chars HighCharts (www.HighCharts.com) library was used. Information is provided through web-sockets.
 Actor on the server side stores some latest samples to show chart values for last minute when page is opened.
 

