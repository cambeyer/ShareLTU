package com.cambeyer;

import java.util.ArrayList;

public class UserManager {
	
	public static ArrayList<UserObject> users = new ArrayList<UserObject>();
	
	public static void add(UserObject user) {
		remove(getUserByUUID(user.uuid));
		users.add(user);
	}
	
	public static void remove(UserObject user) {
		if (users.contains(user)) {
			users.remove(user);
		}
	}
	
	public static UserObject getUserByUUID(String uuid) {
		for (int i = 0; i < users.size(); i++) {
			if (users.get(i).uuid.equals(uuid)) {
				return users.get(i);
			}
		}
		return null;
	}
	
	public static UserObject getUserByRegID(String regid) {
		for (int i = 0; i < users.size(); i++) {
			if (users.get(i).regid.equals(regid)) {
				return users.get(i);
			}
		}
		return null;
	}
	
	public static UserObject getUserByName(String name) {
		for (int i = 0; i < users.size(); i++) {
			if (users.get(i).name.equals(name)) {
				return users.get(i);
			}
		}
		return null;
	}
	
	public static ArrayList<UserObject> getUsersByRadius(String lat, String lon, Double radius) {
		ArrayList<UserObject> result = new ArrayList<UserObject>();
		for (int i = 0; i < users.size(); i++) {
			if (distFrom(Double.parseDouble(lat), Double.parseDouble(lon), Double.parseDouble(users.get(i).lat), Double.parseDouble(users.get(i).lon)) <= radius) {
				result.add(users.get(i));
			}
		}
		return result;
	}
	
	private static double distFrom(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    return earthRadius * c;
	}
}