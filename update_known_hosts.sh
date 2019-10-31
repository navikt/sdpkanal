#!/usr/bin/env bash

ssh-keyscan -p 2222 -H localhost > sftp/known_hosts -t rsa