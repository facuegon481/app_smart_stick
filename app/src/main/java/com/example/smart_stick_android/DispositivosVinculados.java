package com.example.smart_stick_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class DispositivosVinculados extends AppCompatActivity {

    // Depuración de LOGCAT
    private static final String TAG = "DispositivosVinculados";
    // Declaracion de ListView
    ListView devicesList;
    // String que se enviara a la actividad principal, mainactivity
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    // Declaracion de campos
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter mPairedDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispositivos_vinculados);
    }

    @Override
    public void onResume() {
        super.onResume();
        //---------------------------------------------------------------------
        VerificarEstadoBT();
        mPairedDevicesArrayAdapter = new ArrayAdapter(this, R.layout.dispositivos_encontrados);
        devicesList = (ListView) findViewById(R.id.devicesList);
        devicesList.setAdapter(mPairedDevicesArrayAdapter);
        devicesList.setOnItemClickListener(mDeviceClickListener);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        try{
            Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }catch(SecurityException e){
            Log.e("err2", e.getMessage());
        }


        //---------------------------------------------------------------------
    }

    // Configura un (on-click) para la lista
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {

            // Obtener la dirección MAC del dispositivo
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            finishAffinity();

            // Realiza un intent para iniciar la siguiente actividad
            Intent intend = new Intent(DispositivosVinculados.this, MainActivity.class);
            intend.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(intend);
        }
    };

    private void VerificarEstadoBT() {
        // Comprueba que el dispositivo tiene Bluetooth y que está encendido.
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (mBtAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth Activado...");
            } else {
                //Solicita al usuario que active Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                try{
                    startActivityForResult(enableBtIntent, 1);
                }catch(SecurityException ex){
                    Log.e("err2", ex.getMessage());
                }
            }
        }
    }
}