/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;


import play.*;
import play.mvc.*;

import java.util.*;

import common.DFC;
import com.documentum.fc.common.*;
import com.documentum.com.*;
import com.documentum.fc.*;
import com.documentum.fc.client.*;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import java.lang.reflect.Array;
import java.text.DateFormat;
import play.data.validation.*;
import java.text.SimpleDateFormat;
import javax.print.attribute.DocAttributeSet;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
/**
 *
 * @author Diana
 */
public class Documents extends Controller{
    
    
    public static void searchDocument(String documentId, String documentName, String regNumber, String regDate, String creationDate, Integer tab) throws DfException{
               
        flash.put("currentTab", tab);            
        flash.put("documentId",documentId);
        flash.put("documentName",documentName);
        flash.put("regNumber",regNumber);

        flash.put("regDate",regDate);
        flash.put("creationDate",creationDate);
        
        List<String> errors = new ArrayList<String>();
        if(!validation.hasErrors()){
            
            String all = documentId.trim() + 
                    documentName.trim() + 
                    regNumber.trim() + 
                    regDate.trim() + 
                    creationDate.trim();
            
            
            if (all.length() == 0){
                Application.index();
            }
            
            
            DFC dfc = new DFC();
            List<Map<String, Object>> docList = new ArrayList<Map<String, Object>>();
            
            String query = "select r_object_id, object_name, ka_registration_number, ka_registration_date, r_creation_date,";
            query += "owner_name from kc_document where ";

            List<String> whereClause = new ArrayList<String>();
            
            if(documentId.trim().length() > 0){
                whereClause.add("r_object_id = '"+documentId.trim()+"'");
            }
            if(regNumber.trim().length() > 0){
                if(regNumber.trim().contains("%")){
                    whereClause.add("ka_registration_number like '" + regNumber.trim() + "'");
                }else{
                    whereClause.add("ka_registration_number = '" + regNumber.trim() + "'");
                }
                whereClause.add("r_object_type not in ('kc_mission','kc_report')");
            }
            if(documentName.trim().length() > 0){
                if(documentName.trim().contains("%")){
                    whereClause.add("lower(object_name) like '" + documentName.trim().toLowerCase() + "'");
                }else{
                    whereClause.add("lower(object_name) = '" + documentName.trim().toLowerCase() + "'");
                }
            }
            if(regDate.trim().length() > 0){
                whereClause.add("datefloor(day, ka_registrion_date) = DATE('" + regDate.trim() + "', 'dd.mm.yyyy')");
            }
            if(creationDate.trim().length() > 0){
                whereClause.add("datefloor(day, r_creation_date) = DATE('" + creationDate.trim() + "', 'dd.mm.yyyy')");
            }
            
            String where = Joiner.on(" and ").skipNulls().join(whereClause);
            query += where;                        
            //System.out.println(query);
            IDfCollection col = dfc.query(query);
            while(col.next()){                            
                docList.add(dfc.col2Map(col));
            }
            
            col.close();
            
            play.cache.Cache.set("docList", docList, "30mn");            
            
            dfc.disconnect();
            
        }
            Application.index();           
    }
    
