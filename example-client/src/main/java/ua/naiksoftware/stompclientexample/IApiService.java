package ua.naiksoftware.stompclientexample;

import io.reactivex.Completable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IApiService {

    @GET("/ws")
    Completable sendRestEcho(@Query("msg") String message);

}
