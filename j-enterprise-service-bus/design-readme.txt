MAPPINGS PHILOSOPHY
-------------------
Any source node can be mapped to a target node, even if the types are
incompatible. Actually the intent of the developer may be to generate a base
expression and modify it. 
The role of the UI is to automate common tasks,
provide simpler representations of data, and warn the developer early when
something is wrong. 
If a specific mapping is likely to be done, the it should
be done systematically. If there is a doubt, then the UI should propose the
common options. Note that if a function is complex, the function editor
should be used to compose it rather than the mappings editor. Thus there is
no need to allow to specify all the possible logics in the mappings editor.

DYNAMIC CLASSES UPDATE
----------------------
There are some classes that need to be regenerated each time some model values
change. The model value on which such class relies may reside in the same
containing object as the class. In that case it is easy to regenerate the
class each time the value changes since it is in the same object. But there
are some cases where the value is somewhere else in the global model. It is then
difficult to detect the value change and update the class. It is not an
option to systematically regenerate the class because each generated class
has its own identity, even if it has the same source code as its predecessor.
The actual issue is that the generated class may be requested multiply
during the runtime, for example to create an instance and compile a function 
that will use that instance. The generated class must then be exactly the 
same. The ideal solution would be to detect the dependency value changes
and update the class, but it seems to be impossible unless implementing
a heavy solution. In fact such a class is a weird calculated value that
must not be recalculated when the model is stable. This constraint does
not seem to be normal. But it is common in complex applications. A dependency 
management system should be used to keep model objects up to date at a 
reasonable cost. A change in a part of the model could be detected by
analyzing the serialized version of this model part. Actually the real
model is the serialized model, and model objects are just tools used to
exploit this model.

OPERATION SETTINGS DESIGN
-------------------------
An operation behavior can potentially be configured through some referenced
resource settings, some static settings (operation builder parameters) and
some dynamic settings (calculated by instance builders). All these parameter
values must be available when executing the operation but there is no 
obvious rule that indicates how these parameter values must be specified.
However we can infer that settings which shape some algorithms cannot be
calculated during the runtime. They must then be specified as resource
or operation static parameters. Common sense also dictates that common use 
cases must be easily applicable while uncommon use cases should remain 
applicable through advanced features. 
Settings that are shared by multiple operations in most use cases should be
specified as resource parameters. Other settings should be specified as
operation dynamic parameters unless they shape some algorithms, in which 
case they should be specified as operation static parameters.
But a question arises: can a shared setting be dynamic? In other words,
can we require to share an algorithm (not a simple parameter value)
between multiple operations? Yes, even if it would be very exceptional.
For instance a database password may be automatically changed every day
or hour, requiring to obtain it automatically when it expires. In such
a case the password specified as a shared resource parameter value would 
need to be changed dynamically. It would be possible if an advanced feature
allows to alter environment properties.
That being said, every setting that is required to execute an operation
could be calculated dynamically, except operation static parameter which
values are part of the algorithms. Note that if an operation static parameter
value need to be changed dynamically, then an alternate operation that use
a lower level protocol should be used (eg: HTTP instead of SOAP).  

STRATEGIC POSITIONING
---------------------
Enterprise Service Buses (ESBs) are fundamentally very useful because they 
multiply developer productivity. However, they require specific and rare 
expertise, leading to the creation of dedicated teams that ultimately become 
bottlenecks. It likely explains the dislike for this type of product. 
Actually, autonomy is preferred over productivity, as the former would shorten 
development delays even if it increases workloads. 
Thus how can we benefit from both autonomy and productivity? One solution would 
be to allow each team to leverage the transformation and connectivity 
capabilities of these tools. But for this to work, the required expertise would 
need to be minimal or even nonexistent, and the integration of these tools into 
projects would need to be facilitated. NOTE: It is therefore a good thing that 
JESB does not use a specific syntax (just Java) to express complex logics. 
The tool could, for example, be provided as a library usable in commonly 
mastered programming environments. This involves providing, through the simplest 
possible API, a way to edit and then invoke the tool's components (mappers, 
connectors, etc.). Note that bridge APIs could be provided for other languages. 
The editing interface should also be streamlined to require no special skills. 
It may be a good idea to display only a subset of the editing tools, a subset 
then tailored for each need (transformation only, connection only, etc.). 
It might be necessary to create sub-projects based on derivative tools that 
would only expose specific facets of the main tool. Current market trends 
should be taken into account in this context to avoid overly disruptive 
proposals, capitalizing on fads, etc.
This positioning is particularly interesting at present because JESB would 
not have been a serious competitor in the traditional ESB market due to its 
lack of essential features in this area (monitoring, versioning, security, etc.).

