package com.publickey.callreport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.telephony.TelephonyManager;


public class PhoneStateChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (!ExternalValue.Registered || !ExternalValue.isPermissions) return;

        //boolean isInComeCall = false;
        ExternalValue.AddLog("Intent",intent.getAction());
        if(!intent.getAction().equals("android.intent.action.PHONE_STATE")) return;

        String sNumber;
        String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        ExternalValue.AddLog("phoneState", phoneState);
        sNumber = intent.getStringExtra("incoming_number");
        if (sNumber == null) return;

        ExternalValue.AddLog("sNumber", sNumber);

        if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) ExternalValue.IncomeCall = true;
        if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE))
        {
            // Если исходящие не нужны и этот звонок исходящий, то ничего не далаем
            if ((!ExternalValue.OutCalls) && (!ExternalValue.IncomeCall)) sNumber = "";
            if (!sNumber.equals(""))
            {
                sNumber = (ExternalValue.IncomeCall ?  "->":"<-") + sNumber;
                ExternalValue.Numbers.add(sNumber);
                ExternalValue.SendNumbersToServer();
                ExternalValue.AddNumberOnScreen( sNumber);
            }
            ExternalValue.IncomeCall = false;
        }

    }
}

