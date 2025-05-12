[API Credentials]
https://console.cloud.google.com/apis/credentials?project=ztechno

[See SHA1 for keystore]
keytool -list -v -keystore "C:\Users\Desktop\Desktop\github\AppLogClient\app\zdebug.keystore" -alias zdebug -storepass pass123pass -keypass pass123pass




[OTHER CMDS]
keytool -list -v -alias androiddebugkey -keystore "%USERPROFILE%\.android\debug.keystore" -storepass pass123pass -keypass pass123pass
keytool -genkey -v -keystore zdebug.keystore -alias zdebug -keyalg RSA -keysize 2048 -validity 10000
keytool -list -v -keystore "C:\Users\Desktop\Desktop\github\AppLogClient\app\zdebug.keystore" -alias zdebug -storepass pass123pass -keypass pass123pass