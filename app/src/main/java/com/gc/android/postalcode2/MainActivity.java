package com.gc.android.postalcode2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.design.widget.TabItem;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    TabLayout tabLayout;
    ViewPager viewPager;
    PageAdapter pageAdapter;
    TabItem tabCity;
    TabItem tabPostalCode;

    EditText inputText;
    Button queryButton;
    TextView responseView;
    ProgressBar progressBar;

    public String type = "city";
    public String curr_country_code = null;

    Context context = this;

    private CoordinateTrace coordinateTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tablayout);
        tabCity = findViewById(R.id.tabCity);
        tabPostalCode = findViewById(R.id.tabPostalCode);

        pageAdapter = new PageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        responseView = (TextView) findViewById(R.id.responseView);

        inputText = (EditText) findViewById(R.id.inputText);
        queryButton = (Button) findViewById(R.id.queryButton);
        //instance of the class that manage the current coordinates of the system
        coordinateTrace = new CoordinateTrace(context);

        //create_spinner country
        Locale[] locale = Locale.getAvailableLocales();
        ArrayList<String> countries = new ArrayList<String>();
        String country;
        for (Locale loc : locale) {
            country = loc.getDisplayCountry();
            if (country.length() > 0 && !countries.contains(country)) {
                countries.add(country);
            }
        }
        Collections.sort(countries, String.CASE_INSENSITIVE_ORDER);
        String all_country = "All Country";
        countries.add(0, all_country);
        final Spinner spinnerCountry = (Spinner) findViewById(R.id.inputCountry);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, countries);
        spinnerCountry.setAdapter(adapter);

        spinnerCountry.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String selectedCountry = (String) spinnerCountry.getSelectedItem();
                curr_country_code = getCountryCode(selectedCountry);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });


        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RetrieveFeedTask().execute();
            }
        });


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                responseView.setText("");
                inputText.setText("");
                if (tab.getPosition() == 1) {
                    type = "zipCode";
                    inputText.setHint("Enter postal code");
                } else if (tab.getPosition() == 2) {
                    type = "myPosition";
                    queryButton.performClick();
                } else { // first position
                    type = "city";
                    inputText.setHint("Enter city name");
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public String getCountryCode(String countryName) {

        // Get all country codes in a string array.
        String[] isoCountryCodes = Locale.getISOCountries();
        Map<String, String> countryMap = new HashMap<>();

        // Iterate through all country codes:
        for (String code : isoCountryCodes) {
            // Create a locale using each country code
            Locale locale = new Locale("", code);
            // Get country name for each code.
            String name = locale.getDisplayCountry();
            // Map all country names and codes in key - value pairs.
            countryMap.put(name, code);
        }
        // Get the country code for the given country name using the map.
        // a list of countries to give to user to choose from.
        String countryCode = countryMap.get(countryName); // "IT" for Italy.
        return countryCode;
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, String> {

        private Exception exception;

        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            responseView.setText("");
        }

        protected String doInBackground(Void... urls) {

            GeoApi geoApi = new GeoApi();
            String searchString = inputText.getText().toString();
            String strUrl = null;

            // Do some validation here

            switch (type) {
                case "city":
                    strUrl = geoApi.getZipCodeFromCityAllCountry(searchString, curr_country_code);
                    break;

                case "zipCode":
                    strUrl = geoApi.getCountryFromZipAllCountry(searchString, curr_country_code);
                    break;

                case "myPosition":

                    Double latitude = coordinateTrace.getLatitude();
                    Double longitude = coordinateTrace.getLongitude();
                    strUrl = geoApi.getInfoByCoordinate(latitude, longitude);
                    break;

            }

            try {

                URL url = new URL(strUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }

        }

        protected void onPostExecute(String response) {
            if (response == null) {
                response = "THERE WAS AN ERROR";
            }
            progressBar.setVisibility(View.GONE);
            //responseView.setText(response);
            // TODO: check this.exception
            // TODO: do something with the feed

            try {

                JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
                String responseStr = "";
                String mapLink = "";
                String linkTag = "";
                JSONArray arrResults;
                switch (type) {
                    case "city":
                        arrResults = object.getJSONArray("postalCodes");
                        if (arrResults.length() > 0) {
                            responseStr = "<ol>";
                            Integer counter = 1;
                            for (int i = 0; i < arrResults.length(); i++) {
                                JSONObject postal_code = arrResults.getJSONObject(i);
                                String countryCode = postal_code.getString("countryCode");
                                String postalCode = postal_code.getString("postalCode");
                                String placeName = postal_code.getString("placeName");

                                String lat = postal_code.getString("lat");
                                String lng = postal_code.getString("lng");
                                mapLink = "https://www.google.com/maps/place/";
                                mapLink += lat + "," + lng;
                                linkTag = "<a href=\"" + mapLink + "\" target=\"_blank\">Go to the maps</a>";
                                responseStr += "<p>" + counter + ". " + countryCode + " - " + placeName + " - " + postalCode + "<br>&nbsp;&nbsp;&nbsp;&nbsp;" + linkTag + "</p>";
                                counter++;
                            }
                            responseStr += "</ol>";
                        } else {
                            responseStr = "<p> Results not found</p>";
                        }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            responseView.setText(Html.fromHtml(responseStr, Html.FROM_HTML_MODE_COMPACT, null, new HtmlTagHandler()));
                        } else {
                            responseView.setText(Html.fromHtml(responseStr, null, new HtmlTagHandler()));
                        }

                        responseView.setMovementMethod(LinkMovementMethod.getInstance());
                        break;

                    case "zipCode":
                        arrResults = object.getJSONArray("postalCodes");
                        if (arrResults.length() > 0) {
                            responseStr = "<ol>";
                            Integer counter = 1;
                            for (int i = 0; i < arrResults.length(); i++) {
                                JSONObject postal_code = arrResults.getJSONObject(i);
                                String countryCode = postal_code.getString("countryCode");
                                String postalCode = postal_code.getString("postalCode");
                                String placeName = postal_code.getString("placeName");

                                String lat = postal_code.getString("lat");
                                String lng = postal_code.getString("lng");
                                mapLink = "https://www.google.com/maps/place/";
                                mapLink += lat + "," + lng;
                                linkTag = "<a href=\"" + mapLink + "\" target=\"_blank\">Go to the maps</a>";
                                responseStr += "<p>" + counter + ". " + countryCode + " - " + placeName + " - " + postalCode + "<br>&nbsp;&nbsp;&nbsp;&nbsp;" + linkTag + "</p>";
                                counter++;
                            }
                            responseStr += "</ol>";
                        } else {
                            responseStr = "<p> Results not found</p>";
                        }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            responseView.setText(Html.fromHtml(responseStr, Html.FROM_HTML_MODE_COMPACT, null, new HtmlTagHandler()));
                        } else {
                            responseView.setText(Html.fromHtml(responseStr, null, new HtmlTagHandler()));
                        }

                        responseView.setMovementMethod(LinkMovementMethod.getInstance());
                        break;
                    case "myPosition":

                        JSONObject address = object.getJSONObject("address");
                        if( address.length() > 0) {
                            String countryCode = address.getString("countryCode");
                            String postalCode = address.getString("postalcode");
                            String locality = address.getString("locality");
                            String street = address.getString("street");
                            String houseNumber = address.getString("houseNumber");
                            street = (houseNumber != "") ? street + "," + houseNumber : street;
                            String lat = address.getString("lat");
                            String lng = address.getString("lng");
                            mapLink = "https://www.google.com/maps/place/";
                            mapLink += lat + "," + lng;
                            linkTag = "<a href=\"" + mapLink + "\" target=\"_blank\">Go to the maps</a>";
                            responseStr = "<ol>";
                            responseStr += "<p>1. " + countryCode + " - " + locality  + " - Street: " + street+ " - " + postalCode + "<br>&nbsp;&nbsp;&nbsp;&nbsp;" + linkTag + "</p>";
                            responseStr += "</ol>";
                            inputText.setText(locality);
                        }else {
                            responseStr = "<p> Results not found</p>";
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            responseView.setText(Html.fromHtml(responseStr, Html.FROM_HTML_MODE_COMPACT, null, new HtmlTagHandler()));
                        } else {
                            responseView.setText(Html.fromHtml(responseStr, null, new HtmlTagHandler()));
                        }

                        responseView.setMovementMethod(LinkMovementMethod.getInstance());
                        break;
                }

            } catch (JSONException e) {
                e.printStackTrace();
                responseView.setText("Not have results");
            }

        }

        public String getMyPosition() {
            String StrReturn = "";
                    coordinateTrace = new CoordinateTrace(MainActivity.this);
            if( coordinateTrace.isGetLocation() ){
                double latitude = coordinateTrace.getLatitude();
                double longitude = coordinateTrace.getLongitude();

                StrReturn = String.valueOf(latitude)+","+String.valueOf(longitude);
            }else{
                coordinateTrace.showSettingsAlert();
            }

            return StrReturn;

       }
    }

}
