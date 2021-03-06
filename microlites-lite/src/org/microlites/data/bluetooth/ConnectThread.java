﻿package org.microlites.data.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

// Obtiene el socket para enviar datos
public class ConnectThread extends Thread {
	private final BluetoothAdapter bA;
	private final BluetoothManager bS;
	private final BluetoothSocket socket;
    private final BluetoothDevice device;

    public ConnectThread(BluetoothAdapter bA, BluetoothManager bS, BluetoothDevice device, UUID uuid) {
        this.bA = bA;
        this.bS = bS;
    	this.device = device;
        BluetoothSocket tmp = null;

        // Obtenemos el socket para la conexión con el dispositivo dado
        try {
        	tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(BluetoothManager.TAG, "Ha fallado la creación del socket.", e);
        }
        
        socket = tmp;
    }

    @Override
	public void run() {
        Log.i(BluetoothManager.TAG, "ConnectThread comienza su actividad");

        // Cancelamos el descubrimiento de nuevos dispositivos
        if (bA.isDiscovering())
        	bA.cancelDiscovery();

        // Creamos una conexión con el socket
        try {
            socket.connect(); // Llamada bloqueante
        } catch (IOException e) {
            // Cerramos el socket
        	System.err.println("Fallo al establecer conexión bluetooth: "+e.getMessage());
            try {
                socket.close();
                
            } catch (IOException e2) {
                Log.e(BluetoothManager.TAG, "No se puede cerrar el socket.", e2);
            }
            bS.connectionFailed();
            return;
        }

        // Reseteamos el thread porque hemos terminado
        synchronized (bS) {
            bS.setConnectThread(null);
        }

        // Arrancamos el connectedThread
        bS.connected(socket, device);
    }

    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(BluetoothManager.TAG, "Ha fallado el cierre del socket.", e);
        }
    }
}
