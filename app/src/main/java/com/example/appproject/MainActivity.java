package com.example.appproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
        private TextView tv_1;
        private Button btn_connect;
        static  final int REQUEST_ENABLE_BT = 10;
        int mPariedDeviceCount =0;
        Set<BluetoothDevice>mDevices;

        BluetoothAdapter mBluetoothAdapter;

        BluetoothDevice mRemoteDevie;

        BluetoothSocket mSocket =null;
        OutputStream mOutputStream =null;
        InputStream mInputStream = null;
        String mStrDelimiter ="\n";
        char mCharDelimiter = '\n';
        Thread mWorkerThread;
        byte[] readBuffer;
        int readBufferPosition;
        Handler handler;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            tv_1 = findViewById(R.id.tv_1);
            btn_connect = findViewById(R.id.btn_connect);
            btn_connect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBluetooth();
                    //1. 블루투스 기기 가능 한지 체크 하고 , 2.  select 함수로 넘어가서 활용가능 하다면 목록 띄워주고 페어링
                    //3. select 함수가 리스트들(item)을 배열로 connect 함수에 넘겨주고 ,
                    //4. connect에서 다리를 연결 해주어 데이터 수신 준비를 함 beginListenFordata 함수
                }
            });

        }

        BluetoothDevice getDeviceFromBondedList(String name) {//기기 목록 얻어 옴
            BluetoothDevice selectedDevice = null;
            for (BluetoothDevice device : mDevices) {

                if (name.equals(device.getName())) {
                    selectedDevice = device;
                    break;
                }
            }
            return selectedDevice;
        }
        void sendData(String msg){//문자열 전송하는 함수
            handler = new Handler();
            msg += mStrDelimiter;
            try {
                mOutputStream.write(msg.getBytes());
                byte[]send = msg.getBytes();
                handler.obtainMessage(2,-1,-1,send);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송중 오류 발생", Toast.LENGTH_SHORT).show();
                finish();//app 종료
            }
        }
        void connectToSelectedDevice(String selectedDeviceName){//원격장치와 연결하는 과정
            mRemoteDevie =getDeviceFromBondedList(selectedDeviceName);
            UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            try{
                // 소켓 생성, RFCOMM 채널을 통한 연결.
                // createRfcommSocketToServiceRecord(uuid) : 이 함수를 사용하여 원격 블루투스 장치와
                //                                           통신할 수 있는 소켓을 생성함.
                // 이 메소드가 성공하면 스마트폰과 페어링 된 디바이스간 통신 채널에 대응하는
                //  BluetoothSocket 오브젝트를 리턴함.
                mSocket =mRemoteDevie.createRfcommSocketToServiceRecord(uuid);
                mSocket.connect();

                // 데이터 송수신을 위한 스트림 얻기.
                // BluetoothSocket 오브젝트는 두개의 Stream을 제공한다.
                // 1. 데이터를 보내기 위한 OutputStrem
                // 2. 데이터를 받기 위한 InputStream

                mOutputStream = mSocket.getOutputStream();
                mInputStream = mSocket.getInputStream();

                // 데이터 수신 준비.

                        beginListenForData();
                        mWorkerThread.start();
            }catch (Exception e){
                Toast.makeText(getApplicationContext(),"블루투스 연결중 오류",Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    private void beginListenForData() {//데이터 수신(쓰레드 사용해 수신된 메세지를 계속 검사한다.)
        Log.v("ddd","1");
            handler = new Handler();
            readBufferPosition =0;  // 버퍼 내 수신 문자 저장 위치.
            readBuffer = new byte[1024];  // 수신 버퍼

            mWorkerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // interrupt() 메소드를 이용 스레드를 종료시키는 예제이다.
                    // interrupt() 메소드는 하던 일을 멈추는 메소드이다.
                    // isInterrupted() 메소드를 사용하여 멈추었을 경우 반복문을 나가서 스레드가 종료하게 된다.
                    Log.v("ddd","2");
                    while(!Thread.currentThread().isInterrupted()){
                        try {
                            int byteAvailable = mInputStream.available();
                            Log.v("ddd","3");
                            Thread.sleep(1000);
                            if(byteAvailable >0){
                                Log.v("ddd","4"+byteAvailable);
                                byte[]packetByte = new byte[byteAvailable];
                                mInputStream.read(packetByte);

                                for (int i =0;i<byteAvailable;i++){
                                    final byte b =packetByte[i];
                                    Log.v("ddd","5"+mCharDelimiter);
                                    Log.v("ddd","6"+b);
                                    if (b<103){

                                        Log.v("ddd","7");
                                     byte[] encodeByte = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer,0,encodeByte,0,encodeByte.length);

                                        final String data = new String(encodeByte,"US-ASCII");
                                        readBufferPosition = 0;
                                        Log.v("ddd","8"+data);
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                tv_1.setText(b+data);
                                            }
                                        });
                                    }else{
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(),"데이터 수신중 오류가 발생했습니다",Toast.LENGTH_SHORT).show();
                            finish();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });


    }


    void selectDevice(){//블루투스 페어링 하는 함수
            mDevices = mBluetoothAdapter.getBondedDevices();
            mPariedDeviceCount = mDevices.size();
            if(mPariedDeviceCount ==0){
                Toast.makeText(getApplicationContext(),"페어링된 장치가 없습니다.",Toast.LENGTH_SHORT).show();
                finish();
            }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("블루투스 장치 선택");
        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices){
            listItems.add(device.getName());
        }
        listItems.add("취소");
        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        listItems.toArray(new CharSequence[listItems.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item==mPariedDeviceCount){
                    Toast.makeText(getApplicationContext(),"연결할 장치를 선택하지 않았습니다.",Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    connectToSelectedDevice(items[item].toString());
                    Toast.makeText(getApplicationContext(),"블루투스 연결 성공",Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void checkBluetooth() {//블루투스 모듈이 있는 기기인지 없는 기기인지 체크
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null){
                Toast.makeText(getApplicationContext(),"기기가 블루투스를 지원하지 않습니다. ",Toast.LENGTH_SHORT).show();
                finish();
            }else{
                if (!mBluetoothAdapter.isEnabled()){
                    Toast.makeText(getApplicationContext(),"현재 블루투스가 비활성화 되어 있습니다.",Toast.LENGTH_SHORT).show();
                    Intent enableBIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBIntent,REQUEST_ENABLE_BT);
                }
                else{
                    selectDevice();
                }
            }

    }
    @Override
    protected void onDestroy() {
        try{
            mWorkerThread.interrupt(); // 데이터 수신 쓰레드 종료
            mInputStream.close();
            mSocket.close();
        }catch(Exception e){}
        super.onDestroy();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // startActivityForResult 를 여러번 사용할 땐 이런 식으로
        // switch 문을 사용하여 어떤 요청인지 구분하여 사용함.
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) { // 블루투스 활성화 상태
                    selectDevice();
                }
                else if(resultCode == RESULT_CANCELED) { // 블루투스 비활성화 상태 (종료)
                    Toast.makeText(getApplicationContext(), "블루투수를 사용할 수 없어 프로그램을 종료합니다",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    }
