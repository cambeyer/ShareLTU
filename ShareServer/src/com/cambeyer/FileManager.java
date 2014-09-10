package com.cambeyer;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class FileManager {
	
	public static ArrayList<FileObject> files = new ArrayList<FileObject>();
	
	public static void add(FileObject file) {
		remove(getFile(file.fromuuid, file.touuid, file.filename));
		files.add(file);
	}
	
	public static void remove(FileObject file) {
		if (files.contains(file)) {
			files.remove(file);
		}
	}
	
	public static FileObject getFile(String fromuuid, String touuid, String filename) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).fromuuid.equals(fromuuid) && files.get(i).touuid.equals(touuid) && files.get(i).filename.equals(filename)) {
				return files.get(i);
			}
		}
		return null;
	}
	
	public static ArrayList<FileObject> getFilesByFromUUID(String fromuuid) {
		ArrayList<FileObject> result = new ArrayList<FileObject>();
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).fromuuid.equals(fromuuid)) {
				result.add(files.get(i));
			}
		}
		return result;
	}
	
	public static ArrayList<FileObject> getFilesByToUUID(String touuid) {
		ArrayList<FileObject> result = new ArrayList<FileObject>();
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).touuid.contains(touuid)) {
				result.add(files.get(i));
			}
		}
		return result;
	}
	
	public static void removeStaleFiles(String path) {
		for (int i = 0; i < files.size(); i++) {
	        Calendar cal1 = Calendar.getInstance();
	        cal1.setTime(files.get(i).timestamp);
	        Calendar cal2 = Calendar.getInstance();
	        cal2.setTime(new Date());
	        long daysBetween = 0;
	        while (cal1.before(cal2))
	        {
	            cal1.add(Calendar.DAY_OF_MONTH, 1);
	            daysBetween++;
	        }
	        
	        if (daysBetween >= 2) {		//delete files older than 2 days
	        	try {
	        		File file = new File(path + files.get(i).filename);
	        		file.delete();
		        	files.remove(i);
		        	i--;
	        	} catch (Exception ex) {
	        	}
	        }
		}
	}
}