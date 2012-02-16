package controllers;

import org.apache.commons.collections.bidimap.TreeBidiMap;
import play.*;
import play.mvc.*;

import java.util.*;

import common.DFC;
import com.documentum.fc.common.*;
import com.documentum.com.*;
import com.documentum.fc.*;
import com.documentum.fc.client.*;


import com.google.common.base.Joiner;
import play.data.validation.*;
import sun.text.normalizer.UnicodeSetIterator;

/**
 *
 * @author Diana
 */
public class Users extends Controller {
    
    public static void searchUser(String userSurname, 
                                    String userFirstname,
                                    String userMiddlename,
                                    String userName,
                                    String userId,
                                    String orgPosId,
                                    Integer tab) throws DfException
    {
    

            flash.put("currentTab", tab);
            
            flash.put("userSurname",userSurname);
            flash.put("userFirstname",userFirstname);
            flash.put("userMiddlename",userMiddlename);
            flash.put("userName",userName);
            flash.put("userId",userId);
            flash.put("orgPosId",orgPosId);
            
            if(!validation.hasErrors()){
                String all = userSurname.trim() +
                        userFirstname.trim() + 
                        userMiddlename.trim() + 
                        userName.trim() +
                        userId.trim() + 
                        orgPosId.trim();
                
                if(all.length() == 0){
                    Application.index();
                }
                
                DFC dfc = new DFC();
                                
                String query = "select r_object_id, user_name, ka_surname, ka_firstname, ka_middlename, user_state from kc_user where ";
                List<String> whereClause = new ArrayList<String>();                
                if(userId.trim().length()>0){
                    whereClause.add("r_object_id = '" + userId.trim() + "'");
                }
                if(userName.trim().length()>0){
                    whereClause.add("lower(user_name) like '" + userName.trim().toLowerCase() + "%'");
                }
                if(userSurname.trim().length()>0){
                    whereClause.add("lower(ka_surname) like '" + userSurname.trim().toLowerCase() + "%'");
                }
                if(userFirstname.trim().length()>0){
                    whereClause.add("lower(ka_firstname) like '" + userFirstname.trim().toLowerCase() + "%'");
                }
                if(userMiddlename.trim().length()>0){
                    whereClause.add("lower(ka_middlename) like '" + userMiddlename.trim().toLowerCase() + "%'");
                }
                if(orgPosId.trim().length()>0){
                    whereClause.add("lower(user_name) in (select ka_user_name from kc_org_position where r_object_id = '"+orgPosId.trim()+"')");
                }
                
                String where = Joiner.on(" and ").skipNulls().join(whereClause);
                query += where;

                List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();

                IDfCollection col = dfc.query(query);
                while(col.next()){
                    userList.add(dfc.col2Map(col));
                }

                col.close();
                play.cache.Cache.set("userList", userList);

                dfc.disconnect();

            }
            
            Application.index();
            
    }
    
    public static void userView(String userId) throws DfException{
        
        DFC dfc = new DFC();
        IDfId uid = dfc.clientX.getId(userId);
        IDfUser user = (IDfUser)dfc.session.getObject(uid);
        
        Map<String, Object> userData = dfc.col2Map(user, 
                "r_object_id",
                "user_name",
                "ka_full_name",
                "ka_firstname",
                "ka_surname",
                "ka_middlename",
                "user_address",
                "user_state",
                "user_source",
                "user_password",
                "user_group_name",
                "default_folder",
                "acl_name",
                "workflow_disabled",
                "user_ldap_dn",
                "user_login_domain",
                "ka_substitute_group");
        userData.put("r_object_id", user.getObjectId());
        
        IDfPersistentObject orgPos = dfc.session.getObjectByQualification("kc_org_position where ka_user_name = '" + user.getUserName() + "'");
        Map<String, Object> orgposData = dfc.col2Map(orgPos, 
                "r_object_id",
                "object_name",
                "ka_position_id",
                "ka_folder_id",
                "ka_substitute_group");
        
        IDfPersistentObject position = dfc.session.getObject(orgPos.getId("ka_position_id"));
        Map<String, Object> positionData = dfc.col2Map(position, "r_object_id", "object_name");
        
        List<Map<String, Object>> folderPath = new ArrayList<Map<String, Object>>();
        IDfId folderId = orgPos.getId("ka_folder_id");
        
        while(true){
            IDfPersistentObject folder = dfc.session.getObject(folderId);
            Map<String, Object> folderData = dfc.col2Map(folder, "r_object_id", "object_name", "ka_folder_id", "ka_is_branch");
            folderPath.add(folderData);
            
            if(folder.getBoolean("ka_is_branch")){
                break;
            }else{
                folderId = folder.getId("ka_folder_id");
            }
        }

        dfc.disconnect();
       
        render(userData, orgposData, positionData, folderPath);
    }
    
