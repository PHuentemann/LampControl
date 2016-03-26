package com.phuentemann.www.lampcontrol;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class esp8266 extends AppCompatActivity {
    final Handler handler = new Handler();
    final Handler timerHandler = new Handler();
    private GraphView graph;
    private LineGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;
    Boolean status = false;
    TextView txtView;
    Button btnRefresh;
    Button btnClear;
    Button btnAuto;


    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            new SocketTask().execute();
            timerHandler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp8266);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setTitle("ESP8266");

        // Get the action bar to show the back button
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        txtView = (TextView)findViewById(R.id.espTextView);
        // Button to manually refresh
        btnRefresh = (Button)findViewById(R.id.espButtonRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Log.d("esp8266", "Starting new instance of AsyncTask");
                new SocketTask().execute();
            }
        });
        // Button to clear the graph
        btnClear = (Button)findViewById(R.id.espButtonClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Calendar calendar = Calendar.getInstance();
                Date now = calendar.getTime();
                try {
                    series1.resetData(new DataPoint[]{
                            new DataPoint(now.getTime(), 0)});
                    series2.resetData(new DataPoint[]{
                            new DataPoint(now.getTime(), 0)});
                } catch (Exception e) {
                    Log.e("esp8266", e.getMessage());
                }
            }
        });
        // Button to automatically request new data every * seconds
        btnAuto = (Button)findViewById(R.id.espButtonAuto);
        btnAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                try {
                    if (!status) {
                        timerHandler.postDelayed(timerRunnable, 0);
                        btnAuto.setText("MANUAL");
                    } else {
                        timerHandler.removeCallbacks(timerRunnable);
                        btnAuto.setText("AUTOMATIC");
                    }
                    status = !status;
                } catch (Exception e) {
                    Log.e("esp8266", e.getMessage());
                }
            }
        });
        // Create first datapoint
        Calendar calendar = Calendar.getInstance();
        Date first = calendar.getTime();
        calendar.add(Calendar.MINUTE, 10);
        Date second = calendar.getTime();
        // Setting up graph view
        graph = (GraphView)findViewById(R.id.graph);
        graph.setTitle("Temperature and Humidity");
        graph.getViewport().setScrollable(true);
        // Set x label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(3);
        graph.getGridLabelRenderer().setNumVerticalLabels(6);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.BLUE);
        // Set X-Axis to manual and define minX and maxX
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(first.getTime());
        graph.getViewport().setMaxX(second.getTime());
        // Define first series for graph
        series1 = new LineGraphSeries<DataPoint>(new DataPoint[]{
                new DataPoint(first.getTime(), 0)});
        series1.setColor(Color.BLUE);
        series1.setTitle("Temperature");
        graph.addSeries(series1);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(15);
        graph.getViewport().setMaxY(30);
        // Define second series for graph
        series2 = new LineGraphSeries<DataPoint>(new DataPoint[]{
                new DataPoint(first.getTime(), 0)});
        series2.setColor(Color.RED);
        series2.setTitle("Humidity");
        graph.getSecondScale().addSeries(series2);
        graph.getSecondScale().setMinY(10);
        graph.getSecondScale().setMaxY(90);
        graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.RED);
        // Set graph legend
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
    }

    public void addDataPoint(final Date x, final int y1, final int y2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    series1.appendData(new DataPoint(x.getTime(), y1), true, 25);
                    series2.appendData(new DataPoint(x.getTime(), y2), true, 25);
                } catch (Exception e) {
                    Log.e("esp8266", e.getMessage());
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
    }

    private class SocketTask extends AsyncTask<String, Void, String> {
        Socket socket = null;
        String response = null;
        String ip;
        Integer port;
        Boolean debug;
        PrintWriter out = null;
        BufferedReader in = null;

        @Override
        protected void onPreExecute() {
            Log.d("onPreExec", "Starting onPreExecute()");
        }

        @Override
        protected String doInBackground(String...args) {
            Log.d("doInBg", "Starting doInBackground()");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(esp8266.this);
            port = Integer.valueOf(prefs.getString("esp_port", "7777"));
            ip = prefs.getString("esp_ipaddress", "192.168.4.1");
            debug = prefs.getBoolean("debug", false);
            if (debug) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(esp8266.this, String.format(Locale.ENGLISH,
                                "Ip: %s\nPort: %d", ip, port), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            try {
                socket = new Socket();
                socket.setSendBufferSize(2048);
                socket.bind(null);
                socket.connect((new InetSocketAddress(ip, port)), 10000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("getdata");
                out.flush();
                while ((response = in.readLine()) != null) {
                    Log.d("test-response", response);
                    if (response.contains(",")) {
                        break;
                    }
                }
                Log.d("Received message", response);
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                Log.d("SocketException", e.getMessage());
            } catch (Exception e) {
                Log.d("Exception", e.getMessage());
            }
            return response;
        }

        @Override
        protected void onPostExecute(final String result){
            Log.d("onPostExec", "Starting onPostExecute()");
            if (result != null) {
                if (debug) {Toast.makeText(esp8266.this, result, Toast.LENGTH_LONG).show();}
                String[] parts = response.split(",");
                txtView.setText(String.format(Locale.ENGLISH,
                        "Temperature: %s °C\nHumidity: %s %%", parts[0], parts[1]));
                //long unixTime = System.currentTimeMillis() / 1000;
                Calendar calendar = Calendar.getInstance();
                Date date = calendar.getTime();
                addDataPoint(date, Integer.valueOf(parts[0]), Integer.valueOf(parts[1]));
            } else {
                if (debug) {Toast.makeText(esp8266.this, "No response!", Toast.LENGTH_LONG).show();}
            }
        }
    }
}
