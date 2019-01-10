keytool -genkey -noprompt \
 -alias alias1 \
 -dname "CN=sdpkanal-nais, OU=Unknown, O=NAV, L=Oslo, S=Oslo, C=NO" \
 -keystore keystore.p12 \
 -storepass changeit \
 -keypass changeit
keytool -list -keystore keystore.p12 -storepass changeit -storetype pkcs12
