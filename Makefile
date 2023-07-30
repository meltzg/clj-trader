.PHONY: gen-cert
gen-cert:
	keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass $(PASSWORD) -keysize 2048