    public static void documentView(String documentId) throws DfException{
        
        DFC dfc = new DFC();
        
        IDfId docId = dfc.clientX.getId(documentId);
        IDfDocument document = (IDfDocument)dfc.session.getObject(docId);
        String r_object_id = documentId;
        
        Map<String, Object> docAttributes = dfc.col2Map(document);
        docAttributes.put("r_object_id", document.getObjectId());
        
        try{
            IDfTypedObject filial = (IDfTypedObject)dfc.session.getObject((IDfId)docAttributes.get("ka_filial"));            
            docAttributes.put("ka_filial", dfc.col2Map(filial, "r_object_id", "object_name"));
        }catch(Exception ex){            
            docAttributes.put("ka_filial", null);
        }
        
        
        try{
            IDfTypedObject fileNumber = (IDfTypedObject)dfc.session.getObject((IDfId)docAttributes.get("ka_file_number"));            
            docAttributes.put("ka_file_number", dfc.col2Map(fileNumber, "r_object_id", "ka_number", "ka_description"));
        }catch(Exception ex){
            docAttributes.put("ka_file_number", null);
        }
        
        try{
            IDfTypedObject owner = (IDfTypedObject)dfc.session.getObjectByQualification("kc_user where user_name = '" +  docAttributes.get("owner_name") + "'");
            docAttributes.put("owner_name", dfc.col2Map(owner, "ka_surname", "ka_firstname", "ka_middlename","user_name"));
        }catch(Exception ex){
            docAttributes.put("owner_name", null);
        }
        
        try{
            IDfTypedObject policy = (IDfTypedObject)dfc.session.getObject((IDfId)docAttributes.get("r_policy_id"));
            docAttributes.put("r_policy_id", dfc.col2Map(policy, "r_object_id", "object_name"));
        }catch(Exception ex){
            docAttributes.put("r_policy_id", null);
        }
        
        Map<String,Object> performers = new TreeMap<String, Object>();
        try{
            List<IDfId> col = (List<IDfId>)docAttributes.get("ka_person_performer");
            for(IDfId id: col){
                IDfTypedObject performer = (IDfTypedObject)dfc.session.getObject(id);
                IDfTypedObject user = (IDfTypedObject)dfc.session.getObjectByQualification("kc_user where user_name = '" + performer.getString("object_name")+"'");
                performers.put(performer.getString("object_name"), dfc.col2Map(user, "ka_surname", "ka_firstname", "ka_middlename", "user_name"));
            }
            docAttributes.put("ka_person_performer", performers);
        }catch(Exception ex){
            docAttributes.put("ka_person_performer", null);
        }
        
        docAttributes.put("isCheckedOut", document.isCheckedOut());       
        
        String[] availableDocumets = {"kc_incoming_doc", 
                                        "kc_inform_doc",
                                        "kc_outgoing_doc",
                                        "kc_order_doc",
                                        "kc_exp_doc",
                                        "kc_der_doc",
                                        "kc_inc_doc",
                                        "kc_exp_inc_doc"};
        
        docAttributes.put("available_documents__",  Arrays.asList(availableDocumets));
        
        String[] docTypesWithReports = {"kc_mission", "kc_internal_agree_task","kc_transfer_control_date","kc_grant_request"};
        docAttributes.put("doctypes_with_reports__", Arrays.asList(docTypesWithReports));
        
        List<Map<String, Object>> attachments = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> originals = new ArrayList<Map<String, Object>>();
        /*
        if(document.isVirtualDocument()){
            IDfVirtualDocument vdocument = document.asVirtualDocument("CURRENT", false);
            IDfVirtualDocumentNode root = vdocument.getRootNode();
            for(int i = 0; i<root.getChildCount(); i++){
                IDfVirtualDocumentNode childNode = root.getChild(i);
                IDfSysObject child = childNode.getSelectedObject();
                
                if(!child.getTypeName().equalsIgnoreCase("kc_content")){                    
                    continue;
                }
                
                Map<String, Object> content = new TreeMap<String, Object>();
                content.put("object_name", child.getObjectName());
                content.put("r_object_id", child.getObjectId());
                content.put("r_modify_date", child.getModifyDate());
                content.put("r_modifier", child.getModifier());
                content.put("real_path", child.getPath(0));
                
                int c = child.getInt("ka_type");
                if(c==0){
                    attachments.add(content);                    
                }else{
                    originals.add(content);
                }
            }
        }
        */

        Boolean do_not_include_closed_missions = true;
        Boolean do_not_include_closed_tasks = true;
        
        dfc.disconnect();
        render(r_object_id, docAttributes, do_not_include_closed_missions, do_not_include_closed_tasks, attachments, originals);

    }
    
