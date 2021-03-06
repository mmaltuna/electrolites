package com.electrolites.services;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.util.Log;

import com.electrolites.util.DPoint;
import com.electrolites.util.ExtendedDPoint;
import com.electrolites.util.FileConverter;
import com.electrolites.util.SamplePoint;
import com.electrolites.util.StaticFriendlyDataParser;

public class FileParserService extends DataService {
	public static final String TAG = "FileParserService";
	public static final boolean DEBUG = true;
	
	private FileConverter fc;
	private StaticFriendlyDataParser dp;
	
	private ArrayList<Byte> stream;
	private ArrayList<SamplePoint> samples;
	private ArrayList<ExtendedDPoint> dpoints;
	private HashMap<Integer, Short> hbrs;
	private int offset;
	
	public FileParserService() {
		super("FileParserService");
		
		fc = new FileConverter();
		stream = fc.readBinary(data.staticData.toLoad);
		
		dp = new StaticFriendlyDataParser();
		dp.setStream(stream);
	}
	
	@Override
	public void startRunning(Intent intent) {
		if (DEBUG)
			Log.d(TAG, "FileParserService starts.");
	}
	
	@Override
	public void retrieveData(Intent intent) {
		synchronized(this) {
			data.loading = true;
			
			if (data.staticData.samples != null)
				data.staticData.samples.clear();
			else
				data.staticData.samples = new ArrayList<SamplePoint>();
			
			if (data.staticData.dpoints != null)
				data.staticData.dpoints.clear();
			else
				data.staticData.dpoints = new ArrayList<ExtendedDPoint>();
			
			data.offset = 0;
		}
		
		dp.parseStream();
		samples = dp.getSamples();
		dpoints = dp.getDPoints();
		hbrs = dp.getHBRs();
		offset = dp.getOffset();
		
		synchronized(this) {
			data.staticData.samples.addAll(samples);
			data.staticData.dpoints.addAll(dpoints);
			data.offset = offset;
			data.loading = false;
		}
	}
}
