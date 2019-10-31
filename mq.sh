#!/usr/bin/env bash


eval docker run --rm -it --env-file mq_env.list -p 1414:1414 -p 9443:9443 ibmcom/mq:9.1.2.0