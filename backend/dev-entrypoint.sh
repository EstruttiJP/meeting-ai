#!/bin/sh
# Dev-only entrypoint: runs the app and recompiles on save so DevTools can
# pick up the change and restart. `mvn spring-boot:run` alone does NOT
# recompile .java files on its own — Spring Boot DevTools only reacts to
# changes under target/classes, so something has to keep running `mvn
# compile` in the background. `entr` does that; `-d` makes it exit when a
# file is added/removed so the outer loop can refresh the watch list to
# include new files.
set -e

mvn -B spring-boot:run &

while true; do
	find src -name '*.java' | entr -dn mvn -q -o compile
done
