package pt.lsts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class TaskListing implements Serializable {

	private static final long serialVersionUID = 1L;
	private File srcDir;
	private LinkedHashMap<File, DuneTask> tasks = new LinkedHashMap<>();
	
	//private LinkedHashMap<String, ArrayList<File> > producers = new LinkedHashMap<>();
	//private LinkedHashMap<String, ArrayList<File> > consumers = new LinkedHashMap<>();

	public LinkedHashMap<File, DuneTask> getTasks() {
		return tasks;
	}

	private static TaskListing instance;
	
	public static synchronized TaskListing instance() {
		if (instance == null) {
			try {
				System.out.println("TaskListing read from cache.");
				instance = read();
			}
			catch (Exception e) {
				System.out.println("Generating new TaskListing...");
				try {
					instance = rebuild(new File("/home/zp/workspace/dune/source"));	
				}
				catch (Exception ex) {
					ex.printStackTrace();
					return null;					
				}
				
			}
		}
		return instance;
	}
	
	public static TaskListing read() throws Exception {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream("tl.obj"));
			return (TaskListing) ois.readObject();
		} finally {
			if (ois != null)
				ois.close();
		}
	}

	public void write() throws Exception {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream("tl.obj"));
			oos.writeObject(this);
		} finally {
			if (oos != null)
				oos.close();
		}
	}

	public static TaskListing rebuild(File duneSrcDir) throws Exception {
		TaskListing tl = new TaskListing(duneSrcDir);
		tl.write();
		return tl;
	}

	public TaskListing(File duneSrcSir) {
		this.srcDir = duneSrcSir;
		ArrayList<File> tasks = listTasks(srcDir);

		for (File t : tasks) {
			try {
				this.tasks.put(t, new DuneTask(t));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Also process the inherited code (consumes / dispatches)
		ArrayList<DuneTask> toProcess = new ArrayList<>();
		toProcess.addAll(this.tasks.values());

		while (!toProcess.isEmpty()) {
			ArrayList<DuneTask> curProc = new ArrayList<>();
			curProc.addAll(toProcess);
			for (DuneTask t : curProc) {
				File f = resolveTaskFile(t.superClass);
				if (f == null || this.tasks.containsKey(f)) {
					toProcess.remove(t);
					t.superTask = this.tasks.get(f);
					if (f != null)
						t.superClass = t.superTask.name;
				} else {
					DuneTask parent = resolveTask(t.superClass);
					this.tasks.put(f, parent);
					toProcess.add(parent);
					toProcess.remove(t);
					t.superTask = parent;
					t.superClass = parent.name;
				}
			}
		}
	}

	private ArrayList<File> listTasks(File duneSource) {
		ArrayList<File> accum = new ArrayList<>();
		listTasks(duneSource, accum);
		return accum;
	}

	private void listTasks(File root, ArrayList<File> accum) {
		for (File f : root.listFiles()) {
			if (f.isDirectory()) {
				listTasks(f, accum);
			} else if (f.getName().equals("Task.cpp"))
				accum.add(f);
		}
	}

	public File resolveTaskFile(String taskName) {
		HashSet<File> roots = new HashSet<>();

		if (taskName.contains("/")) {
			taskName = taskName.substring(0, taskName.indexOf("/"));
		}

		roots.add(new File(srcDir, "src"));
		File core = new File(srcDir, "src/DUNE");
		roots.add(core);
		for (File f : core.listFiles()) {
			if (f.isDirectory())
				roots.add(f);
		}

		roots.add(new File(srcDir, "private/src"));

		String dir = taskName.replaceAll("\\.", "/");

		for (File r : roots) {
			String fname = r.getAbsolutePath() + "/" + dir + ".cpp";
			if (new File(fname).exists()) {
				return new File(fname);
			}
			fname = r.getAbsolutePath() + "/" + dir + "/Task.cpp";
			if (new File(fname).exists()) {
				return new File(fname);
			}
		}

		return null;
	}

	public DuneTask resolveTask(String taskName) {
		try {
			return new DuneTask(resolveTaskFile(taskName));
		} catch (Exception e) {
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		TaskListing tl = read();//rebuild(new File("/home/zp/workspace/dune/source"));
		
		System.out.println(tl.resolveTask("Tasks.Task"));
		
		/*
	Plan.Engine (/home/zp/workspace/dune/source/src/Plan/Engine/Task.cpp) : Tasks.Task {
	inputs: [ManeuverControlState, FuelLevel, VehicleState, EntityActivationState, PlanControl, EntityInfo, EstimatedState, VehicleCommand, RegisterManeuver]
	outputs: [PlanSpecification, PlanControl, VehicleCommand, LoggingControl, PlanControlState]
}
		 */
	}
}
