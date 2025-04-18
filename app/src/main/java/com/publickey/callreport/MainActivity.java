package com.publickey.callreport;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView mInfoTextView;
        mInfoTextView = findViewById(R.id.textViewScroll);
        //mInfoTextView.setMovementMethod(new ScrollingMovementMethod());

        mInfoTextView.setText("");

        ExternalValue.MA = this;

        String[] mFiles = fileList();
        for(String sFile: mFiles) {
            if (sFile.equals(ExternalValue.FileLogName)) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            openFileInput(sFile)));
                    StringBuilder LogData = new StringBuilder(0);
                    String str;
                    while ((str = br.readLine()) != null) {
                        LogData.append(str).append(System.lineSeparator());
                    }
                    br.close();
                    deleteFile(sFile);

                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                            openFileOutput(ExternalValue.FileLogName + "1", Context.MODE_PRIVATE)));
                    bw.write(LogData.toString());
                    bw.close();

                } catch (IOException e) {
                    ExternalValue.AddLog("RenameFile/Error", e.toString());
                }
                break;
            }
        }

        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(),0);
        }
        catch(PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
            pInfo = null;
        }
        if (pInfo != null) ExternalValue.version = pInfo.versionName;

        ExternalValue.AddLog("Version",ExternalValue.version);
        ExternalValue.AddLogOnScreen("Версия -" + ExternalValue.version);


        SwitchCompat SwitchCalls = findViewById(R.id.switch1);
        SwitchCalls.setOnCheckedChangeListener((CompoundButton v,boolean isChecked) ->
        {
            ExternalValue.AddLog("SwitchCalls", ""+isChecked);
            ExternalValue.OutCalls = isChecked;
        });

        SwitchCompat SwitchLogs = findViewById(R.id.switch2);
        SwitchLogs.setOnCheckedChangeListener((CompoundButton v,boolean isChecked) ->
        {
            ExternalValue.AddLog("SwitchLogs", ""+isChecked);
            ExternalValue.CallsLogs = isChecked;
        });


        @SuppressLint("HardwareIds") String id= Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        ExternalValue.hash = ExternalValue.sha256(id+ExternalValue.Salt);
        ExternalValue.id = id;

        ExternalValue.AddLog("SDK"," "+Build.VERSION.SDK_INT);

        ExternalValue.SendRegisterMessageToServer("");

        /*
        Toast TM = Toast.makeText(this,"Attention !!! This App gathering information about your calls.",Toast.LENGTH_LONG);
        TM.show();
        */

        Button mButton = findViewById(R.id.button);
        mButton.setOnClickListener((View v) ->
        {
            mInfoTextView.setText("");
            if (!ExternalValue.Registered) ExternalValue.SendRegisterMessageToServer(ExternalValue.SendSMS);
            else ExternalValue.CheckPermissions();

        });


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((View v) ->  {
                ExternalValue.FabCurrentClick++;
                if ((ExternalValue.FabActivateClick-ExternalValue.FabCurrentClick) > 0) {
                    //for (int i = 0; i < 50; i++)
                        ExternalValue.AddLogOnScreen("До отправки лога осталось " + (ExternalValue.FabActivateClick - ExternalValue.FabCurrentClick) + " нажатий");
                }
                else {
                    ExternalValue.FabCurrentClick = 0;
                    mInfoTextView.setText("");
                    ExternalValue.AddLogOnScreen("Лог отправляется");
                    ExternalValue.SendLogToServer();
                }

        });

    }

    @Override
    public void onBackPressed() {
        // Переводим приложение в фоновый режим
        moveTaskToBack(true);
    }

}

