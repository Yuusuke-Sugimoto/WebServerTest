package jp.ddo.kingdragon.webserver;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * AndroidをWebサーバ化するためのテストアプリ
 * @author 杉本祐介
 */
public class WebServerTestActivity extends Activity {
    // 定数の宣言
    /**
     * ポート番号
     */
    private static final int PORT_NUMBER = 8080;

    // 変数の宣言
    /**
     * アプリが実行中かどうか
     */
    private boolean isRunning;
    /**
     * 保存用フォルダ
     */
    private File baseDir;
    /**
     * サーバソケット
     */
    private ServerSocket mServerSocket;
    /**
     * スリープ時にWi-Fiを維持するために使用
     */
    private WakeLock mWakeLock;
    /**
     * スリープ時にWi-Fiを維持するために使用
     */
    private WifiLock mWifiLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        isRunning = true;

        // 保存用フォルダの作成
        baseDir = new File(Environment.getExternalStorageDirectory(), "WebServerTest");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Toast.makeText(WebServerTestActivity.this, R.string.error_make_directory_failed, Toast.LENGTH_SHORT).show();

            finish();
        }

        /**
         * 端末のIPアドレスを取得
         * 参考:[Android] Wi-fi接続時のIP Address（アドレス）を取得 - adakoda
         *      http://www.adakoda.com/adakoda/2009/03/android-wi-fiip-address.html
         */
        WifiManager mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        int ipAddress = mWifiInfo.getIpAddress();
        String strIpAddress = (ipAddress & 0xff) + "."
                              + ((ipAddress >> 8)  & 0xff) + "."
                              + ((ipAddress >> 16) & 0xff) + "."
                              + ((ipAddress >> 24) & 0xff);
        TextView mTextView = (TextView)findViewById(R.id.local_ip_address);
        mTextView.setText(strIpAddress + ":" + WebServerTestActivity.PORT_NUMBER);

        /**
         * スリープ時にWi-Fiを維持する
         * 参考:android - How do I keep Wifi from disconnecting when phone is asleep? - Stack Overflow
         *      http://stackoverflow.com/questions/3871824/how-do-i-keep-wifi-from-disconnecting-when-phone-is-asleep
         */
        PowerManager mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "MyWifiLock");

        try {
            mServerSocket = new ServerSocket(WebServerTestActivity.PORT_NUMBER);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isRunning) {
                        try {
                            final Socket clientSocket = mServerSocket.accept();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    BufferedReader br = null;
                                    try {
                                        br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                                        String line = br.readLine();
                                        if (line != null) {
                                            // HTTPリクエストが空でない場合
                                            Request mRequest = new Request(line);
                                            File mFile = new File(baseDir, mRequest.getPath());
                                            if (mFile.exists()) {
                                                // 要求されたファイルが存在する場合
                                                /**
                                                 * MimeTypeを判別
                                                 * 参考:もぷろぐ: Android で MIME Type 判別
                                                 *      http://ac-mopp.blogspot.jp/2011/12/android-mime-type.html
                                                 */
                                                String[] splittedFileName = mFile.getName().split("\\.");
                                                String ext = splittedFileName[splittedFileName.length - 1];
                                                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);

                                                OutputStream os = clientSocket.getOutputStream();
                                                PrintWriter pw = new PrintWriter(os);
                                                pw.print("HTTP/1.1 200 OK\r\n");
                                                pw.print("Content-Type: " + mimeType + "; charset=UTF-8\r\n");
                                                pw.print("Content-Length: " + mFile.length() + "\r\n");
                                                pw.print("Connection: Keep-Alive\r\n\r\n");
                                                pw.flush();
                                                FileInputStream fis = new FileInputStream(mFile);
                                                int byteData;
                                                while ((byteData = fis.read()) != -1) {
                                                    os.write(byteData);
                                                }
                                                os.flush();
                                                fis.close();
                                            }
                                            else {
                                                // 要求されたファイルが存在しない場合
                                                // 404エラーを返す
                                                String content = "<html><head><title>404 Not Found</title><body>404 Not Found</body></html>";
                                                PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());
                                                pw.print("HTTP/1.1 404 Not Found\r\n");
                                                pw.print("Content-Type: text/html; charset=UTF-8\r\n");
                                                pw.print("Content-Length: " + content.getBytes("UTF-8").length + "\r\n");
                                                pw.print("Connection: Keep-Alive\r\n\r\n");
                                                pw.print(content);
                                                pw.flush();
                                            }
                                        }
                                    }
                                    catch (IOException e) {
                                        Log.e("responseThread", e.getMessage(), e);
                                    }
                                    finally {
                                        if (br != null) {
                                            try {
                                                br.close();
                                            }
                                            catch (IOException e) {
                                                Log.e("responseThread", e.getMessage(), e);
                                            }
                                        }
                                    }
                                }
                            }).start();
                        }
                        catch(IOException e) {
                            if (isRunning) {
                                Log.e("acceptThread", e.getMessage(), e);
                            }
                        }
                    }
                }
            }).start();
        }
        catch (IOException e) {
            Log.e("onCreate", e.getMessage(), e);
            finish();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        if (!mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isRunning = false;
        try {
            mServerSocket.close();
        }
        catch (IOException e) {
            Log.e("onDestroy", e.getMessage(), e);
        }
    }
}