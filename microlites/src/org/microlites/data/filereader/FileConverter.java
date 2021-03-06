package org.microlites.data.filereader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import android.content.res.Resources;
import android.os.Environment;

public class FileConverter {
	private ArrayList<Byte> stream;
	
	public FileConverter() {
		stream = new ArrayList<Byte>();
	}
	
	// Lee un archivo de texto y devuelve un vector de datos sin procesar (bytes)
	public ArrayList<Byte> readTxt(String fname) {
		stream.clear();
		
		try {
			Scanner sc = new Scanner(new File(fname));
			
			while(sc.hasNext()) {
				if (sc.hasNextInt()) {
					int aux = sc.nextInt();
					stream.add((byte) aux);
				}
				else
					sc.next();
			}
			
			sc.close();
		} catch (FileNotFoundException e) {
			System.err.println("Archivo no encontrado: " + fname);
			e.printStackTrace();
		}
		
		return stream;
	}
	
	// Lee un archivo binario y devuelve sus datos en un ArrayList de bytes
	// @param resultSize One element array to output size of returned array
	public byte[] readBinaryFriendly(String fname, int[] resultSize, boolean[] stop) {
		byte[] bytes = new byte[1];// = new byte[256];
		//int size = 256;
		int curr = 0;
		
		byte[] buffer = new byte[1024];
		int cnt = 0;
		
		try {
			File f = new File(fname);
			FileInputStream s = new FileInputStream(f);
			// TODO: BIG BIG BIG log sizes won't work
			bytes = new byte[(int) f.length()];
			
			//int aux = 0;
			while (!stop[0] && ((cnt = s.read(buffer)) > 0)) {
				for (int i = 0; i < cnt; i++)
					if (curr < bytes.length-1)
						bytes[curr++] = buffer[i];
			}
			
			/*while (!stop[0] && (aux = s.read()) >= 0) {
				// Add the new byte
				bytes[curr++] = (byte) aux;
				// Duplicate the array if filled
				if (curr >= size) {
					byte[] newbytes = new byte[size*2];
					for (int i = 0; i < size; i++)
						newbytes[i] = bytes[i];
					bytes = newbytes;
					size *= 2;
				}
			}*/
			
			if (stop[0])
				System.out.println("Stopped");
			
			s.close();
		}
		catch (FileNotFoundException e) {
			// TODO: Controlar qué pasa en este caso! (Toast y no fallar, o algo)
			System.err.println("Archivo no encontrado: " + fname);
			e.printStackTrace();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		if (resultSize.length >= 1)
			resultSize[0] = curr;
		else
			System.err.println("FileConverter::readBinaryFriendly - " +
					"No suitable array to contain result size provided");
		
		return bytes;
	}
	
	// Lee un archivo binario y devuelve sus datos en un ArrayList de bytes
	public ArrayList<Byte> readBinary(String fname) {
		stream.clear();
		
		File root = Environment.getExternalStorageDirectory();
		File path = new File(root, "/Download/");
		path.mkdirs();
		
		try {
			//FileInputStream s = new FileInputStream(new File(path, fname));
			FileInputStream s = new FileInputStream(new File(fname));
			
			int aux = 0;
			while ((aux = s.read()) >= 0)
				stream.add((byte) aux);
			
			s.close();
		}
		catch (FileNotFoundException e) {
			System.err.println("Archivo no encontrado: " + fname);
			e.printStackTrace();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		return stream;
	}
	
	// Lee el recurso de identificador id y devuelve sus datos en forma de array de bytes
	public ArrayList<Byte> readResources(Resources resources, int id) {
		InputStream input = resources.openRawResource(id);
		stream.clear();
		
		int r = 0;
		try {
			while ((r = input.read()) >= 0) {
				stream.add((byte) r);
			}
			input.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		return stream;
	}
	
	// Escribe el vector de datos en un archivo de texto
	public void writeTxt(String fname) {
		if (!stream.isEmpty()) {
			try {
				FileWriter writer = new FileWriter(new File(fname));
				
				Iterator<Byte> it = stream.iterator();
				while (it.hasNext()) {
					Byte b = it.next();
					int i = (b & 0xff);
					writer.write(i + "\n");
				}
				
				writer.close();
			}
			catch (FileNotFoundException e) {
				System.err.println("Ruta no v�lida: " + fname);
				e.printStackTrace();
			}
			catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		else System.err.println("No hay nada que guardar!");
	}
	
	// Vuelca el contenido del vector de datos en un archivo binario
	public void writeBinary(String fname) {
		if (!stream.isEmpty()) {
			try {
				FileOutputStream s = new FileOutputStream(new File(fname));
				DataOutputStream d = new DataOutputStream(s);
				
				Iterator<Byte> it = stream.iterator();
				while (it.hasNext())
					d.writeByte(it.next());
				
				s.close();
			} catch (FileNotFoundException e) {
				System.err.println("Ruta no v�lida: " + fname);
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		else System.err.println("No hay nada que guardar!");
	}
	
	public ArrayList<Byte> getStream() { return stream; }
	
	/*
	public static void main(String args[]) {
		FileConverter f = new FileConverter();
		
		f.readTxt("traza.txt");
		f.writeTxt("traza2.txt");
		f.writeBinary("traza3.txt");
		f.readBinary("traza3.txt");
		f.writeTxt("traza4.txt");
		
		System.exit(0);
	}
	*/
}