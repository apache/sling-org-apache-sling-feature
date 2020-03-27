# Feature Reference Files

Sometimes it is necessary to pass around a list of features. For this purpose feature *ref*erence files can be used. These files are simple text files where each non empty line which is not a comment, is treated as Maven coordinates. The coordinates can be specified as a Maven id or a Maven url.

Ref files should use the extension *.ref*.

```
# This is a comment and below are three features
org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0
org.apache.sling:org.apache.sling.core.servlets:slingosgifeature:1.3.0
org.apache.sling:org.apache.sling.core.scripting:slingosgifeature:1.1.0
```
