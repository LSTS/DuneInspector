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

	public static String classDiagUml(DuneTask dt) {
		StringBuilder sb = new StringBuilder();
		sb.append("@startuml\n");
		sb.append("Bob -> Alice : hello\n");
		sb.append("@enduml\n");

		return sb.toString();
	}
	
	public static void showUml(DuneTask dt) {
		String uml = classDiagUml(null);
		BufferedImage image = generateImage(uml);
		JLabel lbl = new JLabel(new ImageIcon(image));
		JFrame frm = new JFrame("test");
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setSize(800, 800);
		frm.getContentPane().add(lbl);
		frm.setVisible(true);
	}

	public static void main(String[] args) {
		showUml(null);
	}

}
