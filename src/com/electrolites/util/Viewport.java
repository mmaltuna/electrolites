package com.electrolites.util;

public class Viewport {
	// Posici�n, ancho y alto del viewport en pixels
	public int vpPxX;
	public int vpPxY;
	public int vpPxWidth;
	public int vpPxHeight;

	// Posici�n X de la viewarea en segundos
	public float vaSecX;
	// Segundos comprendidos en la viewarea
	public float vaSeconds;
	// Muestras por segundo
	public float samplesPerSecond;
	
	// Base de dibujo horizontal
	public float baselinePxY;
	
	// Datos (temporal)
	public float[] data;
	public int dataStart;
	public int dataEnd;
	
	public Viewport(int width, int height) {
		vpPxWidth = width;
		vpPxHeight = height;
		
		samplesPerSecond = 250f;
		vaSeconds = 2f;
		vaSecX = 0f;
		baselinePxY = vpPxY + vpPxHeight/2;
	}
	
	public Viewport(int width, int height, float seconds) {
		vpPxWidth = width;
		vpPxHeight = height;
		vaSeconds = seconds;
		
		samplesPerSecond = 250f;
		vaSecX = 0f;
		baselinePxY = vpPxY + vpPxHeight/2;
	}
	
	public float[] getViewContents() {
		// Calcular cantidad de puntos que caben
		float npoints = vaSeconds*samplesPerSecond;
		// Calcular densidad de puntos
		float dpoints = vpPxWidth / npoints;
		// Si la densidad es < 0 es que se quieren mostrar 
		// m�s puntos de los que caben (aglutinar o...)
		if (dpoints < 0)
			return null;
		// Buscar primer punto
		// Por ahora, redonder y coger el que sea (mejorar esto)
		int start = Math.round(vaSecX);
		// Buscar �ltimo punto
		int end = start + Math.round(npoints);
		// Construir la lista de puntos a devolver
		float points[] = new float[(end-start-2)*4+4];
		for (int i = 0; i < start-end; i+=1) {
			// Devolver array de puntos a pintar
			// X, Y
			if (i == 0) {
				points[i] = vpPxX;
				points[i+1] = baselinePxY + data[start+1];
			}
			else {
				// Si no es el primer punto, duplicar el anterior
				points[4*i] = points[4*i-2];
				points[4*i+1] = points[4*i-1];
				points[4*i+2] = vpPxX + i*dpoints;
				points[4*i+3] = baselinePxY + data[start+i];
			}
		}
		return points;
	}
	
	public boolean move(float secDeltaX) {
		// Comprobaci�n de l�mites
		if (secDeltaX > 0) {
			if (vaSecX + secDeltaX >= samplesPerSecond*(dataEnd - dataStart) - vaSeconds) {
				vaSecX = samplesPerSecond*(dataEnd - dataStart) - vaSeconds;
				return false;
			} else {
				vaSecX += secDeltaX;
			}
		}
		else if (secDeltaX < 0) {
			if (vaSecX + secDeltaX < 0) {
				vaSecX = 0;
				return false;
			} else {
				vaSecX += secDeltaX;
			}
		}
		
		return true;
	}
}