    public static void userTasks(String userid) throws DfException{
        DFC dfc = new DFC();
        IDfId uid = dfc.clientX.getId(userid);
        IDfUser user = (IDfUser)dfc.session.getObject(uid);


        String query = "select event, router_id, task_name, task_subject, item_id, read_flag, delete_flag, date_sent" +
                        " from dmi_queue_item where name = '" + user.getUserName() + "'";

        List<Map<String, Object>> userTasks = new ArrayList<Map<String, Object>>();        

        IDfCollection col = dfc.query(query);
        while(col.next()){
            userTasks.add(dfc.col2Map(col));
        }
        col.close();

        Boolean ajax = request.isAjax();

        dfc.disconnect();
        render(userTasks, ajax);
    }

    public static void userInbox(String userId) throws DfException{
        badRequest();
    }
    
    public static void userGroups(String userId) throws DfException{
        
        DFC dfc = new DFC();
        IDfId uid = dfc.clientX.getId(userId);
        IDfUser user = (IDfUser)dfc.session.getObject(uid);
        
        String query = "select group_name from dm_group where any i_all_users_names = '" + user.getUserName() + "'";
        
        List<String> groups = new ArrayList<String>();
        IDfCollection col = dfc.query(query);
        while(col.next()){
            groups.add(col.getString("group_name"));
        }
        col.close();
        
        dfc.disconnect();

        Boolean ajax = request.isAjax();
        render(groups, ajax, userId);
        
    }
    
    public static void addUserToGroup(String userId, String groupName) throws DfException{

        DFC dfc = new DFC();
        IDfId uid = dfc.clientX.getId(userId);
        IDfUser user = (IDfUser)dfc.session.getObject(uid);
        
        IDfGroup group = (IDfGroup)dfc.session.getObjectByQualification("dm_group where group_name = '" + groupName + "'");
        Boolean result = group.addUser(user.getUserName());
        group.save();        

        dfc.disconnect();

        renderJSON(result);
    }
    
    public static void removeUserFromGroup(String userId, String groupName) throws  DfException{
        
        DFC dfc = new DFC();
        IDfId uid = dfc.clientX.getId(userId);
        IDfUser user = (IDfUser)dfc.session.getObject(uid);
        
        IDfGroup group = (IDfGroup)dfc.session.getObjectByQualification("dm_group where group_name = '" + groupName + "'");
        boolean result = group.removeUser(user.getUserName());
        group.save();
        
        dfc.disconnect();

        renderJSON(result);
    }
    
    public static void getGroupList(String filter) throws DfException{

        DFC dfc = new DFC();
        List<String> groups = new ArrayList<String>();
        IDfCollection col = dfc.query("select group_name from dm_group where lower(group_name) like '"
                + filter.toLowerCase() + "%' order by group_name ENABLE (RETURN_TOP 10)");
        while(col.next()){
            groups.add(col.getString("group_name"));
        }
        col.close();
        
        dfc.disconnect();

        renderJSON(groups);
    }
    
    public static void getPositionList(String filter) throws DfException{
        
        DFC dfc = new DFC();
        List<Map<String, Object>> positions = new ArrayList<Map<String, Object>>();
        IDfCollection col = dfc.query("select r_object_id, object_name from kc_position where lower(object_name) like '" +
                filter.toLowerCase() + "%' order by object_name ENABLE (RETURN_TOP 10)");
        while(col.next()){
            positions.add(dfc.col2Map(col));
        }
        col.close();
        
        dfc.disconnect();

        renderJSON(positions);
    }

    
}
