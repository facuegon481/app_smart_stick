package com.example.smart_stick_android;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //Button btnConectar;

    Button btnDesconectar;
    Button btnDistancia;
    EditText txtDistancia;

    BluetoothSocket btSocket;
    Handler bluetoothIn;
    final int handlerState = 0;
    private ConnectedThread mConnectedThread;
    private String address;

    /* Sensores */

    private SensorManager sensorManager;
    private Sensor sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothIn = Handler_Msg_Hilo_Principal();

        //btnConectar=(Button)findViewById(R.id.btnConectar);
        //Seteamos los valores para vista
        btnDesconectar=(Button)findViewById(R.id.btnDesconectar);
        btnDistancia=(Button)findViewById(R.id.btnDistancia);
        txtDistancia=(EditText) findViewById(R.id.txtDistancia);

        //defino los handlers para los botones Apagar y encender
        //btnConectar.setOnClickListener(btnConectarListener);
        btnDesconectar.setOnClickListener(btnDesconectarListener);
        btnDistancia.setOnClickListener(btnDistanciaListener);

        Intent intent = getIntent();
        address = intent.getStringExtra(DispositivosVinculados.EXTRA_DEVICE_ADDRESS);

        btSocket = null;
        try{
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.i("info", btAdapter.getBondedDevices().toString());

            BluetoothDevice device = btAdapter.getRemoteDevice(address);
            Log.i("info2", device.getName());

            btSocket = device.createRfcommSocketToServiceRecord(mUUID);
            Log.i("info3", btSocket.toString());

            btSocket.connect();
            Log.i("info4", btSocket.isConnected() == true ? "true" : "false");

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

        }catch(SecurityException ex){
            Log.i("err", ex.getMessage());
        } catch (IOException e) {
            Log.i("err2", e.getMessage());
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, sensor, sensorManager.SENSOR_DELAY_NORMAL);

    }

    private Handler Handler_Msg_Hilo_Principal ()
    {
        return new Handler() {
            public void handleMessage(android.os.Message msg) {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState) {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    String[] comando_completo = readMessage.split("_");

                    if(comando_completo.length == 2) {
                        String evento = comando_completo[0];
                        String valor = comando_completo[1];

                        switch (evento){
                            case "D":
                                txtDistancia.setText(valor);
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), "Evento inválido", Toast.LENGTH_SHORT).show();
                                break;
                        }

                    }else{
                        Toast.makeText(getApplicationContext(), "Comando inválido", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
    }

    private View.OnClickListener btnDesconectarListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            try {
                OutputStream outpuStream = btSocket.getOutputStream();
                //outpuStream.write("DESCONECTANDO DE LA APP".getBytes(Charset.forName("UTF-8")));
                btSocket.close();
            } catch (IOException e) {
                Log.i("err3", e.getMessage());

            }
        }
    };

    private View.OnClickListener btnDistanciaListener = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            try {
                /*
                OutputStream outpuStream = btSocket.getOutputStream();
                outpuStream.write("D".getBytes(Charset.forName("UTF-8")));
                outpuStream.flush();

                 */
                mConnectedThread.write("T");
            } catch (Exception e) {
                Log.i("err3", e.getMessage());
            }
        }
    };

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i("INFO_SENSOR", "x: " + sensorEvent.values[0] + ";y: " + sensorEvent.values[1] + ";z: " + sensorEvent.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private class ConnectedThread extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Constructor de la clase del hilo secundario
        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //metodo run del hilo, que va a entrar en una espera activa para recibir los msjs del HC05
        public void run()
        {
            byte[] buffer = new byte[256];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }


        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.e("error tread", "conexion falló");
                finish();

            }
        }
    }
}

