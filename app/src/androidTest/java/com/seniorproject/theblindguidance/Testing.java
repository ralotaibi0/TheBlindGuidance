package com.seniorproject.theblindguidance;

import android.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.rule.ActivityTestRule;
import android.widget.Button;

import static org.junit.Assert.*;

public class Testing {
    ActivityTestRule myActivity;

    @Before
    public void setUp() throws Exception {
        myActivity = new ActivityTestRule(MainActivity.class);
    }

    @Test
    public void TestONE() { // Permissions Testing
        assertEquals(myActivity.getActivity().checkCallingPermission(Manifest.permission.BLUETOOTH)
                , PackageManager.PERMISSION_GRANTED);
        assertEquals(myActivity.getActivity().checkCallingPermission(Manifest.permission.BLUETOOTH_ADMIN)
                , PackageManager.PERMISSION_GRANTED);
        assertEquals(myActivity.getActivity().checkCallingPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                , PackageManager.PERMISSION_GRANTED);
        assertEquals(myActivity.getActivity().checkCallingPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                , PackageManager.PERMISSION_GRANTED);
        assertEquals(myActivity.getActivity().checkCallingPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                , PackageManager.PERMISSION_GRANTED);
        assertEquals(myActivity.getActivity().checkCallingPermission(Manifest.permission.RECORD_AUDIO)
                , PackageManager.PERMISSION_GRANTED);
    }

    @Test
    public void TestTwo() { // Bluetooth Connection Testing
        Button b = myActivity.getActivity().findViewById(R.id.Connect);
        assertEquals("disconnect", b.getText());
    }


    @After
    public void tearDown() throws Exception {
        myActivity = null;
    }
}




