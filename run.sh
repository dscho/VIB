#!/bin/sh

curdir="$(cd "$(dirname "$0")"; pwd)"
java -Dplugins.dir="$curdir" -jar "$curdir"/../ImageJ/ij.jar "$@"
