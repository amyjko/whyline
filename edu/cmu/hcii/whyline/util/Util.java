package edu.cmu.hcii.whyline.util;

import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import edu.cmu.hcii.whyline.ui.UI;
import gnu.trove.TIntIntHashMap;

/**
 * A non-profit shelter for homeless functionality.
 *
 * @author Andrew J. Ko
 *
 */
public class Util {

	private static final int OUTPUT_STREAM_BUFFER_SIZE = 65536;

	public static void save(Saveable saveable, File file) throws IOException {
		
		DataOutputStream out = Util.getWriterFor(file);
		saveable.write(out);
		out.close();
		
	}
	
	public static void load(Saveable saveable, File file) throws IOException {
		
		DataInputStream in = Util.getReaderFor(file);
		saveable.read(in);
		in.close();
		
	}

	public static DataOutputStream getWriterFor(File file) throws FileNotFoundException {
		
		return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), OUTPUT_STREAM_BUFFER_SIZE));
		
	}
	
	public static DataInputStream getReaderFor(File file) throws IOException {

		DataInputStream data;
		
		// If the file is longer than we can fit into an array, use the slow method.
		if(file.length() > Integer.MAX_VALUE)		
			data = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		// Otherwise, read from a byte array, which is 4x faster on my machine.
		else {

			InputStream stream = new BufferedInputStream(new FileInputStream(file), 256);
			byte[] bytes = new byte[(int)file.length()];
			stream.read(bytes, 0, bytes.length);
			stream.close();
			data = new DataInputStream(new ByteArrayInputStream(bytes));

		}

		return data;
		
	}

	public static String fillOrTruncateString(String string, int length) {
		
		if(string.length() > length) string = string.substring(0, length - 3) + "...";
		else if(string.length() < length) {

			int numberOfSpaces = length - string.length();
			for(int i = 0; i < numberOfSpaces; i++) string = string + " ";
			
		}		
		return string;
		
	}
	
	public static String fillString(char c, int length) {
		
		String s = "";
		for(int i = 0; i < length; i++) s = s + c;
		return s;
		
	}

	public static String elide(String string, int limit) {

		if(string == null) return null;
		else if(limit <= 0) return string;
		else if(string.length() < limit) return string;
		return string.substring(0, limit - 3) + "...";
		
	}
	
	public static String getSimpleNameFromFullyQualifiedInternalClassName(String classname) {

		int indexOfLast = classname.lastIndexOf("/");
		if(indexOfLast < 0) return classname;
		else return classname.substring(indexOfLast + 1);

	}
	
	public static void drawArrowhead(Graphics2D g, int fromX, int fromY, int toX, int toY) {
		
		// Calculate and draw arrowhead, if selected
		double angle = Math.atan2(fromY - toY, fromX - toX);
		double arrowheadWidth = UI.ARROWHEAD_WIDTH;
		int arrowEdgeX = (int)(toX + Math.cos(angle) * arrowheadWidth);
		int arrowEdgeY = (int)(toY + Math.sin(angle) * arrowheadWidth);
		int arrowClockX = (int)(arrowEdgeX + Math.cos(angle + Math.PI / 4) * arrowheadWidth);
		int arrowClockY = (int)(arrowEdgeY + Math.sin(angle + Math.PI / 4) * arrowheadWidth);
		int arrowCounterX = (int)(arrowEdgeX + Math.cos(angle - Math.PI / 4) * arrowheadWidth);
		int arrowCounterY = (int)(arrowEdgeY + Math.sin(angle - Math.PI / 4) * arrowheadWidth);
		Polygon arrowhead = new Polygon();
		arrowhead.addPoint(arrowClockX, arrowClockY);
		arrowhead.addPoint(arrowCounterX, arrowCounterY);
		arrowhead.addPoint(toX, toY);
	
		g.fill(arrowhead);

		
	}
	
	public static Polygon getArrowhead(int fromX, int fromY, int toX, int toY, int controlXOffset, int controlYOffset) {

		int controlX = (toX + fromX) / 2 + controlXOffset;
		int controlY = (toY + fromY) / 2 + controlYOffset;

		// Calculate and draw arrowhead, if selected
		double angle = Math.atan2(controlY - toY, controlX - toX);
		double arrowheadWidth = UI.ARROWHEAD_WIDTH;
		int arrowEdgeX = (int)(toX + Math.cos(angle) * arrowheadWidth);
		int arrowEdgeY = (int)(toY + Math.sin(angle) * arrowheadWidth);
		int arrowClockX = (int)(arrowEdgeX + Math.cos(angle + Math.PI / 4) * arrowheadWidth);
		int arrowClockY = (int)(arrowEdgeY + Math.sin(angle + Math.PI / 4) * arrowheadWidth);
		int arrowCounterX = (int)(arrowEdgeX + Math.cos(angle - Math.PI / 4) * arrowheadWidth);
		int arrowCounterY = (int)(arrowEdgeY + Math.sin(angle - Math.PI / 4) * arrowheadWidth);
		Polygon arrowhead = new Polygon();
		arrowhead.addPoint(arrowClockX, arrowClockY);
		arrowhead.addPoint(arrowCounterX, arrowCounterY);
		arrowhead.addPoint(toX, toY);
		
		return arrowhead;
		
	}
	
	public static QuadCurve2D getCurve(int fromX, int fromY, int toX, int toY, int controlXOffset, int controlYOffset, boolean drawArrowhead) {
		
		int controlX = (toX + fromX) / 2 + controlXOffset;
		int controlY = (toY + fromY) / 2 + controlYOffset;
	
		// Calculate and draw arrowhead, if selected
		double angle = Math.atan2(controlY - toY, controlX - toX);
		double arrowheadWidth = UI.ARROWHEAD_WIDTH;
		int arrowEdgeX = (int)(toX + Math.cos(angle) * arrowheadWidth);
		int arrowEdgeY = (int)(toY + Math.sin(angle) * arrowheadWidth);
		int arrowClockX = (int)(arrowEdgeX + Math.cos(angle + Math.PI / 4) * arrowheadWidth);
		int arrowClockY = (int)(arrowEdgeY + Math.sin(angle + Math.PI / 4) * arrowheadWidth);
		int arrowCounterX = (int)(arrowEdgeX + Math.cos(angle - Math.PI / 4) * arrowheadWidth);
		int arrowCounterY = (int)(arrowEdgeY + Math.sin(angle - Math.PI / 4) * arrowheadWidth);
		Polygon arrowhead = new Polygon();
		arrowhead.addPoint(arrowClockX, arrowClockY);
		arrowhead.addPoint(arrowCounterX, arrowCounterY);
		arrowhead.addPoint(toX, toY);
	
		// Draw the curve
		toX = arrowEdgeX;
		toY = arrowEdgeY;
	
		QuadCurve2D.Float curve = new QuadCurve2D.Float(fromX, fromY, controlX, controlY, toX, toY);

		return curve;
		
	}
	
	public static void drawQuadraticCurveArrow(Graphics2D g, int fromX, int fromY, int toX, int toY, int controlXOffset, int controlYOffset, boolean drawArrowhead, Stroke neck) {
		
		int controlX = (toX + fromX) / 2 + controlXOffset;
		int controlY = (toY + fromY) / 2 + controlYOffset;
	
		// Calculate and draw arrowhead, if selected
		double angle = Math.atan2(controlY - toY, controlX - toX);
		double arrowheadWidth = UI.ARROWHEAD_WIDTH;
		int arrowEdgeX = (int)(toX + Math.cos(angle) * arrowheadWidth);
		int arrowEdgeY = (int)(toY + Math.sin(angle) * arrowheadWidth);
		int arrowClockX = (int)(arrowEdgeX + Math.cos(angle + Math.PI / 4) * arrowheadWidth);
		int arrowClockY = (int)(arrowEdgeY + Math.sin(angle + Math.PI / 4) * arrowheadWidth);
		int arrowCounterX = (int)(arrowEdgeX + Math.cos(angle - Math.PI / 4) * arrowheadWidth);
		int arrowCounterY = (int)(arrowEdgeY + Math.sin(angle - Math.PI / 4) * arrowheadWidth);
		Polygon arrowhead = new Polygon();
		arrowhead.addPoint(arrowClockX, arrowClockY);
		arrowhead.addPoint(arrowCounterX, arrowCounterY);
		arrowhead.addPoint(toX, toY);
	
		// Draw the curve
		toX = arrowEdgeX;
		toY = arrowEdgeY;

		g.setStroke(neck);
		
		QuadCurve2D.Float curve = new QuadCurve2D.Float(fromX, fromY, controlX, controlY, toX, toY);
		g.draw(curve);

		if(!drawArrowhead) return;
		
		g.fill(arrowhead);
			
	}
		
	public static void drawCrosshatch(Graphics2D g, Color color, int left,  int right, int top, int height, int spacing, int verticalOffset) {

		g = (Graphics2D)g.create();
		
		g.clipRect(left + 1, top + 1, right - left - 1, height - 1);

		g.setColor(color);
				
		int x = left - height + 1 - verticalOffset, bottom = top + height;
		int count = 0;
		while(x < right) {
			
			g.drawLine(x, bottom - 1, x + height, top + 1);
			x += spacing;
			count++;
			
		}
		
	}
	
	public static void deleteFolder(File folder) throws IOException {
		
		if(!folder.exists()) throw new IOException("" + folder + " doesn't exist.");
		if(!folder.isDirectory()) throw new IOException("" + folder + " is not a folder.");
		for(File fileOrFolder : folder.listFiles()) {
			if(fileOrFolder.isDirectory()) {
				deleteFolder(fileOrFolder);
				fileOrFolder.delete();
			}
			else fileOrFolder.delete();
		}
		folder.delete();
		
	}
	
	public static void copyFile(File source, File dest) throws IOException {

		if(!source.exists()) return;
		if(!dest.exists()) {
			dest.getParentFile().mkdirs();
			dest.createNewFile();
		}
		
        FileChannel srcChannel = new FileInputStream(source).getChannel();
        FileChannel dstChannel = new FileOutputStream(dest).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();

	}

	public static interface ProgressListener {
		public void progress(double percent);
		public void notice(String notice);
	}
	
	public static long getSizeOfFolder(File folder) {
		
		if(!folder.exists() || !folder.isDirectory()) throw new RuntimeException("" + folder + " does not exist or is not a folder");
		
		long size = 0;
		
		File[] files = folder.listFiles();
		for(File fileOrFolder : files) {
			if(fileOrFolder.isFile()) size += fileOrFolder.length();
			else if(fileOrFolder.isDirectory()) size += getSizeOfFolder(fileOrFolder);
		}
		return size;

	}
	
	public static void copyFolder(File source, File dest, ProgressListener progressListener) throws IOException {

		long size = getSizeOfFolder(source);
		
		copyFolder(source, dest, progressListener, size, 0L);
		
	}
		
	private static long copyFolder(File source, File dest, ProgressListener progressListener, long size, long copied) throws IOException {

		if(!source.isDirectory() || !source.exists()) throw new RuntimeException("" + source + " is not a folder");
		if(!dest.isDirectory() || !dest.exists()) throw new RuntimeException("" + dest + " is not a folder");
		
		File[] files = source.listFiles();
		for(File fileOrFolder : files) {

			if(progressListener != null) progressListener.notice("Copying " + fileOrFolder.getName()); 
			if(fileOrFolder.isFile()) {
				copyFile(fileOrFolder, new File(dest, fileOrFolder.getName()));
				copied += fileOrFolder.length();
				progressListener.progress(((double)copied) / size);
			}
			else if(fileOrFolder.isDirectory()) {
				
				File newFolder = new File(dest, fileOrFolder.getName());
				newFolder.mkdir();
				copied = copyFolder(fileOrFolder, newFolder, progressListener, size, copied);
				
			}
			
		}
		return copied;
		
	}
	
	public static int getFolderSizeInBytes(File folder) {
		
		int size = 0;
		
		if(!folder.isDirectory() || !folder.exists()) throw new RuntimeException("" + folder + " is not a folder.");
		
		for(File fileOrFolder : folder.listFiles()) {

			if(fileOrFolder.isDirectory()) size += getFolderSizeInBytes(fileOrFolder);
			else size += fileOrFolder.length();
			
		}
		
		return size;
		
	}
	
	public static String getDateString() {
		
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("EST"));
		
		// Create the log file.
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		String dayString = getDayOfWeekString(day);

		
		int month = calendar.get(Calendar.MONTH);
		String monthString = getMonthString(month);

		int date = calendar.get(Calendar.DAY_OF_MONTH);
		int year = calendar.get(Calendar.YEAR);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		
		String dateString = "" +
			(hour < 10 ? "0" + hour : hour) + " " + 
			(minute < 10 ? "0" + minute : minute) + " " + 
			(second < 10 ? "0" + second : second) + " " +
			"EST " +
			dayString + " " +
			monthString + " " +
			date + " " +
			year; 
		
		return dateString;
		
	}
	
	public static String getDayOfWeekString(int dayOfWeek) {
		
		switch(dayOfWeek) {
		case Calendar.SUNDAY : return "Sunday";
		case Calendar.MONDAY : return "Monday";
		case Calendar.TUESDAY : return "Tuesday";
		case Calendar.WEDNESDAY : return "Wednesday";
		case Calendar.THURSDAY : return "Thursday";
		case Calendar.FRIDAY : return "Friday";
		case Calendar.SATURDAY : return "Saturday";
		default : return "invalid day of week";
		}

	}
	
	public static String getMonthString(int month) {
		
		switch(month) {
		case Calendar.JANUARY : return "January";
		case Calendar.FEBRUARY : return "February";
		case Calendar.MARCH : return "March";
		case Calendar.APRIL : return "April";
		case Calendar.MAY : return "May";
		case Calendar.JUNE : return "June";
		case Calendar.JULY : return "July";
		case Calendar.AUGUST : return "August";
		case Calendar.SEPTEMBER : return "September";
		case Calendar.OCTOBER : return "October";
		case Calendar.NOVEMBER : return "November";
		case Calendar.DECEMBER : return "December";
		default : return "invalid month";
	}

	}
	
	public static void drawCallout(Graphics2D g, ImageIcon icon, String label, int x, int y) {
		
		g = (Graphics2D)g.create();
		
		g.setStroke(new BasicStroke(1.0f));
		g.setFont(UI.getSmallFont());

		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(label, g);
		int padding = 3;
		int iconWidth = icon == null ? 0 : icon.getIconWidth();
		int iconHeight = icon == null ? 0 : icon.getIconHeight();
		int maxContentHeight = (int) Math.max(iconHeight, stringBounds.getHeight());
		int calloutHeight = (maxContentHeight + padding * 2) - 1;
		int calloutWidth = (int) (iconWidth + stringBounds.getWidth() + padding * 3);
		
		int iconX = x + (icon == null ? 0 : padding);
		int iconY = y + padding;
		
		int stringX = iconX + iconWidth + padding;
		int stringBaseline = y + (calloutHeight - padding - (calloutHeight - g.getFontMetrics().getHeight()) / 2);
		
		g.setColor(UI.getPanelDarkColor());
		g.fillRoundRect(x, y, calloutWidth, calloutHeight, UI.getRoundedness(), UI.getRoundedness());

		g.setColor(UI.getControlBorderColor());
		g.drawRoundRect(x, y, calloutWidth, calloutHeight, UI.getRoundedness(), UI.getRoundedness());

		if(icon != null) icon.paintIcon(null, g, iconX, iconY);
		
		g.setColor(UI.getPanelTextColor());
		g.drawString(label, stringX, stringBaseline);

	}
	
	public static Rectangle2D getStringBounds(Graphics2D g, Font font, String s) {
		
		return g.getFontMetrics(font).getStringBounds(s, g);
		
	}
		
	// Find the middle. Find the first space to the left and first space to the right. Return the one closest to the middle.
	public static int findStringSplitIndex(String s) {

		int middle = s.length() / 2;
		int left = middle, right = middle;
		while(left >= 0) 
			if(s.charAt(left) == ' ') break; 
			else left--;
		while(right < s.length()) 
			if(s.charAt(right) == ' ') break; 
			else right++;
		
		return right - middle < middle - left ? right : left; 
		
	}

	private static class Line {

		public Line(double x1, double y1, double x2, double y2) {

			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			slope = (y2 - y1) / (x2 - x1);
			intercept = y1 - slope * x1;

		}
		public final double x1, y1, x2, y2, slope, intercept;
		
		// Treats this line as a segment and tries to find an intersection between the given line and this segment.
		public Point2D getIntersectionOnSegment(Line line) {

			// If they have the same slope, they're parallel, and don't intersect.
			if(slope == line.slope) return null;

			// If either is vertical (but not both, which is handled above), we have a special case, because it will have infinite slope 
			if(Double.isInfinite(slope) || Double.isNaN(slope)) {
				
				double x = x1;
				double y = x1 * line.slope + line.intercept;
				
				// Does the intersection point we found lie on this segment?
				if((y1 - y) * (y - y2) >= 0) return new Point2D.Double(x, y);
				else return null;
				
			}
			else if(Double.isInfinite(line.slope) || Double.isNaN(line.slope)) {

				double x = line.x1;
				double y = line.x1 * slope + intercept;

				// Does the intersection point we found lie on this segment?
				if((line.y1 - y) * (y - line.y2) >= 0) return new Point2D.Double(x, y);
				else return null;

			}
			else {
				
				double x = -(intercept - line.intercept) / (slope - line.slope);
				double y = intercept + slope * x;
				
				// Does the intersection point we found lie on this segment?
				if((x1 - x) * (x - x2) >= 0) return new Point2D.Double(x, y);
				else return null;

			}

		}
		
	}
	
	// Given a center point, a box around the center point, and a point outside the box, returns the point on the box to point to so that the
	// segment between the center point and the outside point points to the center.
	public static Point2D getIntersectionOfSegmentAndBox(double centerX, double centerY, double x1, double y1, double x2, double y2, double outsideX, double outsideY) {
		
		// Make the line representing (center, outside), and four lines representing the boxes edges.
		Line centerOutside = new Line(centerX, centerY, outsideX, outsideY);
		Line leftEdge = new Line(x1, y1, x1, y2);
		Line topEdge = new Line(x1, y1, x2, y1);
		Line rightEdge = new Line(x2, y1, x2, y2);
		Line bottomEdge = new Line(x1, y2, x2, y2);

		// Intersect (center, outside) with each of the edges.
		Point2D left = leftEdge.getIntersectionOnSegment(centerOutside);
		Point2D top = topEdge.getIntersectionOnSegment(centerOutside);
		Point2D right = rightEdge.getIntersectionOnSegment(centerOutside);
		Point2D bottom = bottomEdge.getIntersectionOnSegment(centerOutside);
		
		// Find the distance between the outside point and the intersection points, using a version of infinity for those with no intersection.
		double leftDistance = left == null ? Double.MAX_VALUE : distance(left.getX(), left.getY(), outsideX, outsideY);
		double topDistance = top == null ? Double.MAX_VALUE : distance(top.getX(), top.getY(), outsideX, outsideY);
		double rightDistance = right == null ? Double.MAX_VALUE : distance(right.getX(), right.getY(), outsideX, outsideY);
		double bottomDistance = bottom == null ? Double.MAX_VALUE : distance(bottom.getX(), bottom.getY(), outsideX, outsideY);

		// Which one is the shortest?
		double shortestDistance = Math.min(Math.min(Math.min(leftDistance, topDistance), rightDistance), bottomDistance);
				
		// Return the intersection point that's the shortest.
		Point2D shortest = 
			shortestDistance == leftDistance ? left : 
			shortestDistance == topDistance ? top : 
			shortestDistance == rightDistance ? right :
			bottom;

		// If this is null, it means there is no intersection.
		
		return shortest;
		
	}
	
	public static Line2D getLineBetweenRectangleEdges(double left1, double right1, double top1, double bottom1, double left2, double right2, double top2, double bottom2) {
		   
	   	double centerX1 = (left1 + right1) / 2;
	   	double centerY1 = (top1 + bottom1) / 2;
	   	double centerX2 = (left2 + right2) / 2;
	   	double centerY2 = (top2 + bottom2) / 2;

	   	Point2D one = getIntersectionOfSegmentAndBox(centerX1, centerY1, left1, top1, right1, bottom1, centerX2, centerY2);
	   	Point2D two = getIntersectionOfSegmentAndBox(centerX2, centerY2, left2, top2, right2, bottom2, centerX1, centerY1);
	   	
	   	return new Line2D.Double(one == null ? new Point2D.Double(centerX1, centerY1) : one, two == null ? new Point2D.Double(centerX2, centerY2) : two);
	   
   }

	
	public static double distance(double x1, double y1, double x2, double y2) {
	
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
		
	}
	
	public static String integerToASCII(int number) {
		
		char[] ascii = new char[8];
		ascii[0] = (char)(((number & 0xF0000000) >>> 28) + 97);
		ascii[1] = (char)(((number & 0x0F000000) >>> 24) + 97);
		ascii[2] = (char)(((number & 0x00F00000) >>> 20) + 97);
		ascii[3] = (char)(((number & 0x000F0000) >>> 16) + 97);
		ascii[4] = (char)(((number & 0x0000F000) >>> 12) + 97);
		ascii[5] = (char)(((number & 0x00000F00) >>> 8) + 97);
		ascii[6] = (char)(((number & 0x000000F0) >>> 4) + 97);
		ascii[7] = (char)(((number & 0x0000000F) >>> 0) + 97);

		for(int i = 0; i < 8; i++)
			System.err.println(ascii[i] - 97);
		
		return new String(ascii);
	
	}
	
	public static int ASCIIToInteger(String ascii) {

		long number =
			(ascii.charAt(0) - 97 << 28) |
			((ascii.charAt(1) - 97) << 24) |
			((ascii.charAt(2) - 97) << 20) |
			((ascii.charAt(3) - 97) << 16) |
			((ascii.charAt(4) - 97) << 12) |
			((ascii.charAt(5) - 97) << 8) |
			((ascii.charAt(6) - 97) << 4) |
			((ascii.charAt(7) - 97));
		
		return (int) number;
		
	}
	
	public static String commas(long value) {
		
		return NumberFormat.getNumberInstance().format(value);	
		
	}
	
	public static void writeIntIntMap(DataOutputStream stream, TIntIntHashMap map) throws IOException {
		
		stream.writeInt(map.size());
		for(int key : map.keys()) {
			stream.writeInt(key);
			stream.writeInt(map.get(key));
		}
		
	}

	public static void readIntIntMap(DataInputStream stream, TIntIntHashMap map) throws IOException {
		
		int size = stream.readInt();
		map.ensureCapacity(size);
		for(int i = 0; i < size; i++)
			map.put(stream.readInt(), stream.readInt());
		
	}

	public static String format(Object value, boolean html) {
		
		String valueString = "" + value;
		
		if(value instanceof Color) {
			
			Color color = (Color)value;
			String red = Integer.toHexString(color.getRed());
			if(color.getRed() < 10) red = "0" + red;
			String green = Integer.toHexString(color.getGreen());
			if(color.getGreen() < 10) green = "0" + green;
			String blue = Integer.toHexString(color.getBlue());
			if(color.getBlue() < 10) blue = "0" + blue;
			valueString = 
				html ? "<font color=#" + red + green + blue + ">&#9608</font>" : 
					"rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")"; 
	
		}
		else if(value instanceof String) {
			
			valueString = "\"" + valueString + "\"";
			
		}
		else if(value instanceof Font) {
			
			Font fontface = (Font)value;
			return fontface.getFamily() + " " + fontface.getSize() + " pt";
	
		}
		else if(value instanceof Rectangle) {
			
			Rectangle rect = (Rectangle)value;
			valueString = "[(" + rect.getX() + ", " + rect.getY() + "), (" + rect.getWidth() + ", " + rect.getHeight() +")]";
			
		}
		else if(value instanceof BasicStroke) {
			
			BasicStroke stroke = (BasicStroke)value;
			valueString = stroke.getLineWidth() + " pixel stroke";
			
		}

		return valueString;
		
	}
	
   public static void openURL(String url) {
	   
	   String osName = System.getProperty("os.name");
	   try {

		   if (osName.startsWith("Mac OS")) {
			   Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
			   Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});
			   openURL.invoke(null, new Object[] {url});
		   }
		   else if (osName.startsWith("Windows"))
			   Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
		   else { //assume Unix or Linux
			   String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
			   String browser = null;
			   for (int count = 0; count < browsers.length && browser == null; count++)
				   if (Runtime.getRuntime().exec(new String[] {"which", browsers[count]}).waitFor() == 0)
					   browser = browsers[count];
			   if (browser == null)
				   throw new Exception("Could not find web browser");
			   else
				   Runtime.getRuntime().exec(new String[] {browser, url});
		   }
	   }
	   catch (Exception e) {
		   JOptionPane.showMessageDialog(null, "Couldn't open " + url + " in your browser." + ":\n" + e.getLocalizedMessage());
	   }

   }
		   
}