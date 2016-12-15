package pt.lsts;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuneTask implements Serializable {

	private static final long serialVersionUID = 6659667315202122191L;
	File filename;
	String name;
	String superClass;
	DuneTask superTask;
	HashSet<String> inputs = new HashSet<>();
	HashSet<String> outputs = new HashSet<>();;

	transient ArrayList<File> includedFiles = new ArrayList<>();

	public DuneTask(File task) throws IOException {
		this.filename = task;
		this.name = getName(task);
		this.superClass = getType(task);
		this.inputs = getInputs(task);
		this.outputs = getOutputs(task);
	}

	public HashSet<String> inputs() {
		HashSet<String> ret = new HashSet<>();
		ret.addAll(inputs);
		if (superTask != null)
			ret.addAll(superTask.inputs());

		return ret;
	}

	public HashSet<String> outputs() {
		HashSet<String> ret = new HashSet<>();
		ret.addAll(outputs);
		if (superTask != null)
			ret.addAll(superTask.outputs());

		return ret;
	}

	private File getSrcDir() {
		File f = filename;
		while (!f.getName().equals("src"))
			f = f.getParentFile();
		return f;
	}

	private List<String> getCode(File f) throws IOException {
		List<String> originalLines = Files.readAllLines(f.toPath());
		ArrayList<String> lines = new ArrayList<>();

		Iterator<String> it = originalLines.iterator();

		while (it.hasNext()) {
			String line = it.next();

			if (line.startsWith("#include <") && line.endsWith("hpp>")) {
				String l = line.substring("#include <".length());
				l = l.substring(0, l.indexOf(">"));
				File included = new File(getSrcDir(), l);
				if (included.exists() && !includedFiles.contains(included)) {
					includedFiles.add(included);
					lines.addAll(getCode(included));
				}
			}
			if (line.trim().endsWith(":")) {
				line += " " + it.next();
			}
			lines.add(line);
		}

		return lines;
	}

	private String getType(File f) throws IOException {

		String superType = "";
		for (String l : getCode(f)) {
			l = l.trim();
			if (l.contains("struct Task")) {
				if (l.contains(",")) {
					l = l.substring(0, l.indexOf(","));
				}
				if (l.trim().endsWith(":")) {
					superType = "Tasks::Task";
				} else {
					String[] parts = l.split(" ");
					superType = parts[parts.length - 1];
				}
			} else if (l.contains("class") && l.contains("public")) {
				String[] parts = l.split(" ");
				superType = parts[parts.length - 1];
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
		if (!f.getName().equals("Task.cpp")) {
			names.add(f.getName().replaceAll("\\.cpp", ""));
		}
		return String.join(".", names).replaceAll("DUNE.", "");
	}

	private HashSet<String> getInputs(File f) throws IOException {
		List<String> lines = getCode(f);
		HashSet<String> inputs = new HashSet<>();

		for (String l : lines) {
			l = l.trim();
			if (l.contains("bind<")) {
				l = l.replaceAll("IMC::", "");
				l = l.replaceAll("\\w*Task\\w*,\\w*", "");
				l = l.substring(l.indexOf("bind<")).trim();
				try {
					inputs.add(l.substring("bind<".length(), l.indexOf(">")));
				}

				catch (Exception e) {
					System.out.println(l);
					e.printStackTrace();
				}
			}
		}

		return inputs;
	}

	private HashSet<String> getOutputs(File f) throws IOException {
		List<String> lines = getCode(f);
		HashSet<String> outputs = new HashSet<>();
		HashSet<String> dispatches = new HashSet<>();

		for (String l : lines) {
			l = l.trim();
			String prefix = "dispatch(";
			l = l.replaceAll("Task::dispatch", "");
			l = l.replaceAll("m_ctx\\.mbus\\.dispatch\\(", "");
			if (l.contains(prefix)) {
				l = l.substring(l.indexOf(prefix));
				l = l.substring(prefix.length());

				for (String delim : new String[] { ")", ",", "[" }) {
					if (l.contains(delim))
						l = l.substring(0, l.indexOf(delim));
				}

				l = l.replaceAll("&", "");
				dispatches.add(l.trim());
			} else if (l.contains("dispatchReply(")) {
				l = l.replaceAll("[\\*&\\(\\),;\\[\\]]+", " ");
				l = l.replaceAll("DF_KEEP_SRC_EID", "");
				String[] parts = l.split(" ");
				dispatches.add(parts[parts.length - 1].trim());
			}

		}
		Pattern p = Pattern.compile("IMC\\:\\:([\\w]*).*");

		for (String l : lines) {
			l = l.trim();
			if (l.startsWith("IMC::") || l.startsWith("DUNE::IMC::")) {
				List<String> tokens = Arrays.asList(l.replaceAll("[:;=\\*\\{\\}\\]\\(\\)]", " ").split(" "));
				for (String v : dispatches) {
					if (l.contains("iterator"))
						continue;
					if (tokens.contains(v)) {
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
		return name + " (" + filename + ") : " + superClass + " {\n\tinputs: " + inputs + "\n\toutputs: " + outputs
				+ "\n}\n";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DuneTask) {
			return filename.getAbsolutePath().equals(((DuneTask)obj).filename.getAbsolutePath());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return filename.getAbsolutePath().hashCode();
	}

	public static void main(String[] args) throws Exception {
		System.out.println(new DuneTask(new File("/home/zp/workspace/dune/source/src/Control/UAV/Ardupilot/Task.cpp")));
	}
}
