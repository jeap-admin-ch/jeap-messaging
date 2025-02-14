####################
CA
####################
# Create CA private key
openssl genpkey -algorithm RSA -out jeap-ca-key.pem -pkeyopt rsa_keygen_bits:4096

# Create the CA Certificate, valid for 10 years
openssl req -x509 -new -key jeap-ca-key.pem -out jeap-ca-cert.pem -days 3650 -subj "/CN=jEAP CA"


####################
jme-messaging-receiverpublisher-outbox-service
####################
# Generate the Server/Client Private Key
openssl genpkey -algorithm RSA -out jmrpos.key -pkeyopt rsa_keygen_bits:2048

# Create a Certificate Signing Request (CSR)
openssl req -new -key jmrpos.key -out jmrpos.csr -subj "/CN=jme-messaging-receiverpublisher-outbox-service"

# Sign the CSR with the CA, valid for 5 years
openssl x509 -req -in jmrpos.csr -CA jeap-ca-cert.pem -CAkey jeap-ca-key.pem -CAcreateserial -out jmrpos.crt -days 1825

