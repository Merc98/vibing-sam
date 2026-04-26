package com.example.ide.puente.frida

object FridaTemplates {

    val SSL_PINNING_BYPASS = """
        /**
         * Universal SSL pinning bypass. Runs as frida -l script.js -U -n <pkg>.
         */
        Java.perform(function () {
          var X509TrustManager = Java.use('javax.net.ssl.X509TrustManager');
          var SSLContext = Java.use('javax.net.ssl.SSLContext');

          var TrustAll = Java.registerClass({
            name: 'com.puente.TrustAll',
            implements: [X509TrustManager],
            methods: {
              checkClientTrusted: function (chain, authType) {},
              checkServerTrusted: function (chain, authType) {},
              getAcceptedIssuers: function () { return []; }
            }
          });

          SSLContext.init.overload(
            '[Ljavax.net.ssl.KeyManager;',
            '[Ljavax.net.ssl.TrustManager;',
            'java.security.SecureRandom'
          ).implementation = function (km, tm, sr) {
            console.log('[Puente] SSLContext.init intercepted');
            this.init(km, [TrustAll.${"$"}new()], sr);
          };
        });
    """.trimIndent()

    val ROOT_DETECTION_BYPASS = """
        Java.perform(function () {
          var File = Java.use('java.io.File');
          File.exists.implementation = function () {
            var p = this.getAbsolutePath();
            if (/(su|magisk|busybox|xposed)/i.test(p)) {
              console.log('[Puente] root-check mask: ' + p);
              return false;
            }
            return this.exists();
          };
        });
    """.trimIndent()

    val METHOD_TRACER = """
        /**
         * Replace FULLY.QUALIFIED.Class and methodName below.
         */
        Java.perform(function () {
          var Target = Java.use('com.example.Target');
          Target.methodName.implementation = function () {
            console.log('[Puente] methodName args=' + Array.prototype.slice.call(arguments));
            var rv = this.methodName.apply(this, arguments);
            console.log('[Puente] methodName return=' + rv);
            return rv;
          };
        });
    """.trimIndent()
}
