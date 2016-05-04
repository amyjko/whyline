package edu.cmu.hcii.whyline.ui;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.Whyline;
import edu.cmu.hcii.whyline.source.Line;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.ui.events.AbstractUIEvent;
import edu.cmu.hcii.whyline.util.Util;
import gnu.trove.TLongHashSet;

/**
 * User interface and usage information stored for a saved trace.
 * 
 * @author Andrew J. Ko
 *
 */
public class PersistentState {

	private final WhylineUI whylineUI;
	private final Trace trace;
	private final File path;
	
	private boolean initialized = false;
	
	private int windowWidth, windowHeight;

	private ArrayList<String> unparsedLines = new ArrayList<String>();
	private ArrayList<Line> relevantLines = new ArrayList<Line>();
	private TLongHashSet objects = new TLongHashSet();

	private ArrayList<String> log = new ArrayList<String>(1000);
	
	public PersistentState(WhylineUI whylineUI) {
		
		this(whylineUI, whylineUI.getTrace(), new File(whylineUI.getTrace().getPath(), Whyline.USAGE_PATH));
		
	}
	
	public PersistentState(WhylineUI whylineUI, Trace trace, File path) {

		this.trace = trace;
		this.path = path;
		this.whylineUI = whylineUI;
		
		// Initialize to defaults.
		windowWidth = 1024;
		windowHeight = 740;

		read();
		
	}

	public void read() {
		
		File file = path;
		if(!file.exists()) return;

		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(file));

			readOpen(reader, "whyline");
			
			{
				readOpen(reader, "window");
				windowWidth = Integer.parseInt(readValue(reader, "width"));
				windowHeight = Integer.parseInt(readValue(reader, "height"));
				readClose(reader, "window");
			}

			{
				readOpen(reader, "lines");
				int count = Integer.parseInt(readValue(reader, "count"));
				unparsedLines = new ArrayList<String>(count);
				for(int i = 0; i < count; i++)
					unparsedLines.add(readValue(reader, "line"));
				readClose(reader, "lines");
			}

			{
				readOpen(reader, "objects");
				int count = Integer.parseInt(readValue(reader, "count"));
				objects = new TLongHashSet(count);
				for(int i = 0; i < count; i++)
					objects.add(Long.valueOf(readValue(reader, "object")));
				readClose(reader, "objects");
			}
			
			{ 
				readOpen(reader, "navigation");
				int numberOfNavs = Integer.parseInt(readValue(reader, "count"));
				for(int i = 0; i < numberOfNavs; i++) {
					log.add(readValue(reader, "event"));
				}
				readClose(reader, "navigation");
			}

			readClose(reader, "whyline");

			reader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public synchronized void write() throws IOException {
				
		if(!initialized) return;
		
		File temp = new File(trace.getPath(), Whyline.USAGE_PATH + ".temp");
		temp.delete();
		temp.createNewFile();

		BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

		int depth = 0;

		open(writer, depth++, "whyline");

		open(writer, depth++, "window");
		value(writer, depth, "width", Integer.toString(windowWidth));
		value(writer, depth, "height", Integer.toString(windowHeight));
		close(writer, --depth, "window");
		
		open(writer, depth++, "lines");
		value(writer, depth, "count", Integer.toString(relevantLines.size()));
		for(Line line : relevantLines)
			value(writer, depth, "line", line.getFile().getFileName() + ":" + line.getLineNumber().getNumber());
		close(writer, --depth, "lines");

		open(writer, depth++, "objects");
		value(writer, depth, "count", Integer.toString(objects.size()));
		for(long id : objects.toArray())
			value(writer, depth, "object", Long.toString(id));
		close(writer, --depth, "objects");

		open(writer, depth++, "navigation");
		value(writer, depth, "count", Integer.toString(log.size()));
		for(String event : log)
			value(writer, depth, "event", event);
		close(writer, --depth, "navigation");
		
		close(writer, --depth, "whyline");

		writer.flush();
		writer.close();
		
		// Now that we've successful written it to a temp file, overwrite the old one.
		File file = new File(trace.getPath(), Whyline.USAGE_PATH);
		file.delete();
		temp.renameTo(file);
		
	}
	
	private static void value(BufferedWriter writer, int depth, String tag, String value) throws IOException {
		
		writer.write(Util.fillString(' ', depth));
		writer.write("<");
		writer.write(tag);
		writer.write(">");
		writer.write(value);
		writer.write("</");
		writer.write(tag);
		writer.write(">\n");
		
	}
	
	private static String readValue(BufferedReader reader, String tag) throws IOException {
		
		String line = reader.readLine();
		int first = line.indexOf('>') + 1;
		if(!line.substring(0, first).contains(tag)) throw new IOException("Expected tag " + tag);
		int last = line.lastIndexOf('<') - 1;
		String value = line.substring(first, last + 1);
		return value;
		
	}

	private static void open(BufferedWriter writer, int depth, String tag) throws IOException {
		
		writer.write(Util.fillString(' ', depth));
		writer.write("<");
		writer.write(tag);
		writer.write(">\n");
		
	}

	private static void readOpen(BufferedReader reader, String tag) throws IOException {
		
		String line = reader.readLine();
		if(!line.contains(tag)) throw new IOException("Expected <" + tag + "> but found line " + line);
		
	}

	private static void close(BufferedWriter writer, int depth, String tag) throws IOException {
		
		writer.write(Util.fillString(' ', depth));
		writer.write("</");
		writer.write(tag);
		writer.write(">\n");
		
	}

	private static void readClose(BufferedReader reader, String tag) throws IOException {
		
		String line = reader.readLine();
		if(!line.contains(tag)) throw new IOException("Expected </" + tag + "> but found line " + line);
		
	}

	public synchronized void addNavigation(AbstractUIEvent<?> navigation) {

		String string = navigation.getParsableString();
		string = string.replace("\n", "\\n");
		log.add(string);
		
	}
	
	public void updateWindowSize(int width, int height) {

		this.windowWidth = width;
		this.windowHeight = height;
		
	}

	public synchronized boolean addRelevantLine(Line line) {

		boolean isNew = !relevantLines.contains(line);
		if(!isNew) return false;
		
		// Move it to the end of the list by removing it first.
		relevantLines.remove(line);
		relevantLines.add(line);

		whylineUI.getLinesUI().updateRelevantLines(line);
		return true;

	}

	public synchronized void addRelevantObject(long id) {
		
		objects.add(id);
		
	}
	
	public synchronized void removeRelevantObject(long id) {
		
		objects.remove(id);
		
	}
	
	public List<Line> getRelevantLines() { return relevantLines; }

	public int getWindowWidth() { return windowWidth; }
	public int getWindowHeight() { return windowHeight; }

	public void initializeState() {

		for(String text : unparsedLines)
			addRelevantLine(Serializer.stringToLine(trace, text));
		
		for(long id : objects.toArray()) {
			whylineUI.getObjectsUI().addObject(id);
		}
	
		initialized = true;
		
	}
			
	public Iterable<String> getLog() { return log; }
	
}