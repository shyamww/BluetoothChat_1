package com.example.bluetoothchat_1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bluetoothchat_1.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter myBluetoothAdapter;
    BluetoothDevice[] btarray;
    Button buttonON, buttonOFF, listDevices,send,listen;
    ListView listView;
    TextView msg_box,status;
    EditText writemsg;
    SendReceive sendReceive;

    Intent BTEnablingIntent;
    int req_code=1;

    static final int STATE_LISTENING =1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    private static final String APP_NAME="Bluetooth Chat";
    private static final UUID MYUUID=UUID.fromString("77bc9084-e2f2-4b37-b0ad-ba29ff422953");



    @Override  //disbaling the bluetooth
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==req_code)
        {
            if(resultCode==RESULT_OK)
            {
                Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            }
            else if(resultCode==RESULT_CANCELED)
            {
                Toast.makeText(this, "Bluetooth enabling is cancelled", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonON=(Button) findViewById(R.id.bton);
        buttonOFF=(Button) findViewById(R.id.btoff);
        listen = (Button) findViewById(R.id.listen);
        send = (Button) findViewById(R.id.send);
        listDevices= (Button) findViewById(R.id.btnshow);
        listView =(ListView) findViewById(R.id.listview);
        msg_box =(TextView) findViewById(R.id.msg);
        status =(TextView) findViewById(R.id.status);
        writemsg=(EditText) findViewById(R.id.writemsg);

        myBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        BTEnablingIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        bluetoothOnMethod();
        bluetoothOFFMethod();
        implementlistener();

    }

    private void implementlistener() {
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt=myBluetoothAdapter.getBondedDevices();
                String[] string= new String[bt.size()];
                btarray=new BluetoothDevice[bt.size()];
                int index=0;

                if(bt.size()>0)
                {
                    for(BluetoothDevice device: bt)
                    {
                        btarray[index]=device;
                        string[index]=device.getName();
                        index=index+1;
                    }
                    ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,string);
                    listView.setAdapter(arrayAdapter);
                }
            }
        });

        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass=new ServerClass();
                serverClass.start();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                ClientClass clientClass=new ClientClass(btarray[i]);
                clientClass.start();
                status.setText("Connecting");
            }
        });


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string= String.valueOf(writemsg.getText());
                sendReceive.write(string.getBytes());
                writemsg.setText("");
            }
        });

    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer=(byte[]) msg.obj;
                    String tempMsg=new String(readBuffer,0,msg.arg1);
                    msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void bluetoothOFFMethod() {
        buttonOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothAdapter.isEnabled())
                {
                    myBluetoothAdapter.disable();
                    Toast.makeText(MainActivity.this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Bluetooth is already disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bluetoothOnMethod() {
        buttonON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myBluetoothAdapter==null)
                {
                    Toast.makeText(MainActivity.this, "Bluetooth is not found", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if(!myBluetoothAdapter.isEnabled())
                    {
                        startActivityForResult(BTEnablingIntent,req_code);
                    }
                }
            }
        });
    }

    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;
        public ServerClass()
        {
            try {
                serverSocket=myBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MYUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket=null;

            while(socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null)
                {

                        Message message=Message.obtain();
                        message.what=STATE_CONNECTED;
                        handler.sendMessage(message);
                        sendReceive = new SendReceive(socket);
                        sendReceive.start();
                        break;


//                    break;
                }
            }
        }


    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1)
        {
            device=device1;
            try {
                socket=device.createRfcommSocketToServiceRecord(MYUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            // If i m discovering the devices also then i have to first cancel the discovering before connecting to a device
            try {
                socket.connect();

                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }

    }

    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket)
        {
            bluetoothSocket = socket;
            InputStream tempIN=null;
            OutputStream tempOUT=null;

            try {
                tempIN=bluetoothSocket.getInputStream();
                tempOUT=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIN;
            outputStream=tempOUT;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;
            while(true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }

        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
