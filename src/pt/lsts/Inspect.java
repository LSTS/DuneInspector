package pt.lsts;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessageType;

public class Inspect {

	@Option(name = "-dsrc", usage = "DUNE source folder used to generate Task UML.", required = false)
	private File OptionDuneSrc = null;

	@Option(name = "-msg", usage = "Message for which to generate UML.", required = false)
	private String OptionMessage = null;

	@Option(name = "-comms", usage = "Communications diagram for given DUNE task or IMC message.", required = false)
	private String OptionComms = null;
	
	@Option(name = "-task", usage = "Class diagram for given DUNE task.", required = false)
	private String OptionTaskUml = null;
	
	@Option(name = "-ini", usage = "INI file from where to read running configuration.", required = false)
	private File OptionIniFile = null;

	@Option(name = "-debug", usage = "Output debug in case of errors.", required = false)
	private boolean OptionDebug = false;

	@Option(name = "-o", usage = "Where to write resulting image.", required = false)
	private File OptionOutput = null;
	
	@Option(name = "-p", usage = "Profile to use when listing tasks.", required = false)
	private String OptionProfile = null;
	
	@Option(name = "-uml", usage = "Load previously generated UML.", required = false)
	private File OptionUml = null;

	
	private String uml = null;
	private File lastDir = new File(".");
	private JFrame frm = null;
	private JLabel lbl;
	
	private void showUml(final String uml) {
		this.uml = uml;

		if (OptionOutput != null) {
			SourceStringReader reader = new SourceStringReader(uml);
			try {
				reader.generateImage(OptionOutput);
			} catch (Exception e) {
				System.err.println("Error generating UML image: " + e.getMessage());
				if (OptionDebug)
					e.printStackTrace();
			}
			return;
		}

		frm = new JFrame("UML Inspector");
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setSize(800, 600);
		frm.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frm.getContentPane().setBackground(Color.white);

		BufferedImage img = UmlGenerator.generateImage(uml);
		lbl = new JLabel(new ImageIcon(img));
		frm.getContentPane().add(new JScrollPane(lbl));
		JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu("Save as...");
		menu.add("PNG Image").addActionListener(this::savePng);
		menu.add("SVG Image").addActionListener(this::saveSvg);
		menu.add("Text").addActionListener(this::saveText);
		menu.addSeparator();
		menu.add("Exit").addActionListener(this::exit);
		menubar.add(menu);
		frm.setJMenuBar(menubar);
		frm.setVisible(true);
	}

	private void exit(ActionEvent evt) {
		System.exit(0);
	}
	
	private File selectFile(String description, String... extensions) {
		JFileChooser chooser = new JFileChooser(lastDir);
		chooser.setDialogTitle(description);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
		int op = chooser.showSaveDialog(frm);
		if (op == JFileChooser.APPROVE_OPTION) {
			lastDir = chooser.getSelectedFile();
			return chooser.getSelectedFile();
		}
		return null;
	}

	private void savePng(ActionEvent evt) {

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				File f = selectFile("PNG Images", "png");
				if (f != null) {
					SourceStringReader reader = new SourceStringReader(uml);
					try {
						reader.generateImage(f);
						JOptionPane.showMessageDialog(frm, "File saved.");
					} catch (Exception e) {
						System.err.println("Error generating UML image: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
				}
				return null;
			}
		};

		worker.execute();
	}

	private void saveSvg(ActionEvent evt) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				File f = selectFile("SVG Images", "svg");
				if (f != null) {
					SourceStringReader reader = new SourceStringReader(uml);
					try {
						reader.generateImage(new FileOutputStream(f), new FileFormatOption(FileFormat.SVG));
						JOptionPane.showMessageDialog(frm, "File saved.");
					} catch (Exception e) {
						System.err.println("Error generating SVG image: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
				}
				return null;
			}
		};

