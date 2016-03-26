package com.phuentemann.www.lampcontrol;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;

public class HomeScreen extends AppCompatActivity {
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;
    final Handler handler = new Handler();

    String ip;
    int port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();
        addDrawerItems();
        setupDrawer();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
    }

    private void addDrawerItems() {
        String[] listArray = { "LampControl", "ESP8266", "Settings", "About" };
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listArray);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Navigation");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        if (mDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            mDrawerList.setItemChecked(position, true);
            selectItem(position);
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    private void selectItem(int position) {
        Log.d("Test", "Position: " + Integer.toString(position));
        if (position == 0) {
            Intent a = new Intent(this, HomeScreen.class);
            startActivity(a);
        }else if (position == 1) {
            Intent b = new Intent(this, esp8266.class);
            startActivity(b);
        }else if (position == 2) {
            Intent c = new Intent(this, Settings.class);
            startActivity(c);
        }else if (position == 3) {
            Intent d = new Intent(this, About.class);
            startActivity(d);
        }
    }

    public void sendCommand(View v) {
        String command = v.getTag().toString();
        Log.d("LampControl", command);
        new SocketTask().execute(command);
    }

    private class SocketTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params){
            String command = params[0];
            Socket socket;
            String response;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(HomeScreen.this);
            ip = prefs.getString("rpi_ipaddress", "192.168.2.201");
            port = Integer.valueOf(prefs.getString("rpi_port", "8000"));

            handler.post(new Runnable(){
                @Override
                public void run(){
                    Toast.makeText(HomeScreen.this, String.format(Locale.ENGLISH,
                            "Ip: %s\nPort: %d", ip, port), Toast.LENGTH_LONG).show();
                }
            });

            try {
                socket = new Socket(ip, port);
                socket.setSoTimeout(10000);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);

                pw.println(command);

                while ((response = br.readLine()) != null) {
                    if (response.equals("\n")) {
                        break;
                    }
                }
                br.close();
                pw.close();
                socket.close();
            }catch (IOException e) {
                Log.d("SocketException: ", e.toString());
            }
            return "success";
        }

        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);
        }
    }
}







