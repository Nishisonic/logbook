# 1. CA秘密鍵を作成
openssl genrsa -out logbook-ca.key 2048

# 2. CA自己署名証明書を作成（100年有効）
openssl req -x509 -new -nodes -key logbook-ca.key -sha256 -days 36500 -out logbook-ca.crt -subj "/C=JP/ST=Tokyo/L=Tokyo/O=MyOrg/OU=MITM/CN=LogbookCA"

# 3. PKCS12を作成
openssl pkcs12 -export -in logbook-ca.crt -inkey logbook-ca.key -out logbook-keystore.p12 -name logbook -passout pass:logbook
