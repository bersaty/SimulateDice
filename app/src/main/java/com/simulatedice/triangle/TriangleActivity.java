package com.simulatedice.triangle;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;


public class TriangleActivity extends AppCompatActivity {

    GLSurfaceView mGLSurfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TriangleRenderer render = new TriangleRenderer();
        mGLSurfaceView = new GLSurfaceView(this);

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2)
        {
            // Request an OpenGL ES 2.0 compatible context.
            mGLSurfaceView.setEGLContextClientVersion(2);

            // Set the renderer to our demo renderer, defined below.
            mGLSurfaceView.setRenderer(render);
        }
        else
        {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return;
        }

        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onPause() {
        mGLSurfaceView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mGLSurfaceView.onResume();
        super.onResume();
    }

}
