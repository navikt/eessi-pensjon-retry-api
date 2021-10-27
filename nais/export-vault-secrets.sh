##!/usr/bin/env bash
echo "Sjekker eessi_pensjon_retry-api_s3_creds_password"
if test -f /var/run/secrets/nais.io/appcredentials/eessi_pensjon_retry-api_s3_creds_password;
then
  echo "Setter eessi_pensjon_retry-api_s3_creds_password"
    export eessi_pensjon_retry_api_s3_creds_password=$(cat /var/run/secrets/nais.io/appcredentials/eessi_pensjon_retry-api_s3_creds_password)
fi