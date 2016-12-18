package com.demo.data.crashdata;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;

import android.graphics.Color;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBarUtils;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;


public class HeatMapActivity extends BaseDemoActivity {

    /**
     * Result code coming back from QueryFilter
     */
    private static final int QUERY_FILTER_RESULT_CODE = 515;

    /**
     * shared preference
     */
    private static final String CRASH_DATA_PREFS = "SHARED_PREFS";


    /**
     * Alternative radius for convolution
     */
    private static final int ALT_HEATMAP_RADIUS = 10;

    /**
     * Alternative opacity of heatmap overlay
     */
    private static final double ALT_HEATMAP_OPACITY = 0.4;

    /**
     * Alternative heatmap gradient (blue -> red)
     * Copied from Javascript version
     */
    private static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
            Color.argb(0, 0, 255, 255),// transparent
            Color.argb(255 / 3 * 2, 0, 255, 255),
            Color.rgb(0, 191, 255),
            Color.rgb(0, 0, 127),
            Color.rgb(255, 0, 0)
    };

    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = {
            0.0f, 0.10f, 0.20f, 0.60f, 1.0f
    };

    public static final Gradient ALT_HEATMAP_GRADIENT = new Gradient(ALT_HEATMAP_GRADIENT_COLORS,
            ALT_HEATMAP_GRADIENT_START_POINTS);

    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    private boolean mDefaultGradient = true;
    private boolean mDefaultRadius = true;
    private boolean mDefaultOpacity = true;

    private String AND = "%20AND%20";
    private String gt = "%3E";
    private String lt = "%3C";
    private String sp = "%20";
    private String sq = "%27";

    private SmoothProgressBar mPocketBar;

    /**
     * Maps name of data set to data (list of LatLngs)
     * Also maps to the URL of the data set for attribution
     */
    private HashMap<String, DataSet> mLists = new HashMap<String, DataSet>();

    ArrayList<LatLng> coords = new ArrayList<LatLng>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_heat_map;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent queryFilter = new Intent(HeatMapActivity.this, QueryFilterActivity.class);
                startActivityForResult(queryFilter, QUERY_FILTER_RESULT_CODE);

