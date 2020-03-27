# Feature Aggregation

When creating aggregates, extensions are merged into the resulting aggregate feature. There are
default rules for aggregating extension content, which essentially is appending
all the extension content of a given type. However custom merge plugins can also
be provided. After the merge a postprocessor is always run which can perform
additional operations based on the extension content. Note that both the
aggregate task of the `slingfeature-maven-plugin` as well as the launcher perform
merge operations on all the feature models these are provided with.

TBD
