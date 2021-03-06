package com.electrolites.ecg;

import java.util.LinkedList;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.electrolites.data.Data;
import com.electrolites.util.DPoint;
import com.electrolites.util.DynamicViewport;
import com.electrolites.util.ExtendedDPoint;
import com.electrolites.util.LineDrawCommand;
import com.electrolites.util.PositionedDPoint;
import com.electrolites.util.Viewport;
import com.electrolites.util.DPoint.PointType;
import com.electrolites.util.DPoint.Wave;

public class ECGView extends AnimationView {
	
	private class ECGThread extends AnimationThread {
		
		protected Paint textPaint, rectPaint, ecgPaint;
		protected int bgColor;

		public ECGThread(SurfaceHolder holder) {
			super(holder);
			
			bgColor = Color.rgb(0, 0, 0);
			
			textPaint = new Paint();
			textPaint.setARGB(200, 100, 255, 100);
			textPaint.setStrokeWidth(2.f);
			textPaint.setTextAlign(Align.RIGHT);
			
			ecgPaint = new Paint();
			
			rectPaint = new Paint();
			rectPaint.setColor(Color.rgb(69, 69, 69));
		}
		
		protected void renderDPoint(Canvas canvas, LineDrawCommand com) {
			ecgPaint.setARGB(com.getA(), com.getR(), com.getG(), com.getB());
			ecgPaint.setStrokeWidth(com.getWidth());
			canvas.drawLine(com.getX1(), com.getY1(), com.getX2(), com.getY2(), ecgPaint);
		}

		@Override
		public void onRender(Canvas canvas) {
			// TODO Auto-generated method stub
			
		}
		
	}

	private class ECGThreadStatic extends ECGThread {
		
		public ECGThreadStatic(SurfaceHolder holder) {
			super(holder);
		}
		
