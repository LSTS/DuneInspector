package pt.lsts;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
	

	
	
	public static void main(String[] args) throws IOException {
		TaskListing tl = new TaskListing(new File("/home/zp/workspace/dune/source"));
		System.out.println(tl.tasks);
		
	}
}
