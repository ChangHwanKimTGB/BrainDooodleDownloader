package com.cocoppang.braindooodledownloader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity
{
    String _dooodleAppPackageName = BuildConfig.TARGET_APP_DOMAIN;

    TextView _percentLabel = null;
    Button _downloadButton = null;

    File _downloadedApkFile = null;
    boolean _downloadCompleted = false;

    //UninstallListener _uninstallListener = null;
    long _downloadID;
    DownloadManager _downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _percentLabel = findViewById( R.id.percent_label );
        _downloadButton = findViewById( R.id.download_button );

//        _uninstallListener = new UninstallListener();
//        _uninstallListener.setMain( this );

        _downloadedApkFile =
            new File(
                Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS ),
                BuildConfig.DOWNLOADED_APP_NAME );
        _downloadedApkFile.setWritable(true, false);
    }

    public void OnButtonDownload( View v )
    {
        // 기존 앱 설치 여부 확인.
        Intent installedAppIntent = getPackageManager().getLaunchIntentForPackage( _dooodleAppPackageName );
        if( installedAppIntent != null )
        {
            uninstallApk();
            return;
        }

        // 이미 다운로드 받은 apk가 있으면 바로 apk설치 화면으로 넘어간다.
        if( _downloadCompleted )
        {
            installApk();
            return;
        }

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M )
        {
            if( !checkPermission() )
            {
                return;
            }
        }

        // APK 다운로드 실행.
        //downloadApkByManager();
        downloadApk();
    }

    public void downloadApkByManager()
    {
        Uri downloadUri = Uri.parse( "https://kr.object.ncloudstorage.com/cocoppang-cdn-bucket/BrainContest_NurshingHome.apk" );
        DownloadManager.Request request = new DownloadManager.Request( downloadUri );
        request.setTitle( "전국두뇌자랑 요양원 설치 파일 다운로드" );
        request.setDestinationUri( Uri.fromFile( _downloadedApkFile ) );

        _downloadManager = ( DownloadManager )getSystemService( DOWNLOAD_SERVICE );
        _downloadID = _downloadManager.enqueue( request );

        IntentFilter intentFilter = new IntentFilter( DownloadManager.ACTION_DOWNLOAD_COMPLETE );
        registerReceiver( downloadReceiver, intentFilter );
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById( _downloadID );

            Cursor downloadResult = _downloadManager.query( query );
            if( downloadResult.moveToFirst() )
            {
                int columnIndex = downloadResult.getColumnIndex( DownloadManager.COLUMN_STATUS );
                int status = downloadResult.getInt( columnIndex );
                if( status == DownloadManager.STATUS_SUCCESSFUL )
                {
                    try
                    {
                        Log.d( "[DOWNLOADER]", "onReceive" );
                        installApk();
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    public void downloadApk()
    {
        _downloadButton.setVisibility( View.INVISIBLE );
        FileOutputStream fileToWrite = null;
        try
        {
            fileToWrite = new FileOutputStream( _downloadedApkFile );
            //fileToWrite = openFileOutput( _downloadedApkPath, Context.MODE_PRIVATE );
        }
        catch( Exception e )
        {
            _percentLabel.setText( e.getMessage() );
            Log.e( "[DOWNLOADER]", "FAILED OPEN FILE TO WRITE" );
            Log.e( "[DOWNLOADER]", e.getMessage() );
            return;
        }

        HttpDownloader downloader = new HttpDownloader();
        downloader.Initialize(
                fileToWrite, _percentLabel,
                ()->
                {
                    // 다운로드 완료.
                    _downloadCompleted = true;
                    installApk();
                } );
        downloader.execute();
    }

    void uninstallApk()
    {
        Uri packageURI = Uri.parse( "package:" + _dooodleAppPackageName );
        Intent uninstallIntent = new Intent( Intent.ACTION_DELETE, packageURI );
        startActivity( uninstallIntent );
    }

    void installApk()
    {
        Uri fileUri = null;
        Intent intent = null;
        Context context = getBaseContext();
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N )
        {
            fileUri =
                FileProvider.getUriForFile(
                    context,
                    context.getApplicationContext().getPackageName() + ".fileprovider",
                    _downloadedApkFile );
                    //apkFile );

            intent = new Intent( Intent.ACTION_VIEW );
            intent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );
        }
        else
        {
            _downloadedApkFile.setReadable( true, false );
            fileUri = Uri.fromFile( _downloadedApkFile );

            intent = new Intent( Intent.ACTION_INSTALL_PACKAGE );
            intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        }
        intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        intent.setDataAndType( fileUri, "application/vnd.android.package-archive" );
        startActivity( intent );

        showInstallButton();
    }

    void showInstallButton()
    {
        _downloadButton = findViewById( R.id.download_button );
        _downloadButton.setVisibility( View.VISIBLE );
        _downloadButton.setText( "앱 설치하기" );
    }

    boolean checkPermission()
    {
        Activity activity = ( Activity )this;
        if( ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED
                ||
                ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED
        )
        {
            String[] permission =
                    new String[]
                            {
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            };
            ActivityCompat.requestPermissions(
                    activity,
                    permission,
                    MainActivity.class.hashCode() & 0x000000ff );
            return false;
        }
        return true;
    }
}