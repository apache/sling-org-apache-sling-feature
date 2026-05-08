# Feature Aggregation

When creating aggregates, extensions are merged into the resulting aggregate feature. There are
default rules for aggregating extension content, which essentially is appending
all the extension content of a given type. However custom merge plugins can also
be provided. After the merge a postprocessor is always run which can perform
additional operations based on the extension content. Note that both the
aggregate task of the `slingfeature-maven-plugin` as well as the launcher perform
merge operations on all the feature models these are provided with.

## Resolving artifact clashes

When two features contribute artifacts (typically bundles) with the same Maven
`groupId:artifactId` at different versions, the aggregator requires an explicit
override rule. Rules are added via `BuilderContext.addArtifactsOverride(...)`
and use a fake `ArtifactId` whose version field carries the rule:

| Rule       | Outcome                                                              |
|------------|----------------------------------------------------------------------|
| `HIGHEST`  | Pick the artifact with the highest version (OSGi version order)      |
| `LATEST`   | Pick the last contributed artifact                                   |
| `FIRST`    | Pick the first contributed artifact                                  |
| `ALL`      | Keep all candidates                                                  |
| `<x.y.z>`  | Pick the artifact whose version equals the literal value             |

The `groupId` and `artifactId` of the override are matched against the clashing
artifacts; `*` matches anything. A typical wildcard rule is `*:*:HIGHEST`.

## Resolving Bundle-SymbolicName collisions

The Maven-coordinate clash mechanism above is blind to OSGi identity. Two
bundles can have different `groupId:artifactId` but the same
`Bundle-SymbolicName` — typically when one source synthesises Maven coordinates
from the OSGi manifest because the JAR has no embedded Maven metadata
(`META-INF/maven/.../pom.properties`). The aggregator lets both through; the
OSGi runtime then refuses to install duplicates with `Bundle symbolic name and
version are not unique`.

Set `BuilderContext.setOsgiBsnCollisionDetection(true)` to detect this. After
the standard GAV merge, every assembled bundle's manifest is read via the
configured `ArtifactProvider`, bundles are grouped by BSN (parameters such as
`;singleton:=true` are stripped), and any group of two or more is resolved
through the same wildcard `*:*:<rule>` overrides described above. If detection
is enabled and a colliding group has no matching override, assembly fails —
symmetric with the existing `Artifact override rule required` error for
unresolved Maven-GAV clashes.

`ArtifactProvider` must be configured for detection to do anything. Bundles
whose manifest cannot be read are passed through untouched.
