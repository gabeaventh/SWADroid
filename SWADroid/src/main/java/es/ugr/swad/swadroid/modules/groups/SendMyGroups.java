/*
 *  This file is part of SWADroid.
 *
 *  Copyright (C) 2010 Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 *
 *  SWADroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SWADroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SWADroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.ugr.swad.swadroid.modules.groups;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import es.ugr.swad.swadroid.Constants;
import es.ugr.swad.swadroid.R;
import es.ugr.swad.swadroid.model.Group;
import es.ugr.swad.swadroid.model.Model;
import es.ugr.swad.swadroid.modules.login.Login;
import es.ugr.swad.swadroid.modules.Module;
import es.ugr.swad.swadroid.webservices.SOAPClient;

/**
 * Module to enroll into groups.
 * It makes use of the web service sendMyGroups (see https://openswad.org/ws/#sendMyGroups)
 * It needs as extra data:
 * - (long) courseCode course code . It indicates the course to which the groups belong
 * - (string) myGroups: String that contains group codes separated with comma
 * It returns as extra data:
 * - (int) success :0 - it was impossible to satisfy all enrollment. Therefore it was not made any changes. The enrollment remains like before the request.
 * other than 0 -if all the requested changes were possible and are made. It that case the groups in database will be also updated * 					!= 0 -
 *
 * @author Helena Rodriguez Gijon <hrgijon@gmail.com>
 */


public class SendMyGroups extends Module {
    /**
     * Course code
     */
    private long courseCode = -1;
    /**
     * String that contains group codes separated with comma
     */
    private String myGroups = null;

    /**
     * Groups tag name for Logcat
     */
    private static final String TAG = Constants.APP_TAG + "Send My Groups";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        courseCode = getIntent().getLongExtra("courseCode", -1);
        myGroups = getIntent().getStringExtra("myGroups");
        if (courseCode == -1 || myGroups == null) {
            Log.i(TAG, "Missing arguments");
            finish();
        }

        setMETHOD_NAME("sendMyGroups");
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        try {
            runConnection();
        } catch (Exception e) {
            String errorMsg = getString(R.string.errorServerResponseMsg);
            error(errorMsg, e, true);
        }
    }

    @Override
    protected void runConnection() {
        super.runConnection();
        if (!isConnected) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void requestService() throws Exception {
    	
    	createRequest(SOAPClient.CLIENT_TYPE);
        addParam("wsKey", Login.getLoggedUser().getWsKey());
        addParam("courseCode", (int) courseCode);
        addParam("myGroups", myGroups);
        sendRequest(Group.class, false);
        
        if (result != null) {
            ArrayList<?> res = new ArrayList<Object>((Vector<?>) result);
            SoapPrimitive soapP = (SoapPrimitive) res.get(0);

            /*
              Indicates if the enrollments are done or not
              0 - if the enrollments are not done
              other than 0 - if they are correctly done
             */
            int success = Integer.parseInt(soapP.toString());
            if (success != 0) {
                List<Model> groupsSWAD = new ArrayList<>();

                SoapObject soapO = (SoapObject) res.get(2);
                int propertyCount = soapO.getPropertyCount();

                for (int i = 0; i < propertyCount; ++i) {
                    SoapObject pii = (SoapObject) soapO.getProperty(i);
                    long id = Long.parseLong(pii.getProperty("groupCode").toString());
                    String groupName = pii.getProperty("groupName").toString();
                    long groupTypeCode = Integer.parseInt(pii.getProperty("groupTypeCode").toString());
                    int maxStudents = Integer.parseInt(pii.getProperty("maxStudents").toString());
                    int open = Integer.parseInt(pii.getProperty("open").toString());
                    int numStudents = Integer.parseInt(pii.getProperty("numStudents").toString());
                    int fileZones = Integer.parseInt(pii.getProperty("fileZones").toString());
                    int member = Integer.parseInt(pii.getProperty("member").toString());

                    Group g = new Group(id, groupName, groupTypeCode, maxStudents, open, numStudents, fileZones, member);
                    groupsSWAD.add(g);

                    if (isDebuggable) {
                        Log.i(TAG, g.toString());
                    }
                }
                for (Model aGroupsSWAD : groupsSWAD) {
                    Group g = (Group) aGroupsSWAD;
                    //boolean isAdded = dbHelper.insertGroup(g,Global.getSelectedCourseCode());
                    //if(!isAdded){
                    if (!dbHelper.updateGroup(g.getId(), courseCode, g)) {
                        dbHelper.insertGroup(g, courseCode);
                    }
                }
            }
            Intent resultIntent = new Intent();
            resultIntent.putExtra("success", success);
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED);
        }
    }

    @Override
    protected void connect() {
        startConnection();
    }

    @Override
    protected void postConnect() {
        finish();
    }

    @Override
    protected void onError() {

    }

}
