package com.demo.data.crashdata;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.TimePickerDialog;


public class QueryFilterActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, DatePickerDialog.OnDateSetListener {

    public static final String DATEPICKER_TAG = "datepicker";
    /**
     * shared preference
     */
    private static final String CRASH_DATA_PREFS = "SHARED_PREFS";

    SwitchCompat animalSwitch;
    SwitchCompat distractedSwitch;
    SwitchCompat followedTooCloseSwitch;

    DatePickerDialog datePickerDialog;
    DatePickerDialog startDatePickerDialog;

    TextView startDateInput;
    TextView endDateInput;

    Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_filter);

        animalSwitch = (SwitchCompat) findViewById(R.id.switchanimal);
        distractedSwitch = (SwitchCompat) findViewById(R.id.switchdistracted);
        followedTooCloseSwitch = (SwitchCompat) findViewById(R.id.switchfollowedtooclose);

        startDateInput = (TextView) findViewById(R.id.startdateinput);
        endDateInput = (TextView) findViewById(R.id.enddateinput);

        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences(CRASH_DATA_PREFS, Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            String storedstartdate = pSharedPref.getString("startdateinput", "None");
            String storedenddate = pSharedPref.getString("enddateinput", "None");
            startDateInput.setText(storedstartdate);
            endDateInput.setText(storedenddate);

            String[] startArray;
            if(!storedenddate.equals("None")) {
                startArray = storedstartdate.split("/");
                startDatePickerDialog = DatePickerDialog.newInstance(this, Integer.parseInt(startArray[2]),(Integer.parseInt(startArray[0])-1) , Integer.parseInt(startArray[1]),false);
            }
            String[] endArray;
            if(!storedenddate.equals("None")){
                endArray = storedenddate.split("/");
                datePickerDialog = DatePickerDialog.newInstance(this, Integer.parseInt(endArray[2]), (Integer.parseInt(endArray[0])-1), Integer.parseInt(endArray[1]), false);
            }
            final Calendar calendar = Calendar.getInstance();
            if(startDatePickerDialog == null) {
                startDatePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), false);
            }

            if(datePickerDialog == null) {
                datePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), false);
            }
        }

        HashMap<String,Boolean> mapValues = new HashMap<String,Boolean>(loadMap());
        if (mapValues != null && mapValues.size() > 0) {
            for (Map.Entry<String, Boolean> entry : mapValues.entrySet()) {
                if (entry.getValue()) {
                    if (entry.getKey().equals("1")) {
                        animalSwitch.setChecked(true);
                    }else if(entry.getKey().equals("31")){
                        distractedSwitch.setChecked(true);
                    }else if(entry.getKey().equals("24")){
                        followedTooCloseSwitch.setChecked(true);
                    }
                }
            }
        }

        animalSwitch.setOnCheckedChangeListener(this);
        distractedSwitch.setOnCheckedChangeListener(this);
        followedTooCloseSwitch.setOnCheckedChangeListener(this);




//        datePickerDialog = DatePickerDialog.newInstance(this, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), true);

        if (savedInstanceState != null) {
            DatePickerDialog dpd = (DatePickerDialog) getSupportFragmentManager().findFragmentByTag(DATEPICKER_TAG);
            if (dpd != null) {
                dpd.setOnDateSetListener(this);
            }
        }

        findViewById(R.id.startdatelayout).setOnClickListener(startDateClick);
        findViewById(R.id.enddatelayout).setOnClickListener(endDateClick);

        submit = (Button)findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });

    }



    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day) {
        Toast.makeText(QueryFilterActivity.this, "new date:" + year + "-" + month + "-" + day, Toast.LENGTH_LONG).show();
        SharedPreferences pSharedPref = getApplicationContext().getSharedPreferences(CRASH_DATA_PREFS, Context.MODE_PRIVATE);
        String formattedDate = (month+1)+"/"+day+"/"+year;
        switch (datePickerDialog.getTag()) {
            case ("startDate"):
                if (pSharedPref != null) {
                    SharedPreferences.Editor editor = pSharedPref.edit();
                    editor.remove("startdate").commit();
                    editor.remove("startdateinput").commit();
                    editor.putString("startdate", "CRASH_DATE >= date '"+formattedDate+" 00:00:00'");
                    editor.putString("startdateinput",formattedDate);
                    editor.commit();
                }
                startDateInput.setText(formattedDate);
                break;
            case ("endDate"):
                if (pSharedPref != null) {
                    SharedPreferences.Editor editor = pSharedPref.edit();
                    editor.remove("enddate").commit();
                    editor.remove("enddateinput").commit();

                    editor.putString("enddate", "CRASH_DATE <= date '"+formattedDate+" 00:00:00'");
                    editor.putString("enddateinput", formattedDate);
                    editor.commit();
                }
                endDateInput.setText(formattedDate);
                break;
        }



    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        HashMap<String,Boolean> causeMap = new HashMap<String,Boolean>(loadMap());

        switch (buttonView.getId()) {
            case R.id.switchanimal:
                Log.i("switchanimal ", isChecked + "");

                if(isChecked){
                    causeMap.put("1",true);
                }else{
                    causeMap.put("1",false);
                }

                break;
            case R.id.switchdistracted:
                Log.i("switchdistracted ", isChecked + "");
                if(isChecked){
                    causeMap.put("31",true);
                }else{
                    causeMap.put("31",false);
                }
                break;
            case R.id.switchfollowedtooclose:
                Log.i("switchfollowedtooclose ", isChecked + "");
                if(isChecked){
                    causeMap.put("24",true);
                }else{
                    causeMap.put("24",false);
                }
                break;
        }

        saveMap(causeMap);

    }


    View.OnClickListener startDateClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            startDatePickerDialog.setVibrate(true);
            startDatePickerDialog.setYearRange(1985, 2028);
            startDatePickerDialog.setCloseOnSingleTapDay(true);
            startDatePickerDialog.show(getSupportFragmentManager(), "startDate");
        }
    };

    View.OnClickListener endDateClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            datePickerDialog.setVibrate(true);
            datePickerDialog.setYearRange(1985, 2028);
            datePickerDialog.setCloseOnSingleTapDay(true);
            datePickerDialog.show(getSupportFragmentManager(), "endDate");
        }
    };


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

}
