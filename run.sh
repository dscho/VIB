#!/bin/sh

curdir="$(cd "$(dirname "$0")"; pwd)"
test -h "$curdir"/plugins || ln -s . "$curdir"/plugins
java -jar "$curdir"/../ImageJ/ij.jar -ijpath "$curdir" "$@"
