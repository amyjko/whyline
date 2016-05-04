package edu.cmu.hcii.whyline.ui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.JavaSourceFile;
import edu.cmu.hcii.whyline.ui.components.*;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class DocumentationUI extends HeadlinedPanel {

	private JEditorPane doc;
	
	public DocumentationUI(WhylineUI whylineUI) {
		
		super("documentation", whylineUI);

		doc = new JEditorPane("text/html", "");
		// This makes the pane respect the background, foreground, font.
		doc.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		doc.setBackground(UI.getControlBackColor());
		doc.setForeground(UI.getControlTextColor());
		doc.setOpaque(true);
		doc.setFont(UI.getMediumFont());
		doc.setEditable(false);
		
		WhylineScrollPane pane = new WhylineScrollPane(doc);
		pane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		pane.setBorder(new WhylineControlBorder());
		
		setContent(pane);

		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), 0));
		
		doc.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
					Util.openURL(e.getURL().toString());
			}
		});
		
	}
	
	private String getMethodDescription(MethodInfo method) {
		
		CodeAttribute code = method.getCode();
		JavaSourceFile source = code == null ? null : code.getClassfile().getSourceFile();
		String javadoc = source == null ? null : source.getMethodJavaDoc(method);

		if(javadoc == null)
			javadoc = "No documentation.";
		
		javadoc = javadoc.replace("/**", "");
		javadoc = javadoc.replace("*/", "");
		int firstTagIndex = javadoc.indexOf("* @");
		if(firstTagIndex > 0) javadoc = javadoc.substring(0, firstTagIndex);
		javadoc = javadoc.replace("*", "");		

		return 
			"<b><font size=+1>" + method.getJavaName() + "()</font></b>" +
			"<br><br>" +
			javadoc;
		
	}

	private String getClassDescription(Classfile classfile) {

		JavaSourceFile source = classfile.getSourceFile();
		String javadoc = source == null ? null : source.getClassJavaDoc(classfile);

		if(javadoc == null)
			javadoc = "No documentation.";
		
		javadoc = javadoc.replace("/**", "");
		javadoc = javadoc.replace("*/", "");
		int firstTagIndex = javadoc.indexOf("* @");
		if(firstTagIndex > 0) javadoc = javadoc.substring(0, firstTagIndex);
		javadoc = javadoc.replace("*", "");		

		return 
			"<font size=+1><b>" + classfile.getSimpleName() + "</b></font>" +
			"<br><br>" +
			javadoc;

	}

	public void showMethod(MethodInfo method) {

		if(!whylineUI.isStaticInfoShowing()) return;
		
		showHTML(getMethodDescription(method) + "<br><br>" + getClassDescription(method.getClassfile()));
		
	}

	public void showInstruction(Instruction inst) {

		if(!whylineUI.isStaticInfoShowing()) return;

		if(inst == null) return;
		
		showMethod(inst.getMethod());
		
	}

	public void showClass(Classfile subject) {

		if(!whylineUI.isStaticInfoShowing()) return;

		showHTML(getClassDescription(subject));

	}

	private void showHTML(String text) {
		
		doc.setText("<html>" + text + "</html>");
		doc.setCaretPosition(0);
		
		if(doc.getDocument() instanceof HTMLDocument) {
			HTMLDocument document = (HTMLDocument)doc.getDocument();
			Color color = UI.getHighlightColor();
			document.getStyleSheet().addRule("a { color:rgb(" +color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "); }");
		}
		
	}

}
