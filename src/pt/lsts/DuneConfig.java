package pt.lsts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.ini4j.Ini;

public class DuneConfig {
	
	LinkedHashMap<String, String> activeTasks = new LinkedHashMap<>();
	
	public DuneConfig(File configFile) throws Exception {
		Ini ini = new Ini(configFile);
		
		for (String section : ini.keySet()) {
			String enabled = ini.get(section, "Enabled");
			if (enabled == null || enabled.equals("Never"))
				continue;
			if (section.contains("/")) {
				section = section.substring(0, section.indexOf("/"));
			}
			activeTasks.put(section, enabled);
		}
	}
	
	public Collection<String> activeTasks() {
		return activeTasks.keySet();
	}
	
	public String getProfile(String task) {
		return activeTasks.get(task);
	}
	
	public String getProfileLetter(String task) {
		String profile = activeTasks.get(task).toLowerCase(); 
		if (profile.contains("always"))
			return "A";
		if (!profile.contains("hardware") && profile.contains("simulation"))
			return "S";
		if (!profile.contains("simulation") && profile.contains("hardware"))
			return "H";
		if (profile.contains("simulation") && profile.contains("hardware"))
			return "A";
		return "N";
	}
	
	public static ArrayList<String> activatedTasks(File configFile, String profile) throws Exception {
		DuneConfig cfg = new DuneConfig(configFile);
		
		ArrayList<String> tasks = new ArrayList<>();

		for (Entry<String, String> ts : cfg.activeTasks.entrySet()) {
			if (ts.getValue().toLowerCase().contains(profile.toLowerCase()))
				tasks.add(ts.getKey());
		}
		
		return tasks;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(DuneConfig.activatedTasks(
				new File("/home/zp/Desktop/nop3/20161017/150604_teleoperation-mode/Config.ini"), "Hardware").size());
	}
}
