package com.cocoppang.braindooodledownloader;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class UninstallListener extends BroadcastReceiver
{
    MainActivity _main;

    public void setMain( MainActivity mainActivity )
    {
        _main = mainActivity;
    }

    @Override
    public void onReceive( Context context, Intent intent )
    {
        Log.d( "[DOWNLOADER]", "DETECTED APP UNINSTALLED" );
        _main.downloadApk();
    }
}
