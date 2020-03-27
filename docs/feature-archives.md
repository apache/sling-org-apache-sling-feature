# Feature Archives

A feature file contains only references to artifacts like bundles. There are some use cases where it is useful to distribute a feature with all its artifacts, for example when distributing a feature to downstream users or when distributing a complete OSGi application. This can be done through feature archives.

A feature archive is an uncompressed zip file which contains one or more features together with the artifacts. The zip file uses the jar file format and has a manifest as the first entry in the archive. The manifest must contain these headers:

* 'Feature-Archive-Version' : The version of the format of the feature archive. Currently the value of this must be *1*.
* 'Feature-Archive-Contents' : The features contained in this archive. This is a comma separated list containing Maven ids or Maven urls.

All artifacts including the features are written to the archive in a maven repository like structure, that is the group id is converted to a path, the next path segment is the artifact id, followed by a path segment for the version. And the actual files are written into that directory with the typical maven convention. For example a feature with the id *org.apache.sling:org.apache.sling.core.feature:slingosgifeature:1.0.0* containing a bundle with the id *org.apache.sling:org.apache.sling.api:2.3.0* will result in the following archive:

```
/ META-INF / MANIFEST.MF
/ org / apache / sling / org.apache.sling.core.feature / 1.0.0 / org.apache.sling.core.feature-1.0.0.slingosgifeature
                       / org.apache.sling.api / 2.3.0 / org.apache.sling.api-2.3.0.jar
```
