package com.millicast.android_app;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;

/**
 * Entry point to Sample Application (SA) for the Millicast Android SDK.
 * It initialises the {@link MillicastManager} with the ApplicationContext.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String TAG = "MainActivity";
    private static Context context;
    private static int fragmentId = R.id.nav_settings_mc;
    private static MillicastManager mcManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize on App start.
        if (savedInstanceState == null) {
            context = getApplicationContext();
            mcManager = MillicastManager.getSingleInstance();
            mcManager.init(context);
            replaceFragment(fragmentId);
        }

        mcManager.setMainActivity(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigaation_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Will be able to restore from both restart and new activity.
        mcManager.restoreCameraLock();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        onNavigationItemSelected(item);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        System.out.println("NAV CLICKED");

        int id = item.getItemId();

        replaceFragment(id);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void replaceFragment(int id) {
        Fragment fragment = null;
        if (id == R.id.nav_publish) {
            fragment = new PublishFragment();
        } else if (id == R.id.nav_subscribe) {
            fragment = new SubscribeFragment();
        } else if (id == R.id.nav_settings_mc) {
            fragment = new SettingsMcFragment();
        } else if (id == R.id.nav_settings_media) {
            fragment = new SettingsMediaFragment();
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment).commit();
            fragmentId = id;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Only take action if not changing orientation.
        if (isChangingConfigurations()) {
            Log.d(TAG, "[onStop] Changing orientation.");
            // Release View related objects
            mcManager.releaseViews();
        } else {
            // Release camera.
            mcManager.setCameraLock(false);
            // If SA being destroyed, release Millicast objects.
            if (isFinishing()) {
                Log.d(TAG, "[onStop] MainActivity finishing. Releasing Millicast objects.");
                mcManager.release();
            } else {
                mcManager.flagCameraRestore();
                Log.d(TAG, "[onStop] MainActivity not finishing. Will not release Millicast objects.Changing orientation.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}