		@Override
		public void onRender(Canvas canvas) {
			
			//synchronized (this) {
			if (data == null || vport == null)
				return;
			
				vport.data = data.getSamplesArray();
				if (data.autoScroll)
					vport.moveToEnd();
			//}
			
			vport.dataStart = 0;
			vport.dataEnd = vport.data.length;
			
			
			boolean loading = data.loading;
			
			canvas.drawColor(data.bgColor);
            //canvas.drawText("fps: " + fps, 100, 100, textPaint);
			
			int left = vport.vpPxX-5, right = vport.vpPxX+vport.vpPxWidth+5, top = vport.vpPxY-1, bottom = vport.vpPxY+vport.vpPxHeight+1;

			if (loading) {
				// Ultimate Cutresy!
				canvas.drawRect(new Rect(0, 0, getWidth(), vport.vpPxY-1), rectPaint);
				canvas.drawRect(new Rect(0, vport.vpPxY+vport.vpPxHeight+1, getWidth(), getHeight()), rectPaint);
				canvas.drawRect(new Rect(0, 0, left, getHeight()), rectPaint);
				canvas.drawRect(new Rect(right, 0, getWidth(), getHeight()), rectPaint);
				
				textPaint.setStrokeWidth(2.f);
				canvas.drawLine(left, top, right, top, textPaint);
				canvas.drawLine(left, top, left, bottom, textPaint);
				canvas.drawLine(left, bottom, right, bottom, textPaint);
				canvas.drawLine(right, top, right, bottom, textPaint);
				
				Align a = textPaint.getTextAlign();
				float s = textPaint.getTextSize();
				textPaint.setTextAlign(Align.CENTER);
				textPaint.setTextSize(48);
				canvas.drawText("Loading...", vport.vpPxX+vport.vpPxWidth/2, vport.vpPxY+vport.vpPxHeight/2, textPaint);
				textPaint.setTextAlign(a);
				textPaint.setTextSize(s);
				
			} else {
				// Render Axis and Data
				textPaint.setARGB(230, 150, 150, 150);
				textPaint.setStrokeWidth(2.f);
				canvas.drawLine(left, vport.vpPxY, left, vport.vpPxY+vport.vpPxHeight, textPaint);
				
				// Render Axis Scales
				// Upper part
				int divisions = (int) Math.floor((vport.baselinePxY - vport.vpPxY) / (1000*vport.vFactor));
				
				//canvas.drawText("0.0", left-2, vport.baselinePxY, linePaint);
				canvas.drawLine(left, vport.baselinePxY, vport.vpPxX+5+vport.vpPxWidth, vport.baselinePxY, textPaint);
				textPaint.setStrokeWidth(1.f);
				for (int i = 0; i <= divisions; i++) {
				//	canvas.drawText("" + (float) i, left-2, vport.baselinePxY-i*1000*vport.vFactor, linePaint);
					canvas.drawLine(left, vport.baselinePxY-i*1000*vport.vFactor, vport.vpPxX+5+vport.vpPxWidth, vport.baselinePxY-i*1000*vport.vFactor, textPaint);
				}
				
				// Lower part
				divisions = (int) Math.floor((vport.vpPxY+vport.vpPxHeight- vport.baselinePxY) / (1000*vport.vFactor));
				
				for (int i = 1; i <= divisions; i++) {
				//	canvas.drawText("" + (float) -i, left-2, vport.baselinePxY+i*1000*vport.vFactor, linePaint);
					canvas.drawLine(left, vport.baselinePxY+i*1000*vport.vFactor, vport.vpPxX+5+vport.vpPxWidth, vport.baselinePxY+i*1000*vport.vFactor, textPaint);
				}
				
				
				// Render samples
				ecgPaint.setColor(Color.GREEN);
				ecgPaint.setAlpha((int) (255*0.9));
				ecgPaint.setStrokeWidth(2.f);
	            float points[] = vport.getViewContents();
	            int toDraw = points.length;
	            canvas.drawLines(points, 0, toDraw,  ecgPaint);
	            
				// Render delineation results
	            for (LineDrawCommand cmd: vport.getViewDPoints())
	            	renderDPoint(canvas, cmd);
	            
				// Ultimate Cutresy!
				canvas.drawRect(new Rect(0, 0, getWidth(), vport.vpPxY-1), rectPaint);
				canvas.drawRect(new Rect(0, vport.vpPxY+vport.vpPxHeight+1, getWidth(), getHeight()), rectPaint);
				canvas.drawRect(new Rect(0, 0, left, getHeight()), rectPaint);
				canvas.drawRect(new Rect(right, 0, getWidth(), getHeight()), rectPaint);
				
				textPaint.setStrokeWidth(2.f);
				canvas.drawLine(left, top, right, top, textPaint);
				canvas.drawLine(left, top, left, bottom, textPaint);
				canvas.drawLine(left, bottom, right, bottom, textPaint);
				canvas.drawLine(right, top, right, bottom, textPaint);
				
				divisions = (int) Math.floor((vport.baselinePxY - vport.vpPxY) / (1000*vport.vFactor));
				
				canvas.drawText("0.0", left-2, vport.baselinePxY, textPaint);
				for (int i = 0; i <= divisions; i++) {
					canvas.drawText("" + (float) i, left-2, vport.baselinePxY-i*1000*vport.vFactor, textPaint);
				}
				
				// Lower part
				divisions = (int) Math.floor((vport.vpPxY+vport.vpPxHeight- vport.baselinePxY) / (1000*vport.vFactor));
				
				for (int i = 1; i <= divisions; i++) {
					canvas.drawText("" + (float) -i, left-2, vport.baselinePxY+i*1000*vport.vFactor, textPaint);
				}
				
				textPaint.setTextAlign(Align.LEFT);
				canvas.drawText(vport.vaSecX + " - " + (vport.vaSecX+vport.vaSeconds), left, top-10, textPaint);
				textPaint.setTextAlign(Align.RIGHT);
			}
			
            canvas.restore();
		}
	}

