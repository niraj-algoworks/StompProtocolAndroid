package ua.naiksoftware.stompclientexample;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestClient {

    private static RestClient instance;
    private static final Object lock = new Object();

    public static RestClient getInstance() {
        RestClient instance = RestClient.instance;
        if (instance == null) {
            synchronized (lock) {
                instance = RestClient.instance;
                if (instance == null) {
                    RestClient.instance = instance = new RestClient();
                }
            }
        }
        return instance;
    }

    private final IApiService mIApiService;

    private RestClient() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://dev.api.merlyn.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        mIApiService = retrofit.create(IApiService.class);
    }

    public IApiService getIApiService() {
        return mIApiService;
    }
}
