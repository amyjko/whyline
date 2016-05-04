package edu.cmu.hcii.whyline.ui.events;

import edu.cmu.hcii.whyline.source.FileInterface;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.ParseException;
import edu.cmu.hcii.whyline.trace.Serializer;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public class FileNavigation extends UndoableUIEvent<FileInterface>{

	public FileNavigation(FileInterface e, String ui, boolean userInitiated) { super(e, ui,userInitiated); }

	public FileNavigation(Trace trace, String[] args) {
		super(trace, args);
	}

	public void select(WhylineUI whylineUI) {
		
		whylineUI.selectFile(entity, false, UI.BACK_UI);
		
	}

	protected String getParsableStringArguments() { return Serializer.fileToString(entity); }
	protected UIEventKind getParsableStringKind() { return UIEventKind.FILE_NAVIGATION; }

	protected FileInterface parseEntity(String[] args) {
		
		return trace.getSourceByQualifiedName(args[FIRST_ARGUMENT_INDEX]);
		
	}

	public Line getCorrespondingLine(Trace trace) throws ParseException {

		return entity.getLine(1);
		
	}

}
