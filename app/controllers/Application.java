package controllers;

import com.google.common.base.Joiner;
import play.*;
import play.mvc.*;

import java.util.*;

import common.DFC;
import com.documentum.fc.common.*;
import com.documentum.com.*;
import com.documentum.fc.*;
import com.documentum.fc.client.*;

//import models.*;

public class Application extends Controller {
    
    public static void index() throws DfException{
        
        List<Map<String,Object>> docList;
        List<Map<String,Object>> userList;
        
        String currentTab = flash.get("currentTab");        
        
        docList = (List<Map<String,Object>>)play.cache.Cache.get("docList");
        play.cache.Cache.delete("docList");
        
        userList = (List<Map<String,Object>>)play.cache.Cache.get("userList");
        play.cache.Cache.delete("userList");
        
        render(currentTab, docList, userList);                
    }

    public static void updateAttribute(String typeParam, String idParam) throws DfException{

        if(request.isAjax()){
            DFC dfc = new DFC();

            Map<String, String> args = new TreeMap<String, String>();
            args = request.params.allSimple();
            args.remove("typeParam");
            args.remove("idParam");
            args.remove("body");

            String queryAttrInfo = "select distinct attr_name, domain_type, domain_length from dmi_dd_attr_info ";
            queryAttrInfo += "where type_name = '" + typeParam + "' and attr_name in ('";
            queryAttrInfo += Joiner.on("','").join(args.keySet());
            queryAttrInfo += "')";

            Map<String, Integer> attrInfo = new TreeMap<String, Integer>();
            IDfCollection col = dfc.query(queryAttrInfo);
            while(col.next()){
                attrInfo.put(col.getString("attr_name"), col.getInt("domain_type"));
            }
            col.close();

            String query = "update " + typeParam + " object ";
            List<String> values = new ArrayList<String>();
            String selectQuery = "select ";
            List<String> selectValues = new ArrayList<String>();
            selectValues.add("r_object_id");

            if(typeParam.equalsIgnoreCase("kc_content")){
                selectValues.add("r_modifier");
                selectValues.add("r_modify_date");
            }
            if(typeParam.equalsIgnoreCase("dmr_content")){
                String querydmr = "select r_object_id from dmr_content dc where any dc.parent_id = '" + idParam + "'";
                IDfCollection coldmr = dfc.query(querydmr);
                IDfId dmrId = null;
                while(coldmr.next()){
                    dmrId = coldmr.getId("r_object_id");
                }
                coldmr.close();
                if(!dmrId.isNull()){
                    idParam = dmrId.toString();
                }else{
                    error("dmr_id not found, check input parameters");
                }

            }

            for(Map.Entry<String,String> map : params.allSimple().entrySet()){
                String key = map.getKey();
                String value = map.getValue();
                if(attrInfo.containsKey(key)){
                    /*
                    0        Boolean
                    1        Integer
                    2        String
                    3        ID
                    4        Time, or date
                    5        Double
                    6        Undefined
                    */
                    Integer type = attrInfo.get(key);

                    List<Object> falseValues = new ArrayList<Object>();
                    falseValues.add(0);
                    falseValues.add("0");
                    falseValues.add(false);
                    falseValues.add("False");
                    falseValues.add("false");

                    if(type == IDfAttr.DM_BOOLEAN){
                        if(falseValues.contains(value)){
                            values.add("set " + key + " = False");
                        }else{
                            values.add("set " + key + " = True");
                        }
                    }
                    if(type == IDfAttr.DM_INTEGER){
                        values.add("set " + key + " = " + value);
                    }
                    if(type == IDfAttr.DM_STRING){
                        values.add("set " + key + " = '" + value + "'");
                    }
                    if(type == IDfAttr.DM_ID){
                        values.add("set " + key + " = '" + value + "'");
                    }
                    if(type == IDfAttr.DM_TIME){
                        values.add("set " + key + " = DATE('" + value + "','dd.mm.yyyy hh:mi:ss')");
                    }
                    if(type == IDfAttr.DM_DOUBLE){
                        values.add("set " + key + " = " + value);
                    }
                    selectValues.add(key);
                }

            }

            query += Joiner.on(" ").join(values);
            query += " where r_object_id = '" + idParam + "'";
            selectQuery += Joiner.on(" ,").join(selectValues);
            selectQuery += " from " + typeParam;
            selectQuery += " where r_object_id = '" + idParam + "'";
            //System.out.println(query);
            IDfCollection colq = dfc.query(query);
            if(colq.getState() == IDfCollection.DF_READY_STATE){
                colq.next();
                int updated = colq.getValueAt(0).asInteger();
                if(updated == 0){
                    System.out.println("not updated " + params.allSimple().toString());
                }
                colq.close();
            }

            Map<String, Object> updatedData = null;

            IDfCollection cols = dfc.query(selectQuery);
            if(cols.getState() == IDfCollection.DF_READY_STATE){
                cols.next();
                updatedData = dfc.col2Map(cols);
            }
            cols.close();
            dfc.disconnect();

            renderJSON(updatedData);
        }else{
            badRequest();
        }
        badRequest();
    }
    

    

}