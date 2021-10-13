package ua.naiksoftware.stompclientexample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import io.reactivex.CompletableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private StompClient mStompClient;
    private String url = "wss://dev.api.merlyn.org/ws/websocket";
    private CompositeDisposable compositeDisposable;
    private Disposable mRestPingDisposable;
    private String jsonConfig ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP,url);

        ConfigMessage configMessage2 = new ConfigMessage();
        configMessage2.setType(ConfigMessage.MessageType.REFRESH_REQUEST);
        configMessage2.setReceiver("DEVICE1");
        configMessage2.setContent("The Content 2");
        Gson gson = new Gson();
        jsonConfig = gson.toJson(configMessage2);


        resetSubscriptions();
    }

    public void disconnectStomp(View view) {
        mStompClient.disconnect();
    }


    public void connectStomp(View view) {
        mStompClient.withClientHeartbeat(1000).withServerHeartbeat(1000);
        resetSubscriptions();
        Disposable dispLifecycle = mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            toast("Stomp connection opened");
                            break;
                        case ERROR:
                            Log.e(TAG, "Stomp connection error", lifecycleEvent.getException());
                            toast("Stomp connection error");
                            break;
                        case CLOSED:
                            toast("Stomp connection closed");
                            resetSubscriptions();
                            break;
                        case FAILED_SERVER_HEARTBEAT:
                            toast("Stomp failed server heartbeat");
                            break;
                    }
                });

        compositeDisposable.add(dispLifecycle);

        Disposable dispTopic = mStompClient.topic("/topic/public")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.d(TAG, "Received " + topicMessage.getPayload());
                }, throwable -> {
                    Log.e(TAG, "Error on subscribe topic", throwable);
                });

        compositeDisposable.add(dispTopic);

        mStompClient.connect();
    }

    public void sendEchoViaStomp(View v) {
        if (!mStompClient.isConnected()) {
            return;
        }else {
            Log.e("result" , " connected ");
        }
        compositeDisposable.add(mStompClient.send("/app/device.refresh", jsonConfig)
                .compose(applySchedulers())
                .subscribe(() -> {
                    Log.d(TAG, "STOMP echo send successfully");
                }, throwable -> {
                    Log.e(TAG, "Error send STOMP echo", throwable);
                    toast(throwable.getMessage());
                }));
    }

    public void sendEchoViaRest(View v) {
        mRestPingDisposable = RestClient.getInstance().getIApiService()
                .sendRestEcho(jsonConfig)
                .compose(applySchedulers())
                .subscribe(() -> Log.d(TAG, "REST echo send successfully"),
                        throwable -> {
                            Log.e(TAG, "Error send REST echo", throwable);
                            MainActivity.this.toast(throwable.getMessage());
                        });
    }


    private void toast(String text) {
        Log.i(TAG, text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    protected CompletableTransformer applySchedulers() {
        return upstream -> upstream
                .unsubscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void resetSubscriptions() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    protected void onDestroy() {
        mStompClient.disconnect();

        if (compositeDisposable != null) compositeDisposable.dispose();
        if (mRestPingDisposable != null) mRestPingDisposable.dispose();
        super.onDestroy();
    }
}
