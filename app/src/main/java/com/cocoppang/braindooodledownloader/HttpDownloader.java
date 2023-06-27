package com.cocoppang.braindooodledownloader;

import android.os.AsyncTask;
import android.widget.TextView;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpDownloader extends AsyncTask<Void, Void, Void>
{
    FileOutputStream _fileToWrite;
    TextView _percentLabel;
    float _progressRate;

    Runnable _onComplete;

    public void Initialize(
            FileOutputStream fileToWrite,
            TextView textView,
            Runnable onComplete )
    {
        _fileToWrite = fileToWrite;
        _percentLabel = textView;
        _onComplete = onComplete;
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        requestAPK();
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... Params)
    {
        _percentLabel.setText( String.format( "%.2f%%", _progressRate ) );
    }

    @Override
    protected void onPostExecute( Void s ){ }

    public void requestAPK()
    {
        // HttpURLConnection 참조 변수.
        HttpURLConnection urlConnection = null;
        try
        {
            URL url = new URL( BuildConfig.DOWNLOAD_URL );
            urlConnection = (HttpURLConnection) url.openConnection();

            // 응답 코드.
            //Log.d( "[DOWNLOADER]", String.valueOf( urlConnection.getResponseCode() ) );

            // 연결 요청 확인.
            // 실패 시 null을 리턴하고 메서드를 종료.
            if ( urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK )
            {
                Log.e( "[DOWNLOADER]", "HTTP STATUS CODE - " + String.valueOf( urlConnection.getResponseCode() ) );
                return;
            }

            InputStream inputStream = new BufferedInputStream( urlConnection.getInputStream() );

            int bufferLength = 0;
            int downloadedSize = 0;
            byte[] buffer = new byte[1048576];
            float fileSize = ( float )urlConnection.getContentLength();
            //Log.d( "[DOWNLOADER]", String.valueOf( fileSize ) );
            do
            {
                bufferLength = inputStream.read( buffer );
                if( bufferLength > 0 )
                {
                    _fileToWrite.write( buffer, 0, bufferLength );
                    downloadedSize += bufferLength;

                    _progressRate = ( float )downloadedSize / ( float )fileSize * 100f;
                    publishProgress();
                }
            }
            while( bufferLength > 0 );

            _fileToWrite.close();
            inputStream.close();

            _onComplete.run();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            Log.e( "[DOWNLOADER]", e.getMessage() );
        }
        finally
        {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
    }
}