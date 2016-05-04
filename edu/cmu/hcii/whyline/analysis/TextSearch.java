package edu.cmu.hcii.whyline.analysis;

import java.util.*;

import edu.cmu.hcii.whyline.source.JavaSourceFile;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.source.ParseException;
import edu.cmu.hcii.whyline.source.Token;
import edu.cmu.hcii.whyline.trace.JDKSource;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.util.IntegerVector;

/**
 * @author Andrew J. Ko
 *
 */
public class TextSearch implements SearchResultsInterface {

	public enum Mode {
		
//		FILE("search this file"),
		FILES("search my source"),
		ALL("search everything"),
		;
		
		public final String label;
		
		private Mode(String name) { this.label = name; }
		public String toString() { return label; }

	}
	
	private final String query;
	private final Mode mode;
	private final WhylineUI whylineUI;
	private boolean done = false;
	
	private int filesRemaining;
	
	private final Set<Token> matches = new HashSet<Token>();

	public TextSearch(WhylineUI whyUI, String queryText, Mode searchMode) {

		this.whylineUI = whyUI;
		this.query = queryText.toLowerCase(Locale.ENGLISH);
		this.mode = searchMode;
		
		filesRemaining = whylineUI.getTrace().getNumberOfUserSourceFiles();
		if(mode == Mode.ALL)
			filesRemaining += JDKSource.getNumberOfSourceFilesKnown(); 		
		
		Thread search = new Thread() {
			public void run() {

				for(JavaSourceFile source : whylineUI.getTrace().getAllSourceFiles())
					check(source);
			
				if(mode == Mode.ALL)
					for(JavaSourceFile source : JDKSource.getAllSource())
						check(source);
				
				done = true;
				
			}
			
			private void check(JavaSourceFile source) {
				synchronized(matches) {

					try {
						String string = source.getSourceAsString();
						
						String lower = string.toLowerCase(Locale.ENGLISH);
						IntegerVector matchIndices = new IntegerVector(3);
						int index = 0;
						// Find the character index of all matches.
						while(index >= 0) {
							index = lower.indexOf(query, index);
							if(index > 0) {
								matchIndices.append(index);
								index++;	// Jump to the next character to start searching.
							}
						}

						// Find all tokens intersecting these ranges
						Line[] lines = source.getLines();
						if(lines != null) {
							int charIndex = 0; 
							for(Line line : lines) {
								for(Token token : line.getTokens()) {
									// Does this token's character range contain any of the match indices?
									for(int i = 0; i < matchIndices.size(); i++) {
										int matchIndex = matchIndices.get(i);
										if(matchIndex >= charIndex && matchIndex <= charIndex + token.getText().length())
											matches.add(token);
									}
									charIndex += token.getText().length();
								}
							}
						}
						
					} catch (ParseException e1) {
					}

					filesRemaining--;
				}
			}

		};
		
		search.start();
		
	}
	
	public String getResultsDescription() { return "text search for \"" + query + "\""; }

	public String getCurrentStatus() { return filesRemaining <= 0 ? "Done searching." : "" + filesRemaining + " files left to search."; }
	
	public SortedSet<Token> getResults() { synchronized(matches) { return new TreeSet<Token>(matches); } }
	
	public boolean isDone() { return done; }

}