	private class ECGThreadDynamic extends ECGThread {
		private long lastTime;

		public ECGThreadDynamic(SurfaceHolder holder) {
			super(holder);
			
			lastTime = 0;
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			/*try {
				super.onUpdate();
				sleep((long) 4);
			} catch (InterruptedException e) {
				System.err.println("QUIEN OSA DESPERTAR A MALTUS!?");
			}*/
		}
		
		@Override
		public void onRender(Canvas canvas) {
			
			synchronized (data.dynamicData.mutex) {
			
			if (canvas == null || dvport == null)
				return;

			/*** Calculate border positions ***/
				int left = dvport.vpPxX;
				int right = dvport.vpPxX + dvport.vpPxWidth;
				int top = dvport.vpPxY;
				int bottom = dvport.vpPxY + dvport.vpPxHeight;
			
			/*** Clear canvas ***/
				canvas.drawColor(bgColor);
			
			/*** Render axis and scales ***/
				
				// y axis
				textPaint.setARGB(230, 150, 150, 150);
				textPaint.setStrokeWidth(2.f);
				canvas.drawLine(left, top, left, bottom, textPaint);
				
				// Upper scale part
				int divisions = (int) Math.floor((dvport.baselinePxY - dvport.vpPxY) / (1000*dvport.vFactor));
				
				canvas.drawLine(left, dvport.baselinePxY, right+5, dvport.baselinePxY, textPaint);
				textPaint.setStrokeWidth(1.f);
				for (int i = 0; i <= divisions; i++) {
					canvas.drawLine(left, dvport.baselinePxY-i*1000*dvport.vFactor, right+5, dvport.baselinePxY-i*1000*dvport.vFactor, textPaint);
				}
				
				// Lower part
				divisions = (int) Math.floor((dvport.vpPxY+dvport.vpPxHeight- dvport.baselinePxY) / (1000*dvport.vFactor));
				
				for (int i = 1; i <= divisions; i++) {
					canvas.drawLine(left, dvport.baselinePxY+i*1000*dvport.vFactor, right+5, dvport.baselinePxY+i*1000*dvport.vFactor, textPaint);
				}
			
			// Get Data (samples + dpoints)
	            float points[] = dvport.getViewContents();
	            LinkedList<LineDrawCommand> pointsList = dvport.getViewDPoints();
				
			// Render samples
				ecgPaint.setColor(Color.rgb(59, 250, 59));
				ecgPaint.setAlpha((int) (255*0.9));
				ecgPaint.setStrokeWidth(2.f);
	            
	            if (points != null) {
	            	//canvas.drawLines(points, ecgPaint);
	            	//canvas.drawPoints(points, ecgPaint);
	            	canvas.drawLines(points, 0, (int) (points.length), ecgPaint);
	            }
			
			// Render dpoints
	            //LinkedList<LineDrawCommand> pointsList = dvport.getViewDPoints();
	            int ndPonits = pointsList.size();
	            for (int i = 0; i < ndPonits; i++) {
	            	renderDPoint(canvas, pointsList.remove());
	            }
			
			// Render frame
				canvas.drawRect(new Rect(0, 0, getWidth(), dvport.vpPxY-1), rectPaint);
				canvas.drawRect(new Rect(0, dvport.vpPxY+dvport.vpPxHeight+1, getWidth(), getHeight()), rectPaint);
				canvas.drawRect(new Rect(0, 0, left, getHeight()), rectPaint);
				canvas.drawRect(new Rect(right, 0, getWidth(), getHeight()), rectPaint);
				
				textPaint.setStrokeWidth(2.f);
				canvas.drawLine(left, top, right, top, textPaint);
				canvas.drawLine(left, top, left, bottom, textPaint);
				canvas.drawLine(left, bottom, right, bottom, textPaint);
				canvas.drawLine(right, top, right, bottom, textPaint);
			
			// Render text labels 
				divisions = (int) Math.floor((dvport.baselinePxY - dvport.vpPxY) / (1000*dvport.vFactor));
				
				canvas.drawText("0.0", left-2, dvport.baselinePxY, textPaint);
				for (int i = 0; i <= divisions; i++) {
					canvas.drawText("" + (float) i, left-2, dvport.baselinePxY-i*1000*dvport.vFactor, textPaint);
				}
				
				// Lower part
				divisions = (int) Math.floor((dvport.vpPxY+dvport.vpPxHeight- dvport.baselinePxY) / (1000*dvport.vFactor));
				
				for (int i = 1; i <= divisions; i++) {
					canvas.drawText("" + (float) -i, left-2, dvport.baselinePxY+i*1000*dvport.vFactor, textPaint);
				}
			
			// Debug thingies
				textPaint.setTextAlign(Align.LEFT);
				canvas.drawText("" + dvport.areaOffset + " ~ " + dvport.lastOffset, left, top-10, textPaint);
				textPaint.setTextAlign(Align.RIGHT);
			
				canvas.drawText("FPS: " + fps, (left+right)/2, (top+bottom)/2, textPaint);
				
			// Aaaaand done!
            canvas.restore();
			}
		}
	}

	
	protected Viewport vport;
	protected DynamicViewport dvport;
	
