package id.rhiquest.mozzic.Utils.NetworkUtils

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInterface {
    companion object {
        private var mRetrofit: Retrofit? = null
        fun getInstance(): Retrofit {
            var mHttpLoggingInterceptor = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)

            var mOkHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(mHttpLoggingInterceptor)
                .build()

            mRetrofit = Retrofit.Builder()
                .baseUrl(UrlConstant.BASE_URL)
                .client(mOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return mRetrofit!!
        }
    }
}