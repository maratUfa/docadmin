/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import com.documentum.com.*;
import com.documentum.fc.client.*;
import com.documentum.fc.common.*;
import play.Play;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * @author m.shaechmetov
 */
public class DFC {
    
    public IDfClientX clientX;
    public IDfClient client;
    IDfSessionManager sessionManager;
    IDfLoginInfo loginInfo;
    public IDfSession session;
    public IDfId nullId;
   
    public DFC() throws DfException{
        
        this.clientX = new DfClientX();
        this.client = DfClient.getLocalClient();
        this.sessionManager = this.client.newSessionManager();
        
        this.loginInfo = this.clientX.getLoginInfo();     
        this.loginInfo.setUser(play.Play.configuration.getProperty("docbase.username"));
        this.loginInfo.setPassword(play.Play.configuration.getProperty("docbase.password"));
        
        this.sessionManager.setIdentity(play.Play.configuration.getProperty("docbase.name"), loginInfo);
        
        this.session = this.sessionManager.getSession(play.Play.configuration.getProperty("docbase.name"));
        
        this.nullId = this.clientX.getId("0000000000000000");
        
        
    }
    
    public IDfCollection query(String query) throws DfException{
        
        IDfQuery dql = new DfQuery();
        dql.setDQL(query);
        IDfCollection col = dql.execute(session, DfQuery.READ_QUERY);
        
        return col;        
    }
    
    public void disconnect(){
        if(this.session.isConnected()){
            this.sessionManager.release(this.session);
        }
    }
    
    public Map<String, String> getUserInfo(String id) throws DfException {
        Map<String, String> map = new TreeMap<String, String>();
        IDfId objId = this.clientX.getId(id);
        
        IDfSysObject org = (IDfSysObject)this.session.getObject(objId);
        
        IDfUser user = (IDfUser)this.session.getObjectByQualification("kc_user where user_name = '" + org.getString("object_name") + "'");
        
        map.put("id", objId.toString());
        map.put("user_name", user.getString("user_name"));
        map.put("surname", user.getString("ka_surname"));
        map.put("firstname", user.getString("ka_firstname"));
        map.put("middlename", user.getString("ka_middlename"));
        map.put("full_name", user.getString("ka_surname") + " " + user.getString("ka_firstname") + " " + user.getString("ka_middlename"));
        map.put("short_name", user.getString("ka_surname") + " " + 
                              user.getString("ka_firstname").substring(0, 1).toUpperCase() + "."+
                              user.getString("ka_middlename").substring(0,1).toUpperCase()+".");
        return map;
    }
    
    public Map<String, String> getUserInfo(IDfId id) throws DfException {
        Map<String, String> map = new TreeMap<String, String>();
        IDfId objId = id;
        
        IDfSysObject org = (IDfSysObject)this.session.getObject(objId);
        
        IDfUser user = (IDfUser)this.session.getObjectByQualification("kc_user where user_name = '" + org.getString("object_name") + "'");
        
        map.put("id", objId.toString());
        map.put("user_name", user.getString("user_name"));
        map.put("surname", user.getString("ka_surname"));
        map.put("firstname", user.getString("ka_firstname"));
        map.put("middlename", user.getString("ka_middlename"));
        map.put("full_name", user.getString("ka_surname") + " " + user.getString("ka_firstname") + " " + user.getString("ka_middlename"));
        map.put("short_name", user.getString("ka_surname") + " " + 
                              user.getString("ka_firstname").substring(0, 1).toUpperCase() + "."+
                              user.getString("ka_middlename").substring(0,1).toUpperCase()+".");
        return map;
    }    
    
    public Map<String, Object> col2Map(Object coll) throws DfException{
        
        IDfTypedObject col = (IDfTypedObject)coll;
        
        Map<String, Object> dict = new TreeMap<String, Object>();
        int attrCount = col.getAttrCount();
        for(int i =0; i<attrCount; i++){
            IDfAttr attr = col.getAttr(i);
            String name = attr.getName();
            int type = attr.getDataType();
            
            /*
            0        Boolean
            1        Integer
            2        String
            3        ID
            4        Time, or date
            5        Double
            6        Undefined                 
             */                

            if(type==0){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<Boolean> rep = new ArrayList<Boolean>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingBoolean(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getBoolean(name));
                }
            }else if(type==1){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<Integer> rep = new ArrayList<Integer>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingInt(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getInt(name));
                }
            }else if(type==2){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<String> rep = new ArrayList<String>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingString(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getString(name));
                }
            }else if(type==3){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<IDfId> rep = new ArrayList<IDfId>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingId(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getId(name));
                }
            }else if(type==4){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<IDfTime> rep = new ArrayList<IDfTime>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingTime(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getTime(name));
                }                
            }else if(type==5){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<Double> rep = new ArrayList<Double>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingDouble(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getDouble(name));
                }
            }else{
                continue;
            }                        
        }
        
        return dict;        
    }
    
     public Map<String, Object> col2Map(Object coll, String... namesArray) throws DfException{        
        
        List<String> names = Arrays.asList(namesArray);                
        IDfTypedObject col = (IDfTypedObject)coll;
        
        Map<String, Object> dict = new TreeMap<String, Object>();
        int attrCount = col.getAttrCount();
        for(int i =0; i<attrCount; i++){
            IDfAttr attr = col.getAttr(i);
            String name = attr.getName();
            
            if(!names.contains(name)){
                continue;
            }
            
            int type = attr.getDataType();
            
            /*
            0        Boolean
            1        Integer
            2        String
            3        ID
            4        Time, or date
            5        Double
            6        Undefined                 
             */                

            if(type==0){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<Boolean> rep = new ArrayList<Boolean>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingBoolean(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getBoolean(name));
                }
            }else if(type==1){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<Integer> rep = new ArrayList<Integer>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingInt(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getInt(name));
                }
            }else if(type==2){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<String> rep = new ArrayList<String>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingString(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getString(name));
                }
            }else if(type==3){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<IDfId> rep = new ArrayList<IDfId>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingId(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getId(name));
                }
            }else if(type==4){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<IDfTime> rep = new ArrayList<IDfTime>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingTime(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getTime(name));
                }                
            }else if(type==5){
                if(attr.isRepeating()){
                    int valueCount = col.getValueCount(name);
                    List<Double> rep = new ArrayList<Double>();
                    for(int ii=0; ii<valueCount; ii++){
                        rep.add(col.getRepeatingDouble(name, ii));
                    }
                    dict.put(name, rep);
                }else{
                    dict.put(name, col.getDouble(name));
                }
            }else{
                continue;
            }                        
        }
        
        return dict;        
    }
     
     public String date2String(Date dt){         
         SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
         return df.format(dt);
     }
    
     public String date2String(IDfTime dt){         
         SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
         return df.format(dt.getDate());
     }
}
