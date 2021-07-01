package app.editors.manager.di.module

import app.editors.manager.onedrive.OneDriveService
import app.editors.manager.managers.providers.IOneDriveServiceProvider
import app.editors.manager.managers.retrofit.BaseInterceptor
import app.editors.manager.onedrive.OneDriveServiceProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import lib.toolkit.base.managers.http.NetworkClient
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class OneDriveScope

@Module
class OneDriveModule(val token: String) {

    @Provides
    @OneDriveScope
    fun provideOneDrive(oneDriveService: OneDriveService): IOneDriveServiceProvider = OneDriveServiceProvider(oneDriveService)


    @Provides
    @OneDriveScope
    fun provideOneDriveService(okHttpClient: OkHttpClient): OneDriveService = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl("https://graph.microsoft.com/")
        .addConverterFactory(Json {
            isLenient = true
            ignoreUnknownKeys = true
        }.asConverterFactory(MediaType.get("application/json;odata.metadata=minimal;odata.streaming=true;IEEE754Compatible=false;charset=utf-8")))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
        .create(OneDriveService::class.java)

    @Provides
    @OneDriveScope
    fun provideOkHttpClient(): OkHttpClient {
        val builder = NetworkClient.getOkHttpBuilder(true, false)
        builder.protocols(listOf(Protocol.HTTP_1_1))
            .readTimeout(NetworkClient.ClientSettings.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NetworkClient.ClientSettings.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(NetworkClient.ClientSettings.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(BaseInterceptor(token))
        return builder.build()
    }

}