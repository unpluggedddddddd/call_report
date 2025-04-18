package com.publickey.callreport;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class ExternalValue {


    public static String version = "";
    public static String id = "";
    public static String hash = "";
    public static final List<String> Numbers = new ArrayList<>();
    public static final String sms_prefix= "P0U1B2K3E4Y5C6A7L8L9";
    public static boolean Registered = false;
    public static boolean isPermissions = false;
    public static MainActivity MA;
    public static boolean OutCalls = true;
    public static boolean CallsLogs = false;
    public static boolean IncomeCall = false;
    public static final String ServerOk = "ok";
    public static String ConnectionError = "";
    public static final int ConnectionTimeout = 3000;
    public static boolean ConnectionWorking = false;
    public static final int ConnectionDelay = 1000;
    public static final int ConnectionMaxDelay = 60*1000;
    public static String ConnectionLastSentNumber = "";
    public static int ConnectionAttempt = 0;
    public static int FabCurrentClick = 0;
    public static final int FabActivateClick = 5;
    public static final String FileLogName = "log";
    public static final List<String> ExistingNetworks = new ArrayList<>();

    public static final String OperationTypeRegisterMessage = "RegisterMessage";
    public static final String OperationTypeNumber = "Number";
    public static final String OperationTypeLog = "Log";

    public static final String SendSMS = "SendSMS";
    public static final String Salt = "cucumber2021";

    public static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte a: hash) {
                String hex = Integer.toHexString(0xff & a);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    public static String asciiToHex(char[] chars,int len)
    {
        StringBuilder hex = new StringBuilder();
        for (int i=0;i<len;i++)
        {
            hex.append(Integer.toHexString(chars[i]));
        }
        return hex.toString();
    }

    public static void AddLog(String sTag,String sInfo) {
        try {
                //AddLogOnScreen(new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date())+"-"+sTag + ": " + sInfo);

                // отрываем поток для записи
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    MA.getApplicationContext().openFileOutput(FileLogName,  Context.MODE_APPEND | Context.MODE_PRIVATE)));
                    bw.write(new SimpleDateFormat("yyyy:MM:dd-HH:mm:ss.SSS", Locale.getDefault()).format(new Date())+"-"+sTag + ": " + sInfo+System.lineSeparator());
                bw.close();
                Log.i("CRL-"+sTag,sInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void AddLogOnScreen(String Info)
    {
        if (Info != null)
        {
            TextView mInfoTextView;
            mInfoTextView = ExternalValue.MA.findViewById(R.id.textViewScroll);
            MA.runOnUiThread(() -> {
                mInfoTextView.append(System.lineSeparator() + Info);
                ScrollView mScrollView=MA.findViewById(R.id.ViewScroll);
                mScrollView.post(() -> mScrollView.fullScroll(View.FOCUS_DOWN));
            });
        }
    }

    public static void AddNumberOnScreen(String Info)
    {
        if (Info != null) {
            if (ExternalValue.CallsLogs) {
                AddLogOnScreen( Info + " - " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            }
        }
    }

    public static String ConvertNumber(String sNumber)
    {
        char[] cNumber = new char[16];
        for (int i = 0; i < Math.min(sNumber.length(), 16); i++) {
            int k = (sNumber.charAt(i) + ExternalValue.id.charAt(i));
            if (k > 255) k -= 255;
            cNumber[i] = (char) k;
        }
        return ExternalValue.asciiToHex(cNumber, sNumber.length());
    }

    public static boolean isOnline()
    {
        boolean bRet;

        Context context=ExternalValue.MA.getApplicationContext();
        ConnectivityManager CM =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network ActiveNetwork = CM.getActiveNetwork();
        if (ActiveNetwork == null)
        {
            AddLog("Network","No active network");
            Network[] ActiveNetworks=CM.getAllNetworks();
            for(Network curNetwork:ActiveNetworks)
            {
                AddLog("AllNetworks","Network-"+curNetwork.toString());
                NetworkCapabilities ANC = CM.getNetworkCapabilities(curNetwork);
                if (!ExistingNetworks.contains(curNetwork.toString())) {
                    ExistingNetworks.add(curNetwork.toString());
                    AddLog("AllNetworks", "NetworkCapabilities-" + ANC.toString());
                }
            }
            return false;
        }
        AddLog("Network","Active network-"+ActiveNetwork.toString());
        NetworkCapabilities ANC = CM.getNetworkCapabilities(ActiveNetwork);
        bRet = ANC != null &&
                (ANC.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || ANC.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        if (bRet) AddLog("NetworkCapabilities",ANC.toString());

        return bRet;
    }

    public static void SendLogToServer() {
        new Thread(()->{
            try {
                AddLog("SendLog","Start");
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        MA.getApplicationContext().openFileInput(FileLogName)));
                StringBuilder LogData = new StringBuilder(0);
                String str;
                while ((str = br.readLine()) != null) {
                    LogData.append(str).append(System.lineSeparator());
                }
                br.close();

                String[] mFiles = MA.getApplicationContext().fileList();
                for (String sFile : mFiles) {
                    if (sFile.equals(ExternalValue.FileLogName + "1")) {
                        LogData.append(System.lineSeparator());
                        LogData.append(System.lineSeparator());
                        LogData.append("Previous log");
                        LogData.append(System.lineSeparator());
                        LogData.append(System.lineSeparator());

                        br = new BufferedReader(new InputStreamReader(
                                MA.getApplicationContext().openFileInput(FileLogName + "1")));
                        while ((str = br.readLine()) != null) {
                            LogData.append(str).append(System.lineSeparator());
                        }
                        br.close();
                        break;
                    }
                }
                str = sha256(LogData.toString());
                String sInfo = "d=crl&h=" + str + "&i=" + ExternalValue.hash;
                SendMessageToServer(OperationTypeLog, sInfo, LogData.toString());
                SetTimerForLog();
                AddLog("SendLog","End");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void SendNumberToServer(String sNumber)
    {
        ConnectionLastSentNumber = sNumber;
        AddLog("PS-Number", sNumber);
        String sHexNumber = ConvertNumber(sNumber);
        String sInfo = "v=" + ExternalValue.version + "&p=" + sHexNumber + "&i=" + ExternalValue.hash;
        SendMessageToServer(OperationTypeNumber,sInfo,sNumber);
        SetTimerForNumber(sNumber);
    }

    public static void SendNumbersToServer()
    {
        if (Numbers.size()>0) {
            if (ConnectionLastSentNumber.equals("")) SendNumberToServer(Numbers.get(0));
            SetTimerForSendNumbers();
        }
    }

    public static void SetTimerForSendNumbers()
    {
        SetTimer(new TimerTask(){
            @Override
            public void run() {
                AddLog("Timer",this.toString());
                if (!ConnectionLastSentNumber.equals("")) SetTimerForSendNumbers();
                else SendNumbersToServer();
            }},"TimerForSendNumbers");
    }

    public static void SendRegisterMessageToServer(String Info)
    {
        String sInfo = "v=" + ExternalValue.version + "&r="+ ExternalValue.hash;
        SendMessageToServer(OperationTypeRegisterMessage,sInfo,Info);
        SetTimerForRegistration(Info);
    }

    public static void SetTimerForRegistration(String Info)
    {
        SetTimer(new TimerTask(){
            @Override
            public void run() {
                AddLog("Timer",this.toString());
                if (ExternalValue.ConnectionWorking) SetTimerForRegistration(Info);
                else {
                    if (!ExternalValue.ConnectionError.isEmpty()) {
                        ShowConnectionState();
                        if (ConnectionAttempt < 3) SendRegisterMessageToServer(Info);
                        else {
                            AddLogOnScreen("Регистрация не проверена.Попробуйте выполнить проверку позже. Для этого нажмите кнопку <Повторить>");
                            Button mButton = ExternalValue.MA.findViewById(R.id.button);
                            MA.runOnUiThread(() -> {
                                mButton.setText("Повторить");
                                mButton.setVisibility(View.VISIBLE);
                            });
                        }

                    }
                }
            }},"TimerForRegistration");
    }

    public static void SetTimer(TimerTask TT,String TimerName)
    {
        AddLog("Set timer",TimerName);
        Timer mTimer;
        mTimer = new Timer();
        mTimer.schedule(TT, GetDelay(TT.toString()));
    }

    public static void ShowConnectionState()
    {
        boolean isOnline = isOnline();
        if (ConnectionAttempt == 1) {
            if (isOnline) {
                //Toast.makeText(MA.getApplicationContext(),"Пытаемся соединиться. Подождите несколько секунд.",Toast.LENGTH_SHORT);
                AddLogOnScreen("Пытаемся соединиться. Подождите несколько секунд.");
            }
            else AddLogOnScreen("Нет соединение с сетью интернет. Включите интернет.");
        }
    }

    public static void SetTimerForLog()
    {
        SetTimer(new TimerTask(){
            @Override
            public void run() {
                AddLog("Timer",this.toString());
                if (ExternalValue.ConnectionWorking) SetTimerForLog();
                else {
                    if (!ExternalValue.ConnectionError.isEmpty()) {
                        ShowConnectionState();
                        if (ConnectionAttempt < 3) SendLogToServer();
                        else AddLogOnScreen("Лог не отправлен. Повторите потытку позже.");
                    }
                }
            }},"TimerForLog");
    }

    public static void SetTimerForNumber(String PN)
    {
        SetTimer(new TimerTask(){
            @Override
            public void run() {
                AddLog("Timer",this.toString());
                if (ExternalValue.ConnectionWorking) SetTimerForNumber(PN);
                else {
                    if (!ExternalValue.ConnectionError.isEmpty()) SendNumberToServer(PN);
                }
            }},"TimerForNumber");
    }

    public static int CheckPermission(String sPerm)
    {

        int result = ContextCompat.checkSelfPermission(MA,sPerm );
        if (result == PackageManager.PERMISSION_GRANTED)
            ExternalValue.AddLog(sPerm,"GRANTED");
        else
        {
            ExternalValue.AddLog(sPerm,"NOT GRANTED");
            int requestCode = 0;
            MA.requestPermissions(new String[]{sPerm}, requestCode);
            result = ContextCompat.checkSelfPermission(MA,sPerm);

        }
        return result;
    }

    public static void CheckPermissions()
    {
        if (!ExternalValue.Registered) return;

        Button mButton = MA.findViewById(R.id.button);
        int CountPermission = 0;
        PackageInfo pInfo;
        try {
            pInfo = MA.getPackageManager().getPackageInfo(MA.getPackageName(), PackageManager.GET_PERMISSIONS);
        }
        catch(PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
            AddLog("PackageManager/Error",e.toString());
            pInfo = null;
        }

        List<String> mPermission = new ArrayList<>();
        if (pInfo != null) {
            if (pInfo.requestedPermissions != null) {
                for (String sPerm : pInfo.requestedPermissions) {
                        mPermission.add(sPerm);
                        if (ExternalValue.CheckPermission(sPerm) == PackageManager.PERMISSION_GRANTED)
                            CountPermission++;
                        else
                        {
                            ExternalValue.AddLogOnScreen("Не установлено разрешение " + sPerm);
                            break;
                        }

                }
            }
        }

        if (ExternalValue.isPermissions=(CountPermission==mPermission.size())) {
            ExternalValue.AddLogOnScreen("Начало работы - " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            MA.runOnUiThread(() -> mButton.setVisibility(View.GONE));
        }
        else
        {
            ExternalValue.AddLogOnScreen("Приложение работать не будет. Для корректной работы установите все требуемые разрешения и нажмите кнопку <Повторить>");
            MA.runOnUiThread(() -> {
                mButton.setText("Повторить");
                mButton.setVisibility(View.VISIBLE);
            });

        }
    }

    public static void SendMessageToServer(String Type,String sInfo,String PostData)
    {
        ExternalValue.ConnectionWorking = true;
        ExternalValue.ConnectionError = "";
        boolean isOnline = isOnline();
        AddLog("Send","isOnline-"+isOnline);
        ConnectionAttempt++;
        if (isOnline) {
            new Thread(() -> {
            try {
                AddLog("Send","Start Thread.ConnectionAttempt-"+ConnectionAttempt);
                String sSign = ExternalValue.sha256(sInfo + ExternalValue.id);
                String TextUrl = "" + sInfo + "&s=" + sSign;
                ExternalValue.AddLog("Send", "TextUrl-"+TextUrl);
                StringBuilder Answer = new StringBuilder(0);
                String PN = "";
                URL url = null;
                try {
                    url = new URL(TextUrl);
                    AddLog("Send","url");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                try {
                    assert url != null;
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(ExternalValue.ConnectionTimeout);
                    AddLog("Send","Wait connection timeout");
                    if (Type.equals(OperationTypeLog)) {
                        if (PostData != null) {
                            connection.setDoOutput(true);
                            // Don't use a cached copy.
                            connection.setUseCaches(false);

                            connection.setRequestProperty("Content-Type", "text/html; charset=UTF-8");
                            connection.setRequestProperty("Connection", "Keep-Alive");
                            connection.setRequestProperty("Accept", "text/html");
                            //connection.setRequestProperty("Content-Length", String.valueOf(PostData.length()));
                            AddLog("Content-Length", String.valueOf(PostData.length()));
                            OutputStream out = connection.getOutputStream();
                            out.write(PostData.getBytes());
                            out.flush();
                            out.close();


                        }
                    }
                    else PN = PostData;
                    try {
                        InputStream in=connection.getInputStream();
                        Scanner s = new Scanner(in).useDelimiter("\\A");
                        String result = s.hasNext() ? s.next() : "";
                        for (char a : result.toCharArray()) {
                            if (a > 13) Answer.append(a);
                        }
                        in.close();
                        ConnectionAttempt = 0;
                        AddLog("Send","Got answer-"+Answer.toString());
                    } catch (SocketTimeoutException e) {
                        ExternalValue.ConnectionError = e.toString();
                        AddLog("Send/Error","SocketTimeoutException");
                        AddLog("Send/Error",e.toString());
                    }

                } catch (IOException e) {
                    ExternalValue.ConnectionError = e.toString();
                    AddLog("Send/Error","IOException");
                    AddLog("Send/Error",e.toString());
                }
                ProcessAnswer(Type,Answer.toString(),PN);
                AddLog("Send","Exit");
            } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        else ExternalValue.ConnectionError = "Network isn't available";
        ExternalValue.ConnectionWorking = false;
    }


    public static long GetDelay(String Name)
    {
        long delay = Math.min(ConnectionDelay * (ConnectionAttempt+1),ConnectionMaxDelay);
        AddLog("Set delay",Name+"-"+delay);
        return delay;
    }

    public  static void ProcessAnswer(String Type,String Answer,String Info) {
        try {
            AddLog("ProcessAnswer",Type);
            switch (Type) {
                case OperationTypeRegisterMessage: {
                    if (!Answer.equals("")) {
                        ExternalValue.Registered = Answer.contains(ExternalValue.ServerOk);
                        if (ExternalValue.Registered) {

                            AddLogOnScreen("Ваш номер зарегистрирован.");
                            MA.runOnUiThread(() -> MA.findViewById(R.id.fab).setVisibility(View.VISIBLE));
                            CheckPermissions();

                        } else {
                            ExternalValue.AddLogOnScreen("Приложение не зарегистрировано. Для регистрации нажмите кнопку регистрация и отправьте СМС. После получения ответного СМС нажмите кнопку регистрация еще раз.");
                            Button mButton = ExternalValue.MA.findViewById(R.id.button);

                            MA.runOnUiThread(() -> {
                                mButton.setText("Регистрация");
                                mButton.setVisibility(View.VISIBLE);
                            });
                            if (Info.equals(SendSMS)) {
                                try {
                                    String toSms = "";
                                    String messageText = ExternalValue.sms_prefix + ":" + ExternalValue.id + ":";
                                    String Signature = ExternalValue.sha256(messageText + Salt);
                                    messageText += Signature;
                                    ExternalValue.AddLog("SMS", "Length-" + messageText.length());
                                    Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse(toSms));
                                    sms.putExtra("sms_body", messageText);
                                    MA.startActivity(sms);
                                } catch (Exception e) {
                                    ExternalValue.AddLogOnScreen("Не установлена Сим карта. Приложение работать не будет");
                                    ExternalValue.AddLogOnScreen(e.toString());
                                    MA.runOnUiThread(() -> mButton.setVisibility(View.VISIBLE));
                                }
                            }
                        }
                    }
                }
                break;
                case OperationTypeNumber: {
                    if (Answer.contains(ExternalValue.ServerOk)) {
                        int i = Numbers.indexOf(Info);
                        if (i > -1) Numbers.remove(i);
                        AddNumberOnScreen(Info + " " + Answer);
                        ConnectionLastSentNumber = "";
                        AddLog("TimerForNumber", "got answer");
                        AddLog("TimerForNumber", "Numbers queue-" + Numbers.size());
                    }

                }
                break;
                case OperationTypeLog: {
                    if (Answer.contains(ExternalValue.ServerOk))
                        AddLogOnScreen("Лог отправлен разработчику");
                    else AddLogOnScreen("Лог не отправлен. Повторите попытку позже.");
                }
            }
        }
        catch (Exception e){
            AddLog("ProcessAnswer/Error",e.toString());
        }

    }
}
