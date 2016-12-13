package pt.lsts;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class TaskListing {

	private File srcDir;
	private LinkedHashMap<File, DuneTask> tasks = new LinkedHashMap<>();
	
	public TaskListing(File duneSrcSir) {
		this.srcDir = duneSrcSir;
		ArrayList<File> tasks = listTasks(srcDir);
		
		for (File t : tasks) {
			try {
				this.tasks.put(t, new DuneTask(t));
			}
			catch (Exception e) {
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
				}
				else {
					DuneTask parent = resolveTask(t.superClass);
					this.tasks.put(f, parent);
					toProcess.add(parent);
					toProcess.remove(t);
					t.superTask = parent;
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
			}
			else if (f.getName().equals("Task.cpp"))
				accum.add(f);
		}
	}
	
	private File resolveTaskFile(String taskName) {
		HashSet<File> roots = new HashSet<>();
		roots.add(new File(srcDir,"src"));
		File core = new File(srcDir, "src/DUNE");
		roots.add(core);
		for (File f : core.listFiles()) { 
			if (f.isDirectory())
				roots.add(f);
		}
		
		roots.add(new File(srcDir, "private/src"));
	
		String dir = taskName.replaceAll("\\.", "/");
		
		for (File r : roots) {
			String fname = r.getAbsolutePath()+"/"+dir+".cpp";
			if (new File(fname).exists()) {
				return new File(fname);
			}
			fname = r.getAbsolutePath()+"/"+dir+"/Task.cpp";
			if (new File(fname).exists()) {
				return new File(fname);
			}
		}
		
		return null;
	}
	
	private DuneTask resolveTask(String taskName) {
		try {
			return new DuneTask(resolveTaskFile(taskName));	
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public static void main(String[] args) throws IOException {
		TaskListing tl = new TaskListing(new File("/home/zp/workspace/dune/source"));
		for (DuneTask t : tl.tasks.values()) {
			System.out.println(t);
		}
	}
}
