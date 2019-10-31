#!/usr/bin/env bash
docker run \
    --rm \
    -it \
    -v "$PWD/sftp:/home/sdp" \
    -v "$PWD/sftp/id_rsa.pub:/home/sdp/.ssh/keys/id_rsa.pub:ro" \
    -p 2222:22 \
    atmoz/sftp \
    sdp::1001