		worker.execute();
	}

	private void saveText(ActionEvent evt) {
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				File f = selectFile("Text files", "txt", "uml");
				if (f != null) {
					try {
						Files.write(f.toPath(), uml.getBytes());
						JOptionPane.showMessageDialog(frm, "File saved.");
					} catch (Exception e) {
						System.err.println("Error generating Text file: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
				}
				return null;
			}
		};

		worker.execute();
	}

	@SuppressWarnings("deprecation")
	public void doMain(String[] args) throws IOException {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);

			if (OptionDuneSrc != null) {
				if (!OptionDuneSrc.isDirectory())
					throw new CmdLineException(parser, "The provided DUNE source folder is invalid");
				else {
					try {
						TaskListing.rebuild(OptionDuneSrc);
					} catch (Exception e) {
						System.err.println("Error parsing DUNE source: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
				}
			}

			if (OptionIniFile != null) {
				if (!OptionIniFile.canRead())
					throw new CmdLineException(parser, "The ini file provided is invalid");
				else {
					try {
						new DuneConfig(OptionIniFile);

					} catch (Exception e) {
						System.err.println("Error parsing DUNE configuration: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
				}
			}

			int count = 0;
			if (OptionTaskUml != null)
				count++;
			
			if (OptionMessage != null)
				count++;
			
			if (OptionUml != null)
				count++;
			
			if (OptionComms != null)
				count++;
			
			if (OptionIniFile != null)
				count++;

			if (count == 0)
				throw new CmdLineException(parser, "You have to select an option!!");

			if (count > 1)
				throw new CmdLineException(parser, "You can only use one of (-ini, -task, -comm, -msg, -uml)");

			if (OptionMessage != null) {
				if (OptionMessage.equalsIgnoreCase("all")) {
					try {
						UmlGenerator.msgUmlImages();
					} catch (Exception e) {
						System.err.println("Error generating UML for messages: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
					return;
				} 
				else if (OptionMessage.equalsIgnoreCase("list")) {
					ArrayList<String> msgs = new ArrayList<>();
					msgs.addAll(IMCDefinition.getInstance().getMessageNames());
					Collections.sort(msgs);
					msgs.forEach(it -> {
						System.out.println(" * " + it);
					});
					System.out.println(" * All (all messages ouput as PNG).");
					return;
				} 
				else {
					IMCMessageType type = IMCDefinition.getInstance().getType(OptionMessage);

					if (type == null) {
						throw new CmdLineException(parser,
								"Message name (-msg) provided is unknown use '-msg list' for a list of valid messages.");
					}

					String uml = UmlGenerator.messageUml(type);
					showUml(uml);
				}
			} 
			else if (OptionComms != null) {
				Object obj = TaskListing.instance().resolveTask(OptionComms);
				
				if (obj == null)
					obj = IMCDefinition.getInstance().getType(OptionComms);

				if (obj == null) {
					throw new CmdLineException(parser,
							"Name provided is unknown. Use '-task list' or '-msg list' for a valid names.");
				}

				String uml = UmlGenerator.interactionUml(obj);
				showUml(uml);
				
			}
			if (OptionTaskUml != null) {
				if (OptionTaskUml.equalsIgnoreCase("all")) {
					try {
						UmlGenerator.taskUmlImages();
					} catch (Exception e) {
						System.err.println("Error generating UML for tasks: " + e.getMessage());
						if (OptionDebug)
							e.printStackTrace();
					}
					return;
				}
				else if (OptionTaskUml.equalsIgnoreCase("list")) {
					ArrayList<String> tasks = new ArrayList<>();

					TaskListing.instance().getTasks().values().forEach(it -> {
						tasks.add(it.name);
					});
					Collections.sort(tasks);
					tasks.forEach(it -> {
						System.out.println(" * " + it);
					});
					System.out.println(" * All (all tasks ouput as PNG).");
					return;
				} 
				else {
					DuneTask dt = TaskListing.instance().resolveTask(OptionTaskUml);

					if (dt == null) {
						throw new CmdLineException(parser,
								"Task name (-task) provided is unknown use '-task list' for a list of valid tasks.");
					}

					String uml = UmlGenerator.taskUml(dt);
					showUml(uml);
				}
			}
			else if (OptionIniFile != null) {
				try {
					String uml = UmlGenerator.configUml(OptionIniFile, OptionProfile);
					showUml(uml);
				} catch (Exception e) {
					System.err.println("Error generating UML for tasks: " + e.getMessage());
					if (OptionDebug)
						e.printStackTrace();
				}

			}
			else if (OptionUml != null) {
				try {
					String uml = new String(Files.readAllBytes(OptionUml.toPath()));
					showUml(uml);
				}
				catch (Exception e) {
					System.err.println("Error loading UML file: " + e.getMessage());
					if (OptionDebug)
						e.printStackTrace();
				}
			}
		} catch (CmdLineException e) {
			System.err.println("" + e.getMessage() + ". Valid Arguments:");
			parser.printUsage(System.err);
			return;
		}
	}

	public static void main(String[] args) throws IOException {
		new Inspect().doMain(args);		
	}
}