    public static void documentAttachments(String id) throws DfException{
        
        DFC dfc = new DFC();
        
        List<Map<String, Object>> attachments = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> originals = new ArrayList<Map<String, Object>>();
        
        IDfId docId = dfc.clientX.getId(id);
        IDfDocument document = (IDfDocument)dfc.session.getObject(docId);
        
        if(document.isVirtualDocument()){
            IDfVirtualDocument vdocument = document.asVirtualDocument("CURRENT", false);
            IDfVirtualDocumentNode root = vdocument.getRootNode();
            for(int i=0; i<root.getChildCount(); i++){
                IDfVirtualDocumentNode childNode = root.getChild(i);
                IDfSysObject child = childNode.getSelectedObject();
                
                if(! child.getTypeName().equalsIgnoreCase("kc_content")){
                    continue;
                }
                
                Map<String, Object> content = new TreeMap<String, Object>();
                content.put("object_name", child.getObjectName());
                content.put("r_object_id", child.getObjectId());                
                content.put("r_modify_date", dfc.date2String(child.getModifyDate()));
                content.put("r_modifier", child.getModifier());
                content.put("real_path", child.getPath(0));
                content.put("size", child.getContentSize());
                
                int type = child.getInt("ka_type");
                if(type==0){
                    attachments.add(content);
                }else{
                    originals.add(content);
                }
                
            }
        }
        
        dfc.disconnect();
        //System.out.println(attachments.toString());
        //System.out.println(originals.toString());
        Boolean ajax = request.isAjax();        
        renderTemplate("Documents/documentattachments.html",attachments, originals, ajax);
    }

    

    
    public static void registerDocument(String typeParam, String idParam) throws DfException{
        
        if(request.isAjax()){
            DFC dfc = new DFC();
            IDfId docId = dfc.clientX.getId(idParam);
            IDfDocument doc = (IDfDocument)dfc.session.getObject(docId);
            IDfId filial = doc.getId("ka_filial");
            IDfId reg = null;
            if(! (filial.equals(dfc.nullId) || filial.isNull())){
                IDfCollection col = dfc.query("select r_object_id from kc_registrator where ka_branch = '" + filial.toString() + "'");
                if(col.getState() == IDfCollection.DF_READY_STATE){
                    col.next();
                    reg = col.getId("r_object_id");
                }
                col.close();
            }
            
            int nextNum = 0;
            String template = "";
            
            if(!(reg.equals(dfc.nullId) || reg.isNull())){
                IDfPersistentObject regObject = dfc.session.getObject(reg);
                Map<String, Object> regObjectMap = dfc.col2Map(regObject, "ka_doc_kind", "ka_kind_number", "ka_kind_template");
                List<String> typeList = (List<String>)regObjectMap.get("ka_doc_kind");
                if(typeList.contains(typeParam)){
                    int inx = typeList.indexOf(typeParam);
                    List<Integer> kindNumberList = (List<Integer>)regObjectMap.get("ka_kind_number");
                    int currentRegNum = kindNumberList.get(inx);
                    nextNum = currentRegNum + 1;
                    List<String> kindTemplateList = (List<String>)regObjectMap.get("ka_kind_template");
                    template = kindTemplateList.get(inx);
                    regObject.setRepeatingInt("ka_kind_number", inx, nextNum);
                    regObject.save();                                        
                }
            }
            
            String regNumber = "";
            Date now = new Date();
            if(nextNum > 0){
                IDfId fileId = doc.getId("ka_file_number");
                IDfPersistentObject file = dfc.session.getObject(fileId);
                String fileNumber = file.getString("ka_number");
                if(template.contains("#TYPE_ENUM#")){
                    template = template.replace("#TYPE_ENUM#", "");
                }
                if(template.contains("#FILE_ENUM#")){
                    template = template.replace("#FILE_ENUM#", fileNumber + "/");
                }
                regNumber = template + nextNum;
                
                doc.setString("ka_registration_number", regNumber);
                doc.setString("ka_prev_reg_number", regNumber);
                DfTime time = new DfTime(now);
                doc.setTime("ka_registration_date", time);
                doc.setTime("ka_prev_reg_date", time);
                doc.save();
                        
            }
            Map<String, Object> data = new TreeMap<String, Object>();
            data.put("ka_registration_number", regNumber);            
            data.put("ka_registration_date", dfc.date2String(now));
            
            dfc.disconnect();
            
            renderJSON(data);            
            
        }else{
            badRequest();
        }
    }
    
    public static void documentReports(String idParam) throws DfException{
        
        String query = "select r_object_id, ka_text, ka_is_final, ka_report_type, ka_completion_percent, r_creation_date from kc_report where ";
        query += "ka_mission = '" + idParam + "'";
        
        DFC dfc = new DFC();
        IDfCollection col = dfc.query(query);
        List<Map<String,Object>> reports = new ArrayList<Map<String, Object>>();
        while(col.next()){
            Map<String,Object> report = new TreeMap<String, Object>();
            report.put("r_object_id", col.getId("r_object_id"));
            report.put("ka_text", col.getString("ka_text"));
            report.put("ka_is_final", col.getBoolean("ka_is_final"));
            report.put("ka_completion_percent", col.getInt("ka_completion_percent"));
            report.put("r_creation_date", dfc.date2String(col.getTime("r_creation_date")));
            
            reports.add(report);
        }
        col.close();
        dfc.disconnect();
        Boolean ajax = request.isAjax();
        render(reports, ajax);
        
    }
    
    public static void documentTasks(String idParam) throws DfException{
        
        Boolean doNotIncludeClosedTasks = true;
        if(params.allSimple().keySet().contains("do_not_include_closed_tasks")){
            if(params.allSimple().get("do_not_include_closed_tasks").equalsIgnoreCase("false")){
                doNotIncludeClosedTasks = false;
            }else if(params.allSimple().get("do_not_include_closed_tasks").equalsIgnoreCase("true")){
                doNotIncludeClosedTasks = true;
            }            
        }
        
        DFC dfc = new DFC();
        String query = "select name,event, router_id, task_name, task_subject, item_id, date_sent, read_flag, delete_flag";
        query += " from dmi_queue_item where router_id in ( Select distinct r_workflow_id from dmi_package";
        query += " where  any r_component_id = '" + idParam + "')";
        if(doNotIncludeClosedTasks){
            query += " and delete_flag = 0";
        }
        IDfCollection col = dfc.query(query);
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();
        while(col.next()){            
            tasks.add(dfc.col2Map(col));
        }
        col.close();
        dfc.disconnect();
        Boolean ajax = request.isAjax();
        Map<String, Object> docAttributes = new TreeMap<String, Object>();
        docAttributes.put("r_object_id", idParam);
        render(tasks, ajax, docAttributes);
    }   

