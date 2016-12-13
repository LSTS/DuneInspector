package pt.lsts;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuneTask {

	File filename;

	String name;

	// struct Task: public <TASK>
	String superClass;
	DuneTask superTask;
	HashSet<String> inputs = new HashSet<>();
	HashSet<String> outputs = new HashSet<>();;

	public DuneTask(File task) throws IOException {
		this.filename = task;
		this.name = getName(task);
		this.superClass = getType(task);
		this.inputs = getInputs(task);
		this.outputs = getOutputs(task);
	}

	private String getType(File f) throws IOException {
		List<String> lines = Files.readAllLines(f.toPath());
		String superType = "DUNE::Tasks::Task";
		for (String l : lines) {
			l = l.trim();
			if (l.contains("struct Task:")) {
				String[] parts = l.split(" ");
				superType = parts[parts.length-1];
			}
		}
		
		return superType.replaceAll("DUNE::", "").replaceAll("::", ".");
	}
	
	private String getName(File f) {
		ArrayList<String> names = new ArrayList<>();

		File dir = f.getParentFile();
		while (!dir.getName().equals("src")) {
			names.add(dir.getName());
			dir = dir.getParentFile();
		}
		Collections.reverse(names);
		return String.join(".", names);
	}

	private HashSet<String> getInputs(File f) throws IOException {
		List<String> lines = Files.readAllLines(f.toPath());
		HashSet<String> inputs = new HashSet<>();

		for (String l : lines) {
			l = l.trim();
			if (l.contains("bind<IMC::")) {
				l = l.substring(l.indexOf("bind<IMC::"));
				inputs.add(l.substring("bind<IMC::".length(), l.indexOf(">(this")));
			}
		}

		return inputs;
	}

	private HashSet<String> getOutputs(File f) throws IOException {
		List<String> lines = Files.readAllLines(f.toPath());
		HashSet<String> outputs = new HashSet<>();

		HashSet<String> dispatches = new HashSet<>();
		
		for (String l : lines) {
			l = l.trim();

			String prefix = "dispatch(";
			if (l.contains(prefix)) {

				l = l.substring(l.indexOf(prefix));
				l = l.substring(prefix.length());

				for (String delim : new String[] { ")", ",", "[" }) {
					if (l.contains(delim))
						l = l.substring(0, l.indexOf(delim));
				}

				l = l.replaceAll("&", "");
				dispatches.add(l.trim());
			}
		}
		Pattern p = Pattern.compile("IMC\\:\\:([\\w]*).*");

		for (String l : lines) {
			l = l.trim();
			if (l.startsWith("IMC::")) {
				for (String v : dispatches) {
					if (l.contains(v)) {
						Matcher m = p.matcher(l);
						if (m.matches())
							outputs.add(m.group(1));
					}
				}
			}
		}

		return outputs;

	}

	@Override
	public String toString() {
		return name + " : "+superClass+" {\n\tinputs: " + inputs + "\n\toutputs: " + outputs + "\n}\n";
	}
}
