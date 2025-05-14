package com.couchbase.client.cache.test;

import com.couchbase.client.java.json.JsonObject;

import java.io.Serializable;

public class User implements Serializable {
  static int uid=0;
  public String id;
  public String firstname;
  public String lastname;
  public User(){}

  public User(String id, String firstname, String lastbame){
    this.id = id;
    this.firstname = firstname;
    this.lastname = lastbame;
  }
  public User(String firstname, String lastbame){
    this.id = String.valueOf(uid++);
    this.firstname = firstname;
    this.lastname = lastbame;
  }

  public JsonObject toJson(){
    JsonObject jo = JsonObject.jo();
    jo.put( this.getClass().getSimpleName(),
      JsonObject.jo()
        .put("firstname",firstname.toString())
        .put("lastname",lastname));
    return jo;
  }

  @Override
  public String toString(){
    return toJson().toString();
  }
}
