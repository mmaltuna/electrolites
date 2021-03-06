package org.microlites.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.microlites.data.DataHolder;

import android.os.Environment;
import android.text.format.Time;

public class RealTimeDataParser {
	enum Token {None, Sample, DPoint, Offset, HBR};	// Parsing progress tokens
	
	/* Parsing */
	Token currentToken;								// Current progress token
	byte currentByte;								// Temporal byte storage
	int progress;									// Current parsing progress
	byte[] storedBytes;								// Parsed bytes storage
	
	/* Logical */
	private int lastSample;							// Last parsed sample
	
	/* Log saving */
	private FileOutputStream output;				// Parsed log output 
	private FileOutputStream rawput;				// Raw log output
	
	/* Data receiver */
	private DataHolder dataHolder;
	
	public RealTimeDataParser(DataHolder holder) {
		// Prepare parsing
		// Store reference to data holder
		dataHolder = holder;
		
		// No samples have been parsed yet
		lastSample = 0;
		
		// Reset parsing status
		currentToken = Token.None;
		progress = 0;
	
		// Prepare log saving
		output = null;
		rawput = null;
		
		// Get External Storage State
		String state = Environment.getExternalStorageState();
		// External media should be mounted 
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// Fetch current time
			Time now = new Time();
			now.setToNow();

			// Create log and raw-log files with current timestamp name
			// Compute paths
			File root = Environment.getExternalStorageDirectory();
			File path = new File(root, "/Download/");
			File dir = new File(path, "log-" + now.format("%d%m-%Y_%H-%M") + ".txt");
			File rawDir = new File(path, "raw-" + now.format("%d%m-%Y_%H-%M") + ".txt");

			// Actual file creation
			try {
				output = new FileOutputStream(dir.getPath());
				rawput = new FileOutputStream(rawDir.getPath());
			} catch (IOException e) {
				System.err.println("Couldn't create log files");
				e.printStackTrace();
				output = null;
				rawput = null;
			}
		}
	}
	
	public void step(byte currentByte) {
		if (rawput != null) {
			try {
				rawput.write(currentByte);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		this.currentByte = currentByte;
		
		switch (currentToken) {
		case None:
			switch ((byte) (currentByte & 0x0ff)) {
			case (byte) 0xcc:
				// Start parsing an Offset
				currentToken = Token.Offset;
				break;
			case (byte) 0xda:
				// Start parsing a sample
				currentToken = Token.Sample;
				break;
			case (byte) 0xed:
				// Start parsing a delineation point
				currentToken = Token.DPoint;
				break;
			case (byte) 0xfb:
				currentToken = Token.HBR;
				break;
			default:
				currentToken = Token.None;
				System.out.print("Token unknown: ");
				System.out.println(((byte) currentByte & 0xff));
			}
			// Reset parsing data
			progress = 0;
			storedBytes = new byte[10];
			break;
		case Offset:
			parseOffset();
			break;
		case Sample:
			parseSample();
			break;
		case DPoint:
			parseDPoint();
			break;
		case HBR:
			parseHBR();
			break;
		default:
			currentToken = Token.None;
			break;
		}
	}
	
	public void parseOffset() {
		storedBytes[progress] = currentByte;
		progress++;
		if (progress >= 4) {
			
			int first =  storedBytes[0]	<< 24 & 0xFF000000;
			int second = storedBytes[1]	<< 16 & 0x00FF0000;
			int third =  storedBytes[2]	<< 8  & 0x0000FF00;
			int fourth = storedBytes[3]		  & 0x000000FF;
			
			int offset = first | second | third | fourth;

			if (offset != lastSample) {
				System.err.println("WRONG OFFSET: GOT " + offset + 
						" EXPECTED " + lastSample);
				//dataHolder.handleOffset(offset);
			}
			
			lastSample = offset;
			
			// Add a debug offset dpoint
			dataHolder.addDPoint(lastSample, DataHolder.DP_TYPE_START, 
					DataHolder.WAVE_OFFSET);
			
			if (output != null) {
				try {
					// output.write(0xcc);
                    // output.write(storedBytes, 0, 4);
					output.write("offset[".getBytes());
					output.write((""+offset+"]\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			currentToken = Token.None;
			progress = 0;
		}
	}
	
	public void parseSample() {
		storedBytes[progress] = (byte) (currentByte & 0xff);
		progress++;
		if (progress >= 2) {
			short sample = byteToShort(storedBytes[0], storedBytes[1]);
			
			dataHolder.addSample(lastSample, sample);
			
			if (output != null) {
				try {
					/*output.write(0xda);
                    output.write(storedBytes, 0, 2);*/
					output.write("sample[".getBytes());
					output.write((lastSample+","+sample+"]\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			lastSample++;
			currentToken = Token.None;
			progress = 0;			
		}
	}
	
	public void parseDPoint() {
		storedBytes[progress] = currentByte;//(byte) (currentByte & 0xff);
		progress++;
		if (progress >= 5) {
			int first =  storedBytes[1] << 24 & 0xFF000000;
			int second = storedBytes[2] << 16 & 0x00FF0000;
			int third =  storedBytes[3] << 8  & 0x0000FF00;
			int fourth = storedBytes[4]		  & 0x000000FF;
			
			int index = first | second | third | fourth;
			
			if (output != null) {
				try {
					/*output.write(0xed);
                    output.write(storedBytes, 0, 4);*/
					output.write("dpoint[".getBytes());
					output.write((index+"]\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (checkPointType(storedBytes[0]) != DataHolder.DP_TYPE_SPEAK)
				dataHolder.addDPoint(index, checkPointType(storedBytes[0]), checkWaveType(storedBytes[0]));
			
			currentToken = Token.None;
			progress = 0;
		}
	}
	
	public void parseHBR() {
		System.out.println("AN HBR!");
		storedBytes[progress] = (byte) (currentByte & 0xff);
		progress++;
		if (progress >= 2) {
			int first =  storedBytes[0] << 8 & 0x0000FF00;
			int second = storedBytes[1] 	 & 0x000000FF;
		
			int beatSamples = first | second;
			
			float hbr = 60*250/(float) beatSamples;
			System.out.println("HBR: " + hbr + "(" + beatSamples + ")");
			
			if (output != null) {
				try {
					/*output.write(0xfb);
                    output.write(storedBytes, 0, 2);*/
					output.write("hbr[".getBytes());
					output.write((hbr+"]\n").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			dataHolder.handleHBR(hbr);
			/*synchronized(data.dynamicData.mutex) {
				hbr = Math.round(hbr*100)/100.f;
				System.out.println("HBR: " + hbr);
				data.dynamicData.setHBR(hbr);
			}*/
			
			currentToken = Token.None;
			progress = 0;
		}
	}
	
	// Convierte dos bytes dados en su short correspondiente (en complemento a 2)
	public short byteToShort(byte b1, byte b2) {
		int i1 = b1;
		int i2 = b2;
		i1 &= 0xff;
		i2 &= 0xff;
		
		return (short) (i1*256 + i2);
	}
	
	public long byteToLong(byte b1, byte b2) {
		byte i1 = (byte) (b1 & 0xff);
		byte i2 = (byte) (b2 & 0xff);
		
		return (long) ((i1 * 256) + i2);
	}

	public void finish() {
		if (output != null)
			try {
				output.close();
				rawput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("-----------------------DPARSER OUT-------------------");
	}
	
	public short checkPointType(byte b) {
		switch (b & 0xf0) {
			case 1 << 4: return DataHolder.DP_TYPE_START;
			case 2 << 4: return DataHolder.DP_TYPE_PEAK;
			case 3 << 4: return DataHolder.DP_TYPE_END;
			case 4 << 4: return DataHolder.DP_TYPE_SPEAK;
		}
		
		return 0;
	}
	
	public short checkWaveType(byte i) {
		switch (i & 0x0f) {
			case 1: return DataHolder.WAVE_QRS;
			case 2: return DataHolder.WAVE_P;
			case 3: return DataHolder.WAVE_T;
		}
		
		return 0;
	}
}
