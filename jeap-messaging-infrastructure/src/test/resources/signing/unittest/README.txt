# NOTE all key are generated with a dummy password, for testing this is ok
###################
# Create the Root CA

# Generate the Root CA Private Key
openssl genpkey -algorithm RSA -out rootCA.key -aes256 -pass pass:strongpassword -pkeyopt rsa_keygen_bits:4096

# Create the Root CA Certificate
openssl req -x509 -new -key rootCA.key -sha256 -days 7300 -out rootCA.crt -subj "/C=CH/ST=Bern/L=Bern/O=TestRootCA/OU=IT/CN=RootCA" -passin pass:strongpassword

###################
# Create the Intermediate CA

# Generate the Intermediate CA Private Key
openssl genpkey -algorithm RSA -out intermediateCA.key -aes256 -pass pass:strongpassword -pkeyopt rsa_keygen_bits:4096

# Create the Intermediate CA Certificate Signing Request (CSR)
openssl req -new -key intermediateCA.key -out intermediateCA.csr -subj "/C=CH/ST=Bern/L=Bern/O=TestIntermediateCA/OU=BIT/CN=IntermediateCA" -passin pass:strongpassword

# Sign the Intermediate CA Certificate with the Root CA
openssl x509 -req -in intermediateCA.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out intermediateCA.crt -days 5000 -sha256 -passin pass:strongpassword -extfile <(echo "basicConstraints=critical,CA:TRUE")

###################
# Create the Leaf (End-Entity) Certificate

# Generate the Leaf Private Key
openssl genpkey -algorithm RSA -out jme-example-service.key -pkeyopt rsa_keygen_bits:2048

# Create the Leaf Certificate Signing Request (CSR)
openssl req -new -key jme-example-service.key -out jme-example-service.csr -subj "/C=CH/ST=Bern/L=Bern/O=BIT/OU=jeap/CN=jme-example-service"

# Sign the Leaf Certificate with the Intermediate CA
openssl x509 -req -in jme-example-service.csr -CA intermediateCA.crt -CAkey intermediateCA.key -CAcreateserial -out jme-example-service.crt -days 3000 -sha256 -passin pass:strongpassword

###################
# Create another Leaf (End-Entity) Certificate for jme-messaging-receiverpublisher-service

# Generate the Leaf Private Key
openssl genpkey -algorithm RSA -out jme-messaging-receiverpublisher-service.key -pkeyopt rsa_keygen_bits:2048

# Create the Leaf Certificate Signing Request (CSR)
openssl req -new -key jme-messaging-receiverpublisher-service.key -out jme-messaging-receiverpublisher-service.csr -subj "/C=CH/ST=Bern/L=Bern/O=BIT/OU=jeap/CN=jme-messaging-receiverpublisher-service"

# Sign the Leaf Certificate with the Intermediate CA
openssl x509 -req -in jme-messaging-receiverpublisher-service.csr -CA intermediateCA.crt -CAkey intermediateCA.key -CAcreateserial -out jme-messaging-receiverpublisher-service.crt -days 3000 -sha256 -passin pass:strongpassword

##################
# Create another Leaf (End-Entity) Certificate for jme-messaging-receiverpublisher-service

# Generate the Leaf Private Key
openssl genpkey -algorithm RSA -out jme-messaging-receiverpublisher-service2.key -pkeyopt rsa_keygen_bits:2048

# Create the Leaf Certificate Signing Request (CSR)
openssl req -new -key jme-messaging-receiverpublisher-service2.key -out jme-messaging-receiverpublisher-service2.csr -subj "/C=CH/ST=Bern/L=Bern/O=BIT/OU=jeap/CN=jme-messaging-receiverpublisher-service"

# Sign the Leaf Certificate with the Intermediate CA
openssl x509 -req -in jme-messaging-receiverpublisher-service2.csr -CA intermediateCA.crt -CAkey intermediateCA.key -CAcreateserial -out jme-messaging-receiverpublisher-service2.crt -days 3000 -sha256 -passin pass:strongpassword

##################
# Create another Leaf (End-Entity) Certificate for jme-test-expired-service

# Generate the Leaf Private Key
openssl genpkey -algorithm RSA -out jme-test-expired-service.key -pkeyopt rsa_keygen_bits:2048

# Create the Leaf Certificate Signing Request (CSR)
openssl req -new -key jme-test-expired-service.key -out jme-test-expired-service.csr -subj "/C=CH/ST=Bern/L=Bern/O=BIT/OU=jeap/CN=jme-test-expired-service"

# Sign the Leaf Certificate with the Intermediate CA
openssl x509 -req -in jme-test-expired-service.csr -CA intermediateCA.crt -CAkey intermediateCA.key -CAcreateserial -out jme-test-expired-service.crt -days -1 -sha256 -passin pass:strongpassword
