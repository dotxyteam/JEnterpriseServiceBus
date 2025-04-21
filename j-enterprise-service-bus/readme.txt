ABOUT MAPPINGS
--------------
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
There are some classes that need to regenerated each time some model values
change. The model value on which such class relies may reside in the same
containing object as the class. In that case it is easy to regenerate the
class each time the value changes since it is in the same object. But there
are some cases where the value is somewhere else in the model. It is then
difficult to detect the value change and update the class. It is not an
option to systematically regenerate the class because each generated class
has its own identity, even if it has the same source code as its predecessor.
The actual issue is that the generated class may be requested multiply
during the runtime, for example to create an instance and compile a function 
that will use that instance. The generated class must then be exactly the 
same. The ideal solution would be to detect the dependency value changes
and update the class, but it seems to be impossible unless implementing
a heavy solution. In fact the class is a weird calculated value that
must not be recalculated when the model is stable. This constraint does
not seem to be normal. 