//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });

        mPocketBar = (SmoothProgressBar) findViewById(R.id.smoothprogressbar);
        mPocketBar.setSmoothProgressDrawableBackgroundDrawable(
                SmoothProgressBarUtils.generateDrawableWithColors(
                        getResources().getIntArray(R.array.pocket_background_colors),
                        ((SmoothProgressDrawable) mPocketBar.getIndeterminateDrawable()).getStrokeWidth()));



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == QUERY_FILTER_RESULT_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                query();
            }else if(resultCode == RESULT_CANCELED){
                query();
            }
        }
    }


    @Override
    protected void startDemo() {
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.6005, -93.6091), 6));
        query();
    }

    private void query() {
        mPocketBar.progressiveStart();
        DataSetQueryTask queryTask = new DataSetQueryTask();
        queryTask.execute("");
    }


    private class DataSetQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {

            URL url = null;
            HttpURLConnection urlConnection = null;
            try {

                StringBuilder whereSb = new StringBuilder();

                StringBuilder inSb = new StringBuilder();
                inSb.append("(");
                // TODO: move this section into where builder/helper
                Map<String, Boolean> mapValues = loadMap();
                if (mapValues != null && mapValues.size() > 0) {

                    for (Map.Entry<String, Boolean> entry : mapValues.entrySet()) {

                        if (entry.getValue()) {

                            if (entry.getKey().equals("1")) {
                                inSb.append("1,");
                            }else if(entry.getKey().equals("31")){
                                inSb.append("31,32,33,34,35,36,37,38,39,40,41,");
                            }else if(entry.getKey().equals("24")){
                                inSb.append("24,");
                            }

                        }

                    }
                    if(inSb.length() == 1){
                        inSb.append("1,");
                    }

                }else{
                    inSb.append("1,");
                }

                inSb.setLength(inSb.length() - 1);
                inSb.append(")");
                whereSb.append("MAJCSE in ");
                whereSb.append(inSb.toString());
                whereSb.append(" AND ");
                whereSb.append(getDateClause());

                StringBuilder sb = new StringBuilder();
                sb.append("https://gis.iowadot.gov/public/rest/services/Traffic_Safety/Crash_Data/MapServer/0/query?where=");
                sb.append(URLEncoder.encode(whereSb.toString()));
                sb.append("&text=&objectIds=&time=&geometry=&geometryType=esriGeometryEnvelope&inSR=&spatialRel=esriSpatialRelIntersects&relationParam=&outFields=&returnGeometry=true&returnTrueCurves=false&maxAllowableOffset=&geometryPrecision=&outSR=4326&returnIdsOnly=false&returnCountOnly=false&orderByFields=&groupByFieldsForStatistics=&outStatistics=&returnZ=false&returnM=false&gdbVersion=&returnDistinctValues=false&resultOffset=&resultRecordCount=&f=geojson");

//                url = new URL("https://gis.iowadot.gov/public/rest/services/Traffic_Safety/Crash_Data/MapServer/0/query?where=MAJCSE+%3D+1+AND+CRASH_DATE+%3E%3D+date+%2701%2F01%2F2014+00%3A00%3A00%27+and+CRASH_DATE+%3C%3D+date+%2701%2F01%2F2015+00%3A00%3A00%27&text=&objectIds=&time=&geometry=&geometryType=esriGeometryEnvelope&inSR=&spatialRel=esriSpatialRelIntersects&relationParam=&outFields=&returnGeometry=true&returnTrueCurves=false&maxAllowableOffset=&geometryPrecision=&outSR=4326&returnIdsOnly=false&returnCountOnly=false&orderByFields=&groupByFieldsForStatistics=&outStatistics=&returnZ=false&returnM=false&gdbVersion=&returnDistinctValues=false&resultOffset=&resultRecordCount=&f=geojson");
                url = new URL(sb.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                JSONObject mainObject = new JSONObject(total.toString());
                JSONArray mainarray = mainObject.getJSONArray("features");

                coords = new ArrayList<LatLng>();
                for (int i = 0; i < mainarray.length(); i++) {
                    JSONObject element = mainarray.getJSONObject(i);
                    double lat = element.getJSONObject("geometry").getJSONArray("coordinates").getDouble(1);
                    double lng = element.getJSONObject("geometry").getJSONArray("coordinates").getDouble(0);
                    LatLng latLng = new LatLng(lat, lng);
                    coords.add(latLng);
                }

            } catch (Exception ex) {
                Log.e("jsonerror", ex.toString());

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }


            return null;
        }

        private String getDateClause() {
            String dateClause = "";
            SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences(CRASH_DATA_PREFS, Context.MODE_PRIVATE);
            if (pSharedPref != null) {
                String startDate = pSharedPref.getString("startdate", "");
                String endDate = pSharedPref.getString("enddate", "");

                if (!startDate.isEmpty() && !endDate.isEmpty()) {
                    dateClause = startDate + " AND " + endDate;
                } else {
                    // TODO: find most recent data, time from then
                    // 30 days
                    dateClause = "CRASH_DATE >= date '01/01/2013 00:00:00' AND CRASH_DATE <= date '01/01/2014 00:00:00'";
                }


            }
            return dateClause;
        }

        private void saveMap(Map<String, Boolean> inputMap) {
            SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences(CRASH_DATA_PREFS, Context.MODE_PRIVATE);
            if (pSharedPref != null) {
                JSONObject jsonObject = new JSONObject(inputMap);
                String jsonString = jsonObject.toString();
                SharedPreferences.Editor editor = pSharedPref.edit();
                editor.remove("My_map").commit();
                editor.putString("My_map", jsonString);
                editor.commit();
            }
        }

        private Map<String, Boolean> loadMap() {
            Map<String, Boolean> outputMap = new HashMap<String, Boolean>();
            SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences(CRASH_DATA_PREFS, Context.MODE_PRIVATE);
            try {
                if (pSharedPref != null) {
                    String jsonString = pSharedPref.getString("My_map", (new JSONObject()).toString());
                    JSONObject jsonObject = new JSONObject(jsonString);
                    Iterator<String> keysItr = jsonObject.keys();
                    while (keysItr.hasNext()) {
                        String key = keysItr.next();
                        Boolean value = (Boolean) jsonObject.get(key);
                        outputMap.put(key, value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return outputMap;
        }


        @Override
        protected void onPostExecute(String result) {
            mLists.put(getString(R.string.deer), new DataSet(coords,
                    getString(R.string.deer_url)));

            if(coords != null && coords.size() > 0) {

                if (mProvider == null) {
                    mProvider = new HeatmapTileProvider.Builder().data(
                            mLists.get(getString(R.string.deer)).getData()).build();
                    mOverlay = getMap().addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
                } else {
                    mProvider.setData(mLists.get("deer").getData());
                    mOverlay.clearTileCache();
                }
            }else{

                // TODO: tell user empty results
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                if(mProvider != null) {
                    mOverlay.remove();
                }
            }

            mPocketBar.progressiveStop();
        }

    }

    public void changeRadius(View view) {
        if (mDefaultRadius) {
            mProvider.setRadius(ALT_HEATMAP_RADIUS);
        } else {
            mProvider.setRadius(HeatmapTileProvider.DEFAULT_RADIUS);
        }
        mOverlay.clearTileCache();
        mDefaultRadius = !mDefaultRadius;
    }

    public void changeGradient(View view) {
        if (mDefaultGradient) {
            mProvider.setGradient(ALT_HEATMAP_GRADIENT);
        } else {
            mProvider.setGradient(HeatmapTileProvider.DEFAULT_GRADIENT);
        }
        mOverlay.clearTileCache();
        mDefaultGradient = !mDefaultGradient;
    }

    public void changeOpacity(View view) {
        if (mDefaultOpacity) {
            mProvider.setOpacity(ALT_HEATMAP_OPACITY);
        } else {
            mProvider.setOpacity(HeatmapTileProvider.DEFAULT_OPACITY);
        }
        mOverlay.clearTileCache();
        mDefaultOpacity = !mDefaultOpacity;
    }

    /**
     * Helper class - stores data sets and sources.
     */
    private class DataSet {
        private ArrayList<LatLng> mDataset;
        private String mUrl;

        public DataSet(ArrayList<LatLng> dataSet, String url) {
            this.mDataset = dataSet;
            this.mUrl = url;
        }

        public ArrayList<LatLng> getData() {
            return mDataset;
        }

        public String getUrl() {
            return mUrl;
        }
    }


}
