The .pem files here have been copied from the MicroProfile JWT Quickstart.

Unlike X509 certificates these are just keys and so have no expiration date to recreation of 
the files should not be necessary.

The key pair was created with the following commands: -

openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -in private.pem -pubout -out public.pem
 