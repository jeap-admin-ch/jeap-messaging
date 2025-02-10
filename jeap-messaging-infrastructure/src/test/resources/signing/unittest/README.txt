####################
CA
####################
# Create CA private key
openssl genpkey -algorithm RSA -out jeap-ca-key.pem -pkeyopt rsa_keygen_bits:4096

# Create the CA Certificate, valid for 10 years
openssl req -x509 -new -key jeap-ca-key.pem -out jeap-ca-cert.pem -days 3650 -subj "/CN=jEAP CA"

####################
test-jeap-service
####################
# Generate the Server/Client Private Key
openssl genpkey -algorithm RSA -out test.key -pkeyopt rsa_keygen_bits:2048

# Create a Certificate Signing Request (CSR)
openssl req -new -key test.key -out test.csr -subj "/CN=test-jeap-service"

# Sign the CSR with the CA, valid for 5 years
openssl x509 -req -in test.csr -CA jeap-ca-cert.pem -CAkey jeap-ca-key.pem -CAcreateserial -out test.crt -days 1825

####################
jme-messaging-receiverpublisher-service
####################
# Generate the Server/Client Private Key
openssl genpkey -algorithm RSA -out jmrps.key -pkeyopt rsa_keygen_bits:2048

# Create a Certificate Signing Request (CSR)
openssl req -new -key jmrps.key -out jmrps.csr -subj "/CN=jme-messaging-receiverpublisher-service"

# Sign the CSR with the CA, valid for 5 years
openssl x509 -req -in jmrps.csr -CA jeap-ca-cert.pem -CAkey jeap-ca-key.pem -CAcreateserial -out jmrps.crt -days 1825

