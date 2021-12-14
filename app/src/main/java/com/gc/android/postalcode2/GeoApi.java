package com.gc.android.postalcode2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class GeoApi {

    static final String API_URL = "http://api.geonames.org/";
    static final String language = Locale.getDefault().getLanguage().toLowerCase();
    static final String USERNAME = "?username=insert_username&lang="+language;
    String TYPE;


    public GeoApi() {
    }

    public String getZipCodeFromCityAllCountry(String city, String country_code){
        TYPE = "postalCodeSearchJSON";

        ArrayList<String> arr_values = new ArrayList<String>();
        arr_values.add("formatted=true");
        arr_values.add("placename="+city);
        //arr_values.add("maxRows=10");
        if(country_code != null){
            arr_values.add("country="+country_code);
        }
        String attribute_concatenation = "&"+String.join("&",arr_values);
        return API_URL+TYPE+USERNAME+attribute_concatenation;

    }

    public String getCountryFromZipAllCountry(String city, String country_code){
        TYPE = "postalCodeSearchJSON";

        ArrayList<String> arr_values = new ArrayList<String>();
        arr_values.add("formatted=true");
        arr_values.add("placename="+city);
        if(country_code != null){
            arr_values.add("country="+country_code);
        }
        String attribute_concatenation = "&"+String.join("&",arr_values);
        return API_URL+TYPE+USERNAME+attribute_concatenation;
    }

    public String getInfoByCoordinate(Double latitude, Double  longitude){

        TYPE = "addressJSON";
        ArrayList<String> arr_values = new ArrayList<String>();
        arr_values.add("formatted=true");
        arr_values.add("lat="+String.valueOf(latitude));
        arr_values.add("lng="+String.valueOf(longitude));

        String attribute_concatenation = "&"+String.join("&",arr_values);
        return API_URL+TYPE+USERNAME+attribute_concatenation;
    }




}
