package com.cambeyer;

import java.util.ArrayList;

public class UserManager {
	
	public static ArrayList<UserObject> users = new ArrayList<UserObject>();
	
	public UserManager() {
	}
	
	public static void add(UserObject user) {
		users.add(user);
	}
	
	public static void remove(UserObject user) {
		users.remove(user);
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
	
	public static ArrayList<UserObject> getUsersByRadius(String lat, String lon, Integer radius) {
		ArrayList<UserObject> result = new ArrayList<UserObject>();
		for (int i = 0; i < users.size(); i++) {
			if 
		}
		return result;
	}
}