	private Data data;
	
	public ECGView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		data = Data.getInstance();
		
		/*if (thread == null) {
			thread = new ECGThread(holder);
			thread.setDaemon(true);
		}*/
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		int w = (int) (getWidth()*0.95);
		int h = (int) (getHeight()*0.9);
		dvport = new DynamicViewport(w, h, 3.0f);
		dvport.setOnScreenPosition(30, 30);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		int w = (int) (getWidth()*0.95);
		int h = (int) (getHeight()*0.9);
		vport = new Viewport(w, h, 3.0f);
		vport.setOnScreenPosition(30, 30);
		vport.data = data.getSamplesArray();
		vport.dataStart = 0;
		vport.dataEnd = vport.data.length;
		
		dvport = new DynamicViewport(w, h, 3.0f);
		dvport.setOnScreenPosition(30, 30);
	
		try {
			if (!thread.isAlive()) {
				thread.setRunning(true);
				thread.start();
			}
		} catch (Exception e) {
			/*thread = new ECGThread(getHolder());
			thread.setRunning(true);
			thread.start();*/
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {}
		}
	}
	
	boolean holding;
	float holdStartX;
	float holdStartY;
	float holdEndX;
	float holdEndY;
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (holding) {
				holding = false; return true;
			}
			
			holding = true;
			holdStartX = event.getX();
			holdStartY = event.getY();
		}
		else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (!holding) return true;
			
			if (data.mode == Data.MODE_STATIC) {
				vport.move(-1*(event.getX() - holdStartX)/vport.vpPxWidth*vport.vaSeconds*0.5f);
				holdStartX = event.getX();
			}

			data.setDrawBaseHeight(data.getDrawBaseHeight()+(event.getY() - holdStartY)/vport.vpPxHeight);
			holdStartY = event.getY();
		}
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (!holding) return true;
			
			holding = false;
			holdEndX = event.getX();
			holdEndY = event.getY();
		}
		return true;
	}
	
	public void reset() {
		if (thread != null) {
			thread.setRunning(false);
			boolean retry = true;
			while (retry) {
				try {
					thread.join();
					retry = false;
					System.err.println("SE MORTO");
				} catch (InterruptedException e) {
				}
			}
		}

		if (data.mode == Data.MODE_STATIC)
			thread = new ECGThreadStatic(getHolder());
		else if (data.mode == Data.MODE_DYNAMIC)
			thread = new ECGThreadDynamic(getHolder());
		thread.setRunning(true);
		thread.start();
	}
};
