package server.bookingservice;



public class BookingData{
    private String uid;
    private String vetid;
    private String year;
    private String month;
    private String day;
    private String status;
    private String time;
    
    //ADD PAYMENT

    public BookingData(String uid, String vetid, String year, String month, String day, String time, String status){
        this.uid = uid;
        this.vetid = vetid;
        this.year = year;
        this.month = month;
        this.day = day;
        this.time = time;
        this.status = status;
    }

    public String getUserId(){
        return this.uid;
    }

    public String getVetId(){
        return this.vetid;
    }

    public String getYear(){
        return this.year;
    }
    
    public String getMonth(){
        return this.month;
    }

    public String getDay(){
        return this.day;
    }

    public String getTime(){
        return this.time;
    }

    public String getStatus(){
        return this.status;
    }
}