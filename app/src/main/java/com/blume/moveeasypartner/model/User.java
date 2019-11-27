package com.blume.moveeasypartner.model;

public class User {
    public String uid, email, uname, reg_no, vehicleType;
    public int phone;

    public User(String userid, String email, String uname, int phone, String reg_no, String vehicle){
        this.uid = userid;
        this.uname = uname;
        this.email = email;
        this.phone = phone;
        this.reg_no = reg_no;
        this.vehicleType = vehicle;

    }
    public String get_uid(){
        return uid;
    }
    public void set_uid(String uid){
        this.uid = uid;
    }
    public String get_email(){
        return email;
    }
    public void set_email(String email){
        this.email = email;
    }
    public String get_uname(){
        return uname;
    }
    public void set_uname(String uname){
        this.uname = uname;
    }
    public int get_phone(){
        return phone;
    }
    public void set_phone(int phone){
        this.phone = phone;
    }
    public String get_reg_no(){
        return reg_no;
    }
    public void set_reg_no(String reg_no){
        this.reg_no = reg_no;
    }
    public String get_vehicleType(){
        return vehicleType;
    }
    public void set_vehicleType(String vehicleType){
        this.vehicleType = vehicleType;
    }
}
