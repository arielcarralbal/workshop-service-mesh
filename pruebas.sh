#!/bin/bash

set -e

log() {
  echo
  echo "##### $*"
}

namespace=$PROJECT

if [ -z "$namespace" ]; then
    namespace="workshop"
    exit
fi

log $PROJECT