    public static void documentMissions(String idParam) throws DfException{
        
        Boolean doNotIncludeClosedMissions = true;
        if(params.allSimple().keySet().contains("do_not_include_closed_missions")){
            if(params.allSimple().get("do_not_include_closed_missions").equalsIgnoreCase("false")){
                doNotIncludeClosedMissions = false;
            }else if(params.allSimple().get("do_not_include_closed_missions").equalsIgnoreCase("true")){
                doNotIncludeClosedMissions = true;
            } 
        }
        
        DFC dfc = new DFC();
        String query = "select r_object_id from kc_mission where ka_document = '" + idParam + "'";
        if(doNotIncludeClosedMissions){
            query += " and ka_state_name_ru not in ('Завершено','Согласовано')";
        }
        //System.out.println(query);
        IDfCollection col = dfc.query(query);
        
        List<Map<String, Object>> missionList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> intAgreeList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> transferDateList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> grantReqList = new ArrayList<Map<String, Object>>();
        
        while(col.next()){
            IDfId objId = col.getId("r_object_id");
            IDfDocument document = (IDfDocument)dfc.session.getObject(objId);
            
            Map<String, Object> mission = new TreeMap<String, Object>();
            if(!(document.getId("ka_owner_name").isNull() || document.getId("ka_owner_name").equals(dfc.nullId))){
                mission.put("author", dfc.getUserInfo(document.getId("ka_owner_name")));
            }
            if(!(document.getId("ka_creator_name").isNull() || document.getId("ka_creator_name").equals(dfc.nullId))){
                mission.put("creator", dfc.getUserInfo(document.getId("ka_creator_name")));
            }
            if(!(document.getRepeatingId("ka_person_performer",0).isNull() || document.getRepeatingId("ka_person_performer",0).equals(dfc.nullId))){
                mission.put("performer", dfc.getUserInfo(document.getRepeatingId("ka_person_performer",0)));
            }
            if(!(document.getId("ka_person_controller").isNull() || document.getId("ka_person_controller").equals(dfc.nullId))){
                mission.put("controller", dfc.getUserInfo(document.getId("ka_person_controller")));
            }
            mission.put("object_id", document.getObjectId());
            mission.put("date", dfc.date2String(document.getTime("r_creation_date")));
            mission.put("on_control", document.getBoolean("ka_on_control"));
            mission.put("state", document.getString("ka_state_name_ru"));
            mission.put("text", document.getString("ka_text"));
            
            if(document.getTypeName().equalsIgnoreCase("kc_mission")){
                missionList.add(mission);
            }else if(document.getTypeName().equalsIgnoreCase("kc_internal_agree_task")){
                intAgreeList.add(mission);
            }else if(document.getTypeName().equalsIgnoreCase("kc_transfer_control_date")){
                transferDateList.add(mission);
            }else if(document.getTypeName().equalsIgnoreCase("kc_grant_request")){
                grantReqList.add(mission);
            }                        
        }
        col.close();
        dfc.disconnect();
        Map<String, Object> docAttributes = new TreeMap<String, Object>();
        docAttributes.put("r_object_id", idParam);
        Boolean ajax = request.isAjax();
        render(missionList, intAgreeList, transferDateList, grantReqList, ajax, docAttributes);
    }
   
    public static void abortWorkflow(String idParam) throws DfException{
        if(request.isAjax()){
            DFC dfc = new DFC();
            IDfId workflowId = dfc.clientX.getId(idParam);
            IDfWorkflow workflow = (IDfWorkflow) dfc.session.getObject(workflowId);
            workflow.abort();
            dfc.disconnect();
            renderText("true");
        }else{
            renderText("false");
        }
    }
    
    public static void cancelCheckout(String idParam) throws DfException{
        if(request.isAjax()){
            DFC dfc = new DFC();
            IDfId docId = dfc.clientX.getId(idParam);
            IDfDocument document = (IDfDocument)dfc.session.getObject(docId);
            if(document.isCheckedOut()){
                document.cancelCheckout();
            }
            dfc.disconnect();
            renderText("true");
        }else{
            renderText("false");
        }
    }   
    
}

