package pt.lsts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sourceforge.plantuml.SourceStringReader;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessageType;

/**
 * This class is used to generate UML diagrams for DUNE and IMC
 * @author zp
 */
public class UmlGenerator {

	/**
	 * Parses given UML and generates an Image using PlantUML 
	 * @param uml The String containing the UML
	 * @return Resulting image
	 */
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

	private static String messageUml(IMCMessageType msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("@startuml\n");
		sb.append(msgUml(msg, new HashSet<>()));
		sb.append("@enduml\n");
		return sb.toString();
	}

	private static String msgUml(IMCMessageType msg, HashSet<String> msgs) {
		StringBuilder sb = new StringBuilder();
		HashSet<String> thisMsgs = new HashSet<>();
		HashSet<String> thisMsgLists = new HashSet<>();
		IMCMessageType header = IMCDefinition.getInstance().getHeaderType();

		if (msg == header)
			sb.append("class Message << (A,#CC77CC) >> {\n");
		else if (!IMCDefinition.getInstance().getConcreteMessages().contains(msg.getShortName())) {
			sb.append("class " + msg.getShortName() + " << (A,#CC77CC) >> {\n");
		}
		else {
			sb.append("class " + msg.getShortName() + " {\n");
		}

		for (String field : msg.getFieldNames()) {
			String type = msg.getFieldType(field).getTypeName();
			msg.getFieldPossibleValues(field);
			if (msg.getFieldPossibleValues(field) != null) {
				type = msg.getFieldPrefix(field) + "_" + field.toUpperCase();
			} else if (msg.getFieldSubtype(field) != null) {
				type = msg.getFieldSubtype(field);
				if (msg.getFieldType(field).getTypeName().equals("message-list")) {
					thisMsgLists.add(type);
					type = msg.getFieldSubtype(field) + "[]";
				} else
					thisMsgs.add(type);
			} else if (msg.getFieldType(field).getTypeName().equals("message-list")) {
				thisMsgLists.add("Message");
				type = "Message[]";
			} else if (msg.getFieldType(field).getTypeName().equals("message")) {
				type = "Message";
				thisMsgs.add(type);
			}

			sb.append(" +" + field + ": " + type + "\n");
		}

		sb.append("}\n\n");

		HashSet<String> msgsToAdd = new HashSet<>();
		String superType = msg.getSupertype() != null ? msg.getSupertype().getShortName() : "Message";
		msgsToAdd.addAll(thisMsgLists);
		msgsToAdd.addAll(thisMsgs);
		msgsToAdd.add(superType);
		msgsToAdd.removeAll(msgs);
		
		while (!msgsToAdd.isEmpty()) {
			String m = msgsToAdd.iterator().next();
			msgs.add(m);
			if (m.equals("Message"))
				sb.append(msgUml(IMCDefinition.getInstance().getHeaderType(), msgs));
			else
				sb.append(msgUml(IMCDefinition.getInstance().getType(m), msgs));			
			msgsToAdd.removeAll(msgs);
		}
		
		for (String m : thisMsgLists)
			sb.append(msg.getShortName() + " *-- \"*\"" + m + "\n");

		for (String m : thisMsgs)
			sb.append(msg.getShortName() + " *-- \"1\" " + m + "\n");

		if (msg != header)
			sb.append(superType + " <|-- " + msg.getShortName() + "\n");

		return sb.toString();
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

	public static String taskUml(DuneTask dt) {
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
	
	/**
	 * Parse the provided UML and show the result on the screen
	 * @param uml The UML to be parsed
	 */
	public static void showUml(String uml) {
		BufferedImage image = generateImage(uml);
		JLabel lbl = new JLabel(new ImageIcon(image));
		JFrame frm = new JFrame("UML");
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setSize(800, 800);
		frm.getContentPane().add(lbl);
		frm.setVisible(true);
	}

	/**
	 * Generates PNG images for all cached DUNE tasks. 
	 * @throws Exception In case of disk writting errors
	 * @see TaskListing
	 */
	public static void taskUmlImages() throws Exception {
		for (DuneTask dt : TaskListing.instance().getTasks().values()) {
			BufferedImage img = generateImage(taskUml(dt));
			ImageIO.write(img, "PNG", new File(dt.name + ".png"));
		}
	}

	/**
	 * Generates PNG images for all known IMC Messages
	 * @throws Exception In case of disk writting errors
	 * @see IMCDefinition
	 */
	public static void msgUmlImages() throws Exception {
		for (String msg : IMCDefinition.getInstance().getMessageNames()) {
			IMCMessageType msgType = IMCDefinition.getInstance().getType(msg);

			BufferedImage img = generateImage(messageUml(msgType));
			ImageIO.write(img, "PNG", new File(msg + ".png"));
			System.out.println("Generated "+msg+".png");
		}
	}

	public static void main(String[] args) throws Exception {
		// DuneTask dt = new DuneTask(new
		// File("/home/zp/workspace/dune/source/src/Control/UAV/Ardupilot/Task.cpp"));

		// DuneTask dt =
		// TaskListing.read().resolveTask("Control.UAV.Ardupilot");
		// System.out.println(classDiagUml(dt));
		// showUml(dt);
		// taskUmlImages();

		//String uml = messageUml(IMCDefinition.getInstance().getType("LogBookControl"));
		//System.out.println(uml);
		//showUml(uml);

		msgUmlImages();
	}

}
