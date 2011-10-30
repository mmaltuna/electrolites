package com.electrolites.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Data {
	private static Data instance = null;
	public static Data getInstance() {
		if (instance == null) {
			instance = new Data();
		}
		
		return instance;
	}
	
	// Altura base de renderizado (0~1)
	private float drawBaseHeight = 0.5f;
	// Escala de ancho: 0 (evitar) ~ whatever
	private float WidhtScale = 1;
	// Muestras indexadas por no. de muestra
	public ArrayList<Short> samples = null;
	// Puntos resultantes de la delineaci�n, indexados por no. de muestra
	public Map<Integer, DPoint> dpoints = null;
	// Valores del ritmo card�aco, indexados seg�n el n�mero de muestra anterior a su recepci�n
	public Map<Integer, Short> hbr = null;
	// Primera muestra dibujable
	public int dataOffset;
	
	public Data() {
		drawBaseHeight = 0.5f;
		WidhtScale = 1;
		samples = new ArrayList<Short>();
		dpoints = new HashMap<Integer, DPoint>();
		hbr = new HashMap<Integer, Short>();
	}



	public float getDrawBaseHeight() {
		return drawBaseHeight;
	}



	public void setDrawBaseHeight(float drawBaseHeight) {
		this.drawBaseHeight = drawBaseHeight;
	}



	public float getWidhtScale() {
		return WidhtScale;
	}



	public void setWidhtScale(float widhtScale) {
		WidhtScale = widhtScale;
	}
	
	
	public short[] getSamplesArray() {
		
		Object samp[] = samples.toArray();
		short result[] = new short[samp.length];
		for (int i = 0; i < samp.length; i++)
			result[i] = ((Short) samp[i]).shortValue();
		
		return result;
	}
}
