package pt.lsts;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sourceforge.plantuml.SourceStringReader;

public class DuneUmlGenerator {

	public static BufferedImage generateImage(String uml) {
		try {
			File tmp = File.createTempFile("duneumlgen", "png");
			SourceStringReader reader = new SourceStringReader(uml);
			reader.generateImage(tmp);
			return ImageIO.read(tmp);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String classUml(DuneTask dt) {
		StringBuilder sb = new StringBuilder();
		if (dt.filename.getAbsolutePath().contains("DUNE"))
			sb.append("class " + dt.name.replaceAll("\\.", "::") + " {\n");
		else
			sb.append("class " + dt.name.replaceAll("\\.", "::") + " << (T,#CC77CC) >> {\n");
		for (String input : dt.inputs) {
			sb.append("  ~consume(" + input + ")\n");
		}
		sb.append("---\n");
		for (String output : dt.outputs) {
			sb.append("  +dispatch(): " + output + "\n");
		}

		sb.append("}\n\n");
		return sb.toString();
	}

	public static String classDiagUml(DuneTask dt) {
		StringBuilder sb = new StringBuilder();
		sb.append("@startuml\n");
		sb.append(classUml(dt));

		DuneTask current = dt;
		String parent = dt.superClass;
		while (parent != null) {
			DuneTask p = TaskListing.instance().resolveTask(parent);
			if (p != null) {
				p.name = parent;
				sb.append(classUml(p));
				sb.append(parent.replaceAll("\\.", "::") + " <|-- " + current.name.replaceAll("\\.", "::") + "\n");
				current = p;
				parent = p.superClass;
			} else
				break;
		}

		sb.append("@enduml\n");

		return sb.toString();
	}

	public static void showUml(DuneTask dt) {
		String uml = classDiagUml(dt);
		BufferedImage image = generateImage(uml);
		JLabel lbl = new JLabel(new ImageIcon(image));
		JFrame frm = new JFrame("UML for " + dt.name);
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setSize(800, 800);
		frm.getContentPane().add(lbl);
		frm.setVisible(true);
	}
	
	public static void generateClassDiagramImages() throws Exception {
		for (DuneTask dt : TaskListing.instance().getTasks().values()) {
			BufferedImage img = generateImage(classDiagUml(dt));
			ImageIO.write(img, "PNG", new File(dt.name + ".png"));
			System.out.println("Wrote "+dt.name + ".png");
		}
	}

	public static void main(String[] args) throws Exception {
		//DuneTask dt = new DuneTask(new File("/home/zp/workspace/dune/source/src/Control/UAV/Ardupilot/Task.cpp"));
		
		//DuneTask dt = TaskListing.read().resolveTask("Control.UAV.Ardupilot");
		//System.out.println(classDiagUml(dt));
		//showUml(dt);
		generateClassDiagramImages();
	}

}
