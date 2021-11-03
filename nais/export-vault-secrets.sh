##!/usr/bin/env bash
echo "Sjekker s3 brukernavn"
if test -f /var/run/secrets/nais.io/appcredentials/s3_accesskey;
then
  echo "Setter s3 brukernavn"
    export eessi_pensjon_retry_s3_creds_username=$(cat /var/run/secrets/nais.io/appcredentials/s3_accesskey)
fi

echo "Sjekker s3 passsword "
if test -f /var/run/secrets/nais.io/appcredentials/s3_secretkey;
then
  echo "Setter s3 password"
    export eessi_pensjon_retry_s3_creds_password=$(cat /var/run/secrets/nais.io/appcredentials/s3_secretkey)
fi
