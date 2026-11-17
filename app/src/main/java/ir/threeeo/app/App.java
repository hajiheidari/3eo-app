package ir.threeeo.app;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        L.init(this);
    }
}
