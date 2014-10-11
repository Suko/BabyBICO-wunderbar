package com.babybico.relayr;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import io.relayr.LoginEventListener;
import io.relayr.RelayrSdk;
import io.relayr.model.DeviceModel;
import io.relayr.model.Reading;
import io.relayr.model.Transmitter;
import io.relayr.model.TransmitterDevice;
import io.relayr.model.User;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity implements LoginEventListener {

  //  private TextView temperatureValueTextView;
  //  private TextView accelerometerTextView;
 //   private TextView soundTextView;

    private TransmitterDevice mDevice;
    private Subscription mUserInfoSubscription;
    private Subscription wundebarDeviceSubscription;
    private Subscription mWebSocketSubscription;
    private String TAG = "MY------------Activity---------------";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(this, R.layout.owerview, null);
        setContentView(view);
        Log.w(TAG,"start");
       // temperatureValueTextView = (TextView) view.findViewById(R.id.temperature_value);
       // accelerometerTextView = (TextView) view.findViewById(R.id.accelerometer_value);
      //  soundTextView = (TextView) view.findViewById(R.id.sound_value);

        // TODO button

        view.findViewById(R.id.btn_sleeping).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWarning(WarningType.SLEEPING);
            }
        });

  /*      view.findViewById(R.id.btn_high).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWarning(WarningType.HIGH_TEMPERATURE);
            }
        });


        view.findViewById(R.id.btn_sound).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWarning(WarningType.SOUND);
            }
        });

        view.findViewById(R.id.btn_sleeping).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWarning(WarningType.SLEEPING);
            }
        });

        view.findViewById(R.id.btn_low).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWarning(WarningType.LOW_TEMPERATURE);
            }
        });
 */


        if (!RelayrSdk.isUserLoggedIn()) {
            RelayrSdk.logIn(this, this);
        }



    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (RelayrSdk.isUserLoggedIn()) {
            getMenuInflater().inflate(R.menu.thermometer_demo_logged_in, menu);
        } else {
            getMenuInflater().inflate(R.menu.thermometer_demo_not_logged_in, menu);
        }
        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_log_in) {
            RelayrSdk.logIn(this, this);
            return true;
        } else if (item.getItemId() == R.id.action_log_out) {
            logOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logOut() {
        unSubscribeToUpdates();
        RelayrSdk.logOut();
        invalidateOptionsMenu();
        Toast.makeText(this, R.string.successfully_logged_out, Toast.LENGTH_SHORT).show();
        updateUiForANonLoggedInUser();
    }

    private void updateUiForANonLoggedInUser() {

     //   temperatureValueTextView.setVisibility(View.GONE);
     //   accelerometerTextView.setVisibility(View.GONE);
     //   soundTextView.setVisibility(View.GONE);
    }

    private void updateUiForALoggedInUser() {

     //   temperatureValueTextView.setVisibility(View.VISIBLE);
     //   accelerometerTextView.setVisibility(View.VISIBLE);
     //   soundTextView.setVisibility(View.VISIBLE);
        Log.w(TAG,"load user info");
        loadUserInfo();
    }

    private void loadUserInfo() {
        mUserInfoSubscription = RelayrSdk.getRelayrApi()
                .getUserInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override
                    public void onCompleted() {
                        Log.w(TAG, "eda");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, R.string.something_went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(User user) {

                        loadWundabarDevice(user);
                    }
                });
    }

    private void loadWundabarDevice(User user) {
        Log.w(TAG,"load device");
        wundebarDeviceSubscription = RelayrSdk.getRelayrApi()

                .getTransmitters(user.id)
                .flatMap(new Func1<List<Transmitter>, Observable<List<TransmitterDevice>>>() {
                    @Override
                    public Observable<List<TransmitterDevice>> call(List<Transmitter> transmitters) {
                        // Transmitter

                        if (transmitters.isEmpty())
                            return Observable.from(new ArrayList<List<TransmitterDevice>>());
                        return RelayrSdk.getRelayrApi().getTransmitterDevices(transmitters.get(0).id);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<TransmitterDevice>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, R.string.something_went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(List<TransmitterDevice> devices) {


                        for (TransmitterDevice device : devices) {
                            if (device.model.equals(DeviceModel.TEMPERATURE_HUMIDITY.getId()) ) {
                                subscribeForSensorsUpdates(device);

                            } else if

                            (device.model.equals(DeviceModel.ACCELEROMETER_GYROSCOPE.getId()) ) {
                                subscribeAcceleratorSensorsUpdates(device);

                            }
                            else if (device.model.equals(DeviceModel.MICROPHONE.getId()) ) {
                                subscribeMicrophoneSensorsUpdates(device);

                            }


                        }
                    }
                });

    }

    @Override
    protected void onPause() {
        super.onPause();
        unSubscribeToUpdates();
    }

    private static boolean isSubscribed(Subscription subscription) {
        return subscription != null && !subscription.isUnsubscribed();
    }

    private void unSubscribeToUpdates() {
        if (isSubscribed(mUserInfoSubscription)) {
            mUserInfoSubscription.unsubscribe();
        }
        if (isSubscribed(wundebarDeviceSubscription)) {
            wundebarDeviceSubscription.unsubscribe();
        }
        if (isSubscribed(mWebSocketSubscription)) {
            mWebSocketSubscription.unsubscribe();
            RelayrSdk.getWebSocketClient().unSubscribe(mDevice.id);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (RelayrSdk.isUserLoggedIn()) {
            updateUiForALoggedInUser();
        } else {
            updateUiForANonLoggedInUser();
        }
    }

    private void subscribeMicrophoneSensorsUpdates(TransmitterDevice device){
        mDevice = device;
        Log.w(TAG,"Microphone");
        mWebSocketSubscription = RelayrSdk.getWebSocketClient()
                .subscribe(device, new Subscriber<Object>() {

                    @Override
                    public void onCompleted() {

                        Log.w(TAG,"ON COMPLETE");

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, R.string.something_went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(Object o) {
                        Reading reading = new Gson().fromJson(o.toString(), Reading.class);

                        Log.w(TAG,"Microphone NEXT");

                       //soundTextView.setText(reading.snd_level + "Db");

                       float microphone = reading.snd_level;

                        // TODO microphone trigger

                        if(microphone > 200){

                            showWarning(WarningType.SOUND);
                        }


                        Log.w(TAG,"HERE");

                    }
                });

    }

    private void subscribeAcceleratorSensorsUpdates(TransmitterDevice device){
        mDevice = device;
        Log.w(TAG,"subscribe Accelerometer sensor data");
        mWebSocketSubscription = RelayrSdk.getWebSocketClient()
                .subscribe(device, new Subscriber<Object>() {

                    @Override
                    public void onCompleted() {

                        Log.w(TAG,"ON COMPLETE");

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, R.string.something_went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(Object o) {
                        Reading reading = new Gson().fromJson(o.toString(), Reading.class);

                        Log.w(TAG,"Accelerometer NEXT");

                        //accelerometerTextView.setText(reading.accel.x + "˚C");

                        float accelerometer = reading.accel.x;

                        //TODO accelerometer trigger !!!!

                        if (accelerometer > 0.12 ){
                            showWarning(WarningType.AWAKE);
                        }

                        Log.w(TAG,"HERE");



                    }
                });

    }

    private void subscribeForSensorsUpdates(TransmitterDevice device) {
        mDevice = device;
        Log.w(TAG,"subscribe Temp sensor data");
        mWebSocketSubscription = RelayrSdk.getWebSocketClient()
                .subscribe(device, new Subscriber<Object>() {

                    @Override
                    public void onCompleted() {

                        Log.w(TAG,"ON COMPLETE");

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(MainActivity.this, R.string.something_went_wrong,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(Object o) {
                        Reading reading = new Gson().fromJson(o.toString(), Reading.class);

                        Log.w(TAG,"Temp On NEXT");

                        //temperatureValueTextView.setText(reading.temp + "˚C");

                        float temp = reading.temp;

                        // TODO temperature trigger

                        if(temp > 30){

                            showWarning(WarningType.HIGH_TEMPERATURE);

                        }

                        if (temp < 20){
                            showWarning(WarningType.LOW_TEMPERATURE);
                        }

                        Log.w(TAG,"HERE");

                    }
                });

    }

    private void showWarning(WarningType warningType) {

        Class<?> activityClass;
        String title;
        String text;
        int icon;
        int background;

        switch(warningType) {
            case HIGH_TEMPERATURE:
                activityClass = HighTemperatureActivity.class;
                title ="High Temperature";
                text = "Lower the room temperature";
                icon = R.drawable.high_temp_s;
                background = R.drawable.red_64;
                break;
            case LOW_TEMPERATURE:
                activityClass = LowTemperatureActivity.class;
                title ="Low Temperature";
                text = "Higher the temperature";
                icon = R.drawable.low_temp_s;
                background = R.drawable.dark_blue_64;
                break;
            case AWAKE:
                // TODO dodaj class, title i text !!//
                activityClass = AwakeActivity.class;
                title ="I am Awake!";
                text = "";
                icon = R.drawable.awake_s;
                background = R.drawable.green_64;
                break;
            case SOUND:
                // TODO dodaj class, title i text
                activityClass = SoundActivity.class;
                title ="It is loud";
                text = "I might can awake";
                icon = R.drawable.sound;
                background = R.drawable.orange_64;
                break;
            case SLEEPING:
                // TODO dodaj class, title i text
                activityClass = SleepingActivity.class;
                title ="I am sleeping";
                text = " :)";
                icon = R.drawable.sleeping_s;
                background = R.drawable.blue_64;
                break;
            default:
                return;
        }

        showNotification(activityClass, title, text, icon, background);
        openScreen(activityClass);
    }

    private void openScreen(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void showNotification(Class<?> activityClass, String title, String text,int icon, int background) {
        int notificationId = 001;
// Build intent for notification content
        Intent viewIntent = new Intent(this, activityClass);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(this, 0, viewIntent, 0);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(icon)
                        .setLargeIcon(BitmapFactory.decodeResource(
                          getResources(), background))
                //   .setLargeIcon(BitmapFactory.decodeResource(
                //          getResources(), background))
                .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(viewPendingIntent)
                        .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                        .setAutoCancel(true)
                ;

// Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

// Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }


    @Override
    public void onSuccessUserLogIn() {
        Toast.makeText(this, R.string.successfully_logged_in, Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
        updateUiForALoggedInUser();
    }

    @Override
    public void onErrorLogin(Throwable e) {
        Toast.makeText(this, R.string.unsuccessfully_logged_in, Toast.LENGTH_SHORT).show();
        updateUiForANonLoggedInUser();
    }
}
