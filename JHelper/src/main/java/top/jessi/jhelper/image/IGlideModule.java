package top.jessi.jhelper.image;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.caverock.androidsvg.SVG;

import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

@GlideModule
public final class IGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // 注册 OkHttp
        OkHttpClient client = getUnsafeOkHttpClient();
        if (client != null) {
            registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(client));
        }

        // 注册 SVG 支持
        registry.append(InputStream.class, SVG.class, new SvgDecoder());
        registry.register(SVG.class, PictureDrawable.class, new SvgDrawableTranscoder());
    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // 1. 信任所有证书
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // 2. SSLContext
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 3. SocketFactory
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // 4. OkHttp
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
