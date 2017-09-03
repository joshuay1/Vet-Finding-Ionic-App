package server.profileservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.Application;
import server.response.BasicResponse;
import server.response.ProfileResponse;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class ProfileController {
   
    private Semaphore semaphore = new Semaphore(0);
    private final Logger logger = LoggerFactory.getLogger(Application.class);
    private final String DEFAULTPICTURE= "https://firebasestorage.googleapis.com/v0/b/vetquoll-c22f9.appspot.com/o/Basic%2Fempty.png?alt=media&token=9463fdee-6966-4d87-9928-d4e62e834a9d";
    //GOOGLE API
    private GeoApiContext geocode = new GeoApiContext.Builder()
    .apiKey("AIzaSyAASyK4NcL0JFu9p3Vm3_alRZNEEv1btyE")
    .build();


    //FOR TESTING PURPOSES ONLY
    @RequestMapping(value = "/profile",method = RequestMethod.GET)
    public ProfileResponse profile(@RequestParam(value="userid") String id) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/"+id);
        logger.info("GET Profile Request from " + id);
        if(ref == null){
            logger.error("ref is null");
        }

        ArrayList<ProfileData> datas = new ArrayList<ProfileData>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String firstname =(String) dataSnapshot.child("firstname").getValue();
                String lastname =(String) dataSnapshot.child("lastname").getValue();
                String userType =(String) dataSnapshot.child("userType").getValue();
                String date = (String) dataSnapshot.child("dob").getValue();
                String streetNumber = (String) dataSnapshot.child("streetnumber").getValue();
                String streetName = (String)dataSnapshot.child("streetname").getValue();
                String suburb = (String)dataSnapshot.child("suburb").getValue();
                String state = (String)dataSnapshot.child("state").getValue();
                String postcode = (String) dataSnapshot.child("postcode").getValue();
                String pictureURL = (String)dataSnapshot.child("pictureURL").getValue();
                String country = (String)dataSnapshot.child("country").getValue();


                ProfileData data = new ProfileData(firstname, lastname, userType,date,
                            streetNumber,streetName,suburb,state,postcode,country,pictureURL);
                logger.info("obtain value from database:"+firstname+","+lastname+","+userType+","+date
                                + streetNumber+","+streetName+","+suburb+","+state+","+postcode+","+ pictureURL);
                datas.add(data);
                semaphore.release();
            }
    
            @Override
            public void onCancelled(DatabaseError databaseError) {
    
            }
        });
        try{
            semaphore.acquire();
        }catch(InterruptedException e){
            logger.error("Semaphore is interrupted during the retreival of data");
        }


        if(datas.size()>0){
            return new ProfileResponse("success", id,datas.get(0));  
        }else{
            logger.info("data not found");
            return new ProfileResponse("error",id,datas.get(0));
        }
    }


    @RequestMapping(value = "/postProfile",method = RequestMethod.POST)
    public BasicResponse postProfile(
        @RequestParam(value="userid") String id,
        @RequestParam(value="firstname") String firstname,
        @RequestParam(value="lastname")String lastname,
        @RequestParam(value="dob")String dob,
        @RequestParam(value="userType") String userType,
        @RequestParam(value="streetnumber")String streetnumber,
        @RequestParam(value="streetname")String streetname,
        @RequestParam(value="postcode")String postcode,
        @RequestParam(value="suburb") String suburb,
        @RequestParam(value="state")String state,
        @RequestParam(value="country")String country)
        {
        
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/"+id);
            //TODO: check token.id = id


            //TODO: String checking for input
            //String checking for each input
            
            String address =  streetnumber + " " +streetname + " , "+ suburb + " , "+state + " "+ postcode+ ","+ country;

            ProfileData data = new ProfileData(firstname, lastname, userType, dob, streetnumber, streetname, suburb, state, postcode,country,DEFAULTPICTURE);
            ref.setValue(data);

            

                
            try {
                GeocodingResult[] results =  GeocodingApi.geocode(geocode,
                        address).await();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String latitude = gson.toJson(results[0].geometry.location.lat);
                String longitude = gson.toJson(results[0].geometry.location.lng);
                logger.debug("google api result: Lat :"+ latitude + ", Long :" + longitude);

                
                    //If theres a lat and long create a geofire file
                DatabaseReference georef;
                GeoFire geofire = null;
                if(userType.equals("User")){
                    georef =  FirebaseDatabase.getInstance().getReference("geofire/User");
                    geofire = new GeoFire(georef);
                }else if(userType.equals("Vet")){
                    georef =  FirebaseDatabase.getInstance().getReference("geofire/Vet");
                    geofire = new GeoFire(georef);
                }
                double lat = Double.parseDouble(latitude);
                double lng = Double.parseDouble(longitude);
                geofire.setLocation(id, new GeoLocation(lat, lng));



            } catch (ApiException e) {
                logger.error("Google API exception");
                e.printStackTrace();
                return new BasicResponse("error",id,"Google API error");
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting result for Google API");
                e.printStackTrace();
                return new BasicResponse("error",id,"Interrupted");
            } catch(IOException e){
                logger.error("IOexception at Google API");
                e.printStackTrace();
                return new BasicResponse("error",id,"IO problem from Google API");
            }

        return new BasicResponse("success",id,null);
    }


    @RequestMapping(value = "/profileUpdate",method = RequestMethod.POST)
    public BasicResponse profileUpdate(
        @RequestParam(value="userid") String id,
        @RequestParam(value="firstname",required = false) String firstname,
        @RequestParam(value="lastname",required = false)String lastname,
        @RequestParam(value="dob",required = false)String dob,
        @RequestParam(value="userType") String userType,
        @RequestParam(value="streetnumber",required = false)String streetnumber,
        @RequestParam(value="streetname",required = false)String streetname,
        @RequestParam(value="postcode",required = false)String postcode,
        @RequestParam(value="suburb",required = false) String suburb,
        @RequestParam(value="state",required = false)String state,
        @RequestParam(value="country",required = false)String country)
        {
            //check token id = user id

            boolean changeaddress = false;
            //check if one of address related stuff is changed
            if(streetnumber != null || streetname != null || suburb!=null || state!=null || postcode != null ||country!=null ){
                logger.info("There is an address change");
                if(streetnumber == null || streetname == null || suburb == null || state== null |postcode == null || country == null){
                    logger.info("There is an address change,but some of them is null");
                    return new BasicResponse("error",id,"Some of the address field is null");
                }
                changeaddress = true;
            }

            //here if all address is changed or no address is changed
            //set database ref

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users/"+id);
            HashMap<String, Object> update = new HashMap<String,Object> ();
            if(firstname!= null){
                logger.info("firstname of "+id+" changed to " + firstname);
                update.put("firstname", firstname);
            }

            if(lastname!= null){
                logger.info("lastname of "+id+" changed to " + lastname);
                update.put("lastname",lastname);
            }

            if(dob != null){
                logger.info("dob of "+id+" changed to " + dob);
                update.put("dob",dob);
            }

            if(streetnumber != null){
                logger.info("streetnumber of "+id+" changed to " + streetnumber);
                update.put("streetnumber",streetnumber);
            }

            if(streetname!= null){
                logger.info("streetname of "+id+" changed to " + streetname);
                update.put("streetname",streetname);
            }

            if(suburb != null){
                logger.info("suburb of "+id+" changed to " + suburb);
                update.put("suburb",suburb);
            }

            if(state != null){
                logger.info("state of "+id+" changed to " + state);
                update.put("state",state);
            }

            if(postcode != null){
                logger.info("postcode of "+id+" changed to " + postcode);
                update.put("postcode",postcode);
            }

            if(country != null){
                logger.info("country of "+id+" changed to " + country);
                update.put("country",country);
            }

            if(changeaddress){
                try {
                    String address =  streetnumber + " " +streetname + " , "+ suburb + " , "+state + " "+ postcode+ ","+ country;
                    GeocodingResult[] results =  GeocodingApi.geocode(geocode,
                            address).await();
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String latitude = gson.toJson(results[0].geometry.location.lat);
                    String longitude = gson.toJson(results[0].geometry.location.lng);
                    logger.debug("google api result: Lat :"+ latitude + ", Long :" + longitude);
    
                    
                        //If theres a lat and long create a geofire file
                    DatabaseReference georef;
                    GeoFire geofire = null;
                    if(userType.equals("User")){
                        georef =  FirebaseDatabase.getInstance().getReference("geofire/User");
                        geofire = new GeoFire(georef);
                    }else if(userType.equals("Vet")){
                        georef =  FirebaseDatabase.getInstance().getReference("geofire/Vet");
                        geofire = new GeoFire(georef);
                    }
                    double lat = Double.parseDouble(latitude);
                    double lng = Double.parseDouble(longitude);
                    geofire.setLocation(id, new GeoLocation(lat, lng));
    
    
    
                } catch (ApiException e) {
                    logger.error("Google API exception");
                    e.printStackTrace();
                    return new BasicResponse("error",id,"Google API error");
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting result for Google API");
                    e.printStackTrace();
                    return new BasicResponse("error",id,"Interrupted");
                } catch(IOException e){
                    logger.error("IOexception at Google API");
                    e.printStackTrace();
                    return new BasicResponse("error",id,"IO problem from Google API");
                }
            }




            ref.updateChildren(update);

            return new BasicResponse("success",id,null);
        }

}