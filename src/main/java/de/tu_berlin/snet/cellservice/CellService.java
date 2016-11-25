package de.tu_berlin.snet.cellservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;



public class CellService extends Service {
    private static Service instance;

    public CellService() {
    }

    // Allows others to retrieve a reference to this service, to get the context
    public static Service get() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //mobileNetworkHelper = MobileNetworkHelper.getInstance(this);
        Toast.makeText(this, "Service is created", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service is started", Toast.LENGTH_LONG).show();


        //mobileNetworkHelper.addListener(cdrReceiver);
       // mobileNetworkHelper.onStart();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
       // mobileNetworkHelper.onStop();
       // mobileNetworkHelper.removeListener(cdrReceiver);
        Toast.makeText(this, "Service is stopped", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }





}
