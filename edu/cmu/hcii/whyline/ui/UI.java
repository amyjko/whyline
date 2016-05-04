package edu.cmu.hcii.whyline.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicPopupMenuSeparatorUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.text.DefaultEditorKit;

import edu.cmu.hcii.whyline.ui.components.WhylineControlBorder;
import edu.cmu.hcii.whyline.ui.components.WhylineScrollPane;

/**
 * User interface constants.
 * 
 * @author Andrew J. Ko
 *
 */
public class UI {

	// Characters
	public static final char LEFT_ARROW = '\u2190';
	public static final char UP_ARROW = '\u2191';
	public static final char RIGHT_ARROW = '\u2192';
	public static final char DOWN_ARROW = '\u2193';
	
	public static final char UP_WHITE_ARROW = '\u21E7';

	public static final char CHECKMARK = '\u2713';
	public static final char XMARK = '\u2716';
	public static final char INFINITY = '\u221E';
	public static final char EM_DASH = '\u2015';
	public static final char MINUS = '\u2212';
	
	// Strings
	public static final String EXPLANATION_BOX_TITLE = "notes";
	public static final String LOCALS_BOX_TITLE = "locals";
	public static final String CALL_STACK_BOX_TITLE = "call stack";
	
	public static final String SHOW_ORIGIN_LABEL = "show origin of value";
	public static final String SHOW_PRODUCER_LABEL = "show producer of value";
	
	public static final String POPUP_UI = "popup";
	public static final String BACK_UI = "back";
	public static final String OUTLINE_UI = "outline";
	public static final String RUN_TO_UI = "runto";
	public static final String STEP_INTO_UI = "into";
	public static final String STEP_OUT_UI = "out";
	public static final String STEP_OVER_UI = "over";
	public static final String SOURCE_ARROW_UI = "sourcearrow";
	public static final String EXCEPTIONS_UI = "exceptions";
	public static final String QUESTION_HOVER_UI = "question";
	public static final String LOADING_UI = "loading";
	public static final String OVERRIDE_UI = "override";
	public static final String DECLARATION_UI = "declaration";
	public static final String CLICK_EVENT_UI = "click";
	public static final String CLICK_UNEXECUTED_UI = "click";
	public static final String COLLAPSE_UI = "collapse";
	public static final String DATA_DEPENDENCY_UI = "data";
	public static final String INITIALIZATION_UI = "init";
	public static final String NEXT_EVENT_UI = "nextevent";
	public static final String PREVIOUS_EVENT_UI = "previousevent";
	public static final String NEXT_BLOCK_UI = "nextblock";
	public static final String PREVIOUS_BLOCK_UI = "previousblock";
	public static final String SOURCE_UI = "source";
	public static final String SEARCH_RESULTS_UI = "search";
	public static final String RELEVANT_LINES_UI = "search";
	public static final String BREAKPOINT_LINES_UI = "search";
	public static final String NAVIGATION_HISTORY_UI = "search";
	public static final String TIME_UI = "time";

	private static final String[] fontFamilyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	private static final Set<String> fontFamilyNameSet = new HashSet<String>();
	static {

		 for(String family : fontFamilyNames)
			 fontFamilyNameSet.add(family);
		
	}
	
	// Font names
	private static final String VARIABLE_WIDTH_FONT_NAME = fontFamilyNameSet.contains("Arial") ? "Arial" : "SansSerif";
	private static final String FIXED_WIDTH_FONT_NAME = fontFamilyNameSet.contains("Monaco") ? "Monaco" : "Monospaced";

	// Font
	private static final Font SMALL_FONT = new Font(VARIABLE_WIDTH_FONT_NAME, Font.PLAIN, 11);
	private static final Font MEDIUM_FONT = new Font(VARIABLE_WIDTH_FONT_NAME, Font.PLAIN, 12);
	private static final Font LARGE_FONT = new Font(VARIABLE_WIDTH_FONT_NAME, Font.BOLD, 14);
	private static final Font FIXED_WIDTH_FONT = new Font(FIXED_WIDTH_FONT_NAME, Font.PLAIN, 11);

	public static Font getSmallFont() { return SMALL_FONT; }
	public static Font getMediumFont() { return MEDIUM_FONT; }
	public static Font getLargeFont() { return LARGE_FONT; }
	public static Font getFixedFont() { return FIXED_WIDTH_FONT; }

	public static void setColors(
			Color focus,
			Color highlight, Color highlightText,
			Color panelLight, Color panelDark, Color panelText, 
			Color controlBorder, Color controlBack, Color controlCenter, Color controlFront, Color controlText, Color disabledText, 
			Color consoleBack, Color consoleText) { 

		FOCUS_COLOR = focus;
		
		HIGHLIGHT_COLOR = highlight;
		HIGHLIGHT_TEXT_COLOR = highlightText;

		PANEL_LIGHT_COLOR = panelLight;
		PANEL_DARK_COLOR = panelDark;
		CONTROL_BORDER_COLOR = controlBorder;
		PANEL_TEXT_COLOR = panelText;
		
		CONTROL_BACK_COLOR = controlBack;
		CONTROL_CENTER_COLOR = controlCenter;
		CONTROL_FRONT_COLOR = controlFront;
		CONTROL_TEXT_COLOR = controlText;
		CONTROL_DISABLED_COLOR = disabledText;
		
		CONSOLE_BACK_COLOR = consoleBack;
		CONSOLE_TEXT_COLOR = consoleText;

		FILE_COLOR = CONTROL_BACK_COLOR;
		
		installColorsInSwingComponents();

	}
	
	public static void setDefaultColors() { setCreamColors(); }
	
	public static void setBlackColors() {
		
		setColors(
				new Color(255, 255, 255), // focus
				new Color(255, 128, 0), // highlight
				new Color(255, 255, 255), // highlight text
				new Color( 40,  40,  40), // panel light
				new Color( 20,  20,  20), // panel dark
				new Color(255, 255, 255), // panel border
				new Color( 64,  64,  64), // panel text
				new Color(  0,  0,   0), // control dark
				new Color( 64,  64,  64), // control medium
				new Color(192, 192, 192), // control light
				new Color(255, 255, 255), // control text
				new Color( 96,  96,  96), // control disabled
				new Color(0,0,0),
				new Color(89, 225, 22)
		);
		
	}

	public static void setCreamColors() {
				
		setColors(
				new Color(0, 0, 0), // focus
				new Color(255,165,0), // highlight
				new Color(0,0,0), // highlight text
				new Color(215,210,181), // panel light
				new Color(181,171,113), // panel dark
				new Color(0, 0, 0), // panel text
				new Color(102,98,74), // control border
				new Color(255,255,245), // control back
				new Color(200,200,200), // control center
				new Color(128,128,128), // control front
				new Color(0, 0, 0), // control text
				new Color(64,64,64), // control disabled
				new Color(255,255,245),
				new Color(0,0,0)
		);
		
	}

	public static void setGrayColors() {
		
		Color light = new Color(220,220,220);
		Color dark = new Color(160,160,160);
		Color darker = new Color(120,120,120);

		setColors(
				new Color(0, 0, 0), // focus
				new Color(255,145,0), // highlight
				new Color(0,0,0), // highlight text
				light, // panel light
				dark, // panel dark
				new Color(0, 0, 0), // panel text
				darker, // control border
				new Color(255,255,255), // control back
				light, // control center
				darker, // control front
				new Color(0, 0, 0), // control text
				darker, // control disabled
				new Color(0,0,0),
				new Color(89, 225, 22)
		);
		
	}

	public static Color getFocusColor() { return FOCUS_COLOR; }
	public static Color getHighlightColor() { return HIGHLIGHT_COLOR; }
	public static Color getHighlightTextColor() { return HIGHLIGHT_TEXT_COLOR; }
	public static Color getPanelLightColor() { return PANEL_LIGHT_COLOR; }
	public static Color getPanelDarkColor() { return PANEL_DARK_COLOR; }
	public static Color getPanelTextColor() { return PANEL_TEXT_COLOR; }
	public static Color getControlBorderColor() { return CONTROL_BORDER_COLOR; }
	public static Color getControlBackColor() { return CONTROL_BACK_COLOR; }
	public static Color getControlCenterColor() { return CONTROL_CENTER_COLOR; }
	public static Color getControlFrontColor() { return CONTROL_FRONT_COLOR; }
	public static Color getControlTextColor() { return CONTROL_TEXT_COLOR; }
	public static Color getControlDisabledColor() { return CONTROL_DISABLED_COLOR; }
	public static Color getConsoleTextColor() { return CONSOLE_TEXT_COLOR; }
	public static Color getConsoleBackColor() { return CONSOLE_BACK_COLOR; }

	public static Color getStoppedColor() { return new Color(255, 128, 128); }
	public static Color getRunningColor() { return new Color(96, 255, 96); }

	//////////////////////////////////////////
	// Installable colors
	//////////////////////////////////////////
	
	// Keyboard focus
	private static  Color FOCUS_COLOR = Color.white;

	// Highlight colors
	private static  Color HIGHLIGHT_COLOR = new Color(255, 128, 0);
	private static  Color HIGHLIGHT_TEXT_COLOR = Color.white;

	// Panel colors
	private static  Color PANEL_LIGHT_COLOR = new Color(40, 40, 40);
	private static  Color PANEL_DARK_COLOR = new Color(20, 20, 20);	
	private static  Color CONTROL_BORDER_COLOR = new Color(64, 64, 64);
	private static  Color PANEL_TEXT_COLOR = Color.white;

	// UI component colors
	private static  Color CONTROL_BACK_COLOR = Color.black;
	private static  Color CONTROL_CENTER_COLOR = Color.darkGray;	
	private static  Color CONTROL_FRONT_COLOR = Color.lightGray;
	private static  Color CONTROL_TEXT_COLOR = Color.white;
	private static  Color CONTROL_DISABLED_COLOR = new Color(96, 96, 96);

	private static Color CONSOLE_BACK_COLOR = Color.black;
	private static Color CONSOLE_TEXT_COLOR = new Color(89, 225, 22);

	//////////////////////////////////////////
	// Fixed colors
	//////////////////////////////////////////

	// Colors with meaning
	public static final Color ERROR_COLOR = new Color(200, 0,0);
	public static final Color CORRECT_COLOR = new Color(0,200,0);
	public static final Color CONTROL_COLOR = new Color(64,155,83);
	public static final Color DATA_COLOR = new Color(103,105,211);
	public static final Color IO_COLOR = new Color(196,196,196);
	public static final Color UNFAMILIAR_COLOR = new Color(128,128,128);

	public static Color getFileColor() { return FILE_COLOR; }

	// Source file color
	private static Color FILE_COLOR = Color.white;
	public static final Color FILE_FADED_COLOR = new Color(228, 228, 228);
	public static final Color IDENTIFIER_COLOR = new Color(0,0,0);
	public static final Color KEYWORD_COLOR = new Color(79, 14, 120);
	public static final Color LITERAL_COLOR = new Color(42, 92, 255);
	public static final Color COMMENT_COLOR = new Color(63, 127, 112);
	public static final Color DELIMITER_COLOR = new Color(64, 64, 64);
	public static final Color OPERATOR_COLOR = new Color(64, 64, 64);

	// Breakpoint colors
	public static final Color BREAKPOINT_COLOR = new Color(125, 125, 255);
	public static final Color EXECUTING_LINE_COLOR = new Color(255, 200, 200);

	// Strokes with meaning
	public static final BasicStroke UNSELECTED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	public static final BasicStroke SELECTED_STROKE = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	public static final BasicStroke UNSELECTED_DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,1.0f, new float[] {4, 8}, 0);
	public static final BasicStroke SELECTED_DASHED_STROKE = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {4, 8}, 0);
	public static final BasicStroke INTERACTIVE_UNDERLINE_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] { 1, 1 }, 0);
	
	// Menu character width limits
	public static final int MAX_EVENT_LENGTH = 12;
	public static final int MENU_CHARACTER_LENGTH_LIMIT = 16;
	
	// Measurements
	private static final int ROUNDEDNESS = 10;
	private static final int PANEL_PADDING = 12;
	private static final int BORDER_PADDING = 4;
	private static final double INFO_PANE_PROPORTION = .20;
	private static final int TOOLBAR_HEIGHT = 32;
	private static final int CROSSHATCH_SPACING = 40;

	public static int getRoundedness() { return ROUNDEDNESS; }
	public static int getPanelPadding() { return PANEL_PADDING; }
	public static int getBorderPadding() { return BORDER_PADDING; }
	public static int getDefaultInfoPaneWidth(Window window) { return (int) (window.getWidth() * INFO_PANE_PROPORTION); }
	public static int getDefaultInfoPaneHeight(Window window) { return (int) (window.getHeight() * INFO_PANE_PROPORTION); }
	public static int getToolbarHeight() { return TOOLBAR_HEIGHT; }
	public static int getCrosshatchSpacing() { return CROSSHATCH_SPACING; }

	public static final int SCROLL_BAR_SIZE = 12;
	
	// IO measurements
	public static final int TIME_UI_HEIGHT = 100;	
	public static final int GRAPHICS_SCALE_MIN = 25;
	public static final int GRAPHICS_SCALE_MAX = 250;

	// Visualization measurements
	public static final int PADDING_BETWEEN_EVENTS = 10;
	public static final int ELISION_PADDING = 60;
	public static final int ELISION_DIAMETER= 4;
	public static final int DIAMETERS_PER_ELISION = 4;
	public static final int PADDING_WITHIN_EVENTS = 4;
	public static final int BLOCK_VERTICAL_PADDING = 0;
	public static final int MIN_THREAD_ROW_HEIGHT = 48;
	public static final int HIDDEN_EVENT_SIZE = 0;
	public static final int ARROWHEAD_WIDTH = 8;
	public static final int CALLOUT_BOX_PADDING = 5; 
	public static final double EVENT_BLOCK_SCALING = 1.0;
	public static final double MAX_MINIMIZED_FILE_HEIGHT = 20;
	public static final double MAX_MINIMIZED_FILE_WIDTH = 80;
	public static final int MOUSE_POSITION_MARKER_SIZE = 6;
	
	// Thresholds
	public static final double PROPORTION_OF_WINDOW_AT_WHICH_TO_SCROLL = .2;
	public static final double PROPORTION_OF_PIXELS_TO_SCROLL = .25;
	public static final float TRANSPARENCY_OF_IRRELEVANT_LINES = .5f;
	public static final float DESELECTED_ICON_TRANSPARENCY = .5f;
		
	// Times
	private static final int ANIMATION_DURATION = 250;

	public static int getDuration() { return ANIMATION_DURATION; }

	public static final String TAB_SPACES = "    ";
	public static int FILE_HEADER_PADDING = 3;
		
	// Icons
	public static int ICON_SIZE = 24;
	public static int WIDE_ICON_SIZE = 48;
	
	public static ImageIcon WHYLINE_IMAGE = new ImageIcon(WhylineUI.class.getResource("images/whyline_large.png"));
	
	public static ImageIcon WHYLINE_ICON = loadImage("whyline.png");
	public static ImageIcon REPAINT_ICON = loadImage("repaint.png");
	public static ImageIcon MOUSE_UP_ICON = loadImage("mouseup.png");
	public static ImageIcon MOUSE_DOWN_ICON = loadImage("mousedown.png");
	public static ImageIcon MOUSE_MOVE_ICON = loadImage("mousemove.png");
	public static ImageIcon MOUSE_DRAG_ICON = loadImage("mousedrag.png");
	public static ImageIcon MOUSE_WHEEL_ICON = loadImage("mousewheel.png");
	public static ImageIcon KEY_UP_ICON = loadImage("keyup.png");
	public static ImageIcon KEY_DOWN_ICON = loadImage("keydown.png");
	public static ImageIcon CONSOLE_IN_ICON = loadImage("consolein.png");
	public static ImageIcon CONSOLE_OUT_ICON = loadImage("consoleout.png");

	public static ImageIcon PREVIOUS_EVENT = loadImageWithWidth("previous_event.png", WIDE_ICON_SIZE);
	public static ImageIcon NEXT_EVENT = loadImageWithWidth("next_event.png", WIDE_ICON_SIZE);
	public static ImageIcon PREVIOUS_BLOCK = loadImageWithWidth("previous_block.png", WIDE_ICON_SIZE);
	public static ImageIcon NEXT_BLOCK = loadImageWithWidth("next_block.png", WIDE_ICON_SIZE);
	public static ImageIcon HIDE_THREADS = loadImageWithWidth("hide_threads.png", WIDE_ICON_SIZE);
	public static ImageIcon SHOW_THREADS = loadImageWithWidth("show_threads.png", WIDE_ICON_SIZE);
	public static ImageIcon EXPAND = loadImageWithWidth("expand.png", WIDE_ICON_SIZE);
	public static ImageIcon COLLAPSE = loadImageWithWidth("collapse.png", WIDE_ICON_SIZE);
	public static ImageIcon GO_BACK = loadImageWithWidth("back.png", ICON_SIZE);

	public static ImageIcon STEP_INTO = new ImageIcon(WhylineUI.class.getResource("images/stepinto.gif"));
	public static ImageIcon STEP_OVER = new ImageIcon(WhylineUI.class.getResource("images/stepover.gif"));
	public static ImageIcon STEP_OUT = new ImageIcon(WhylineUI.class.getResource("images/stepout.gif"));
	public static ImageIcon STOP = new ImageIcon(WhylineUI.class.getResource("images/stop.png"));
	public static ImageIcon RESUME = new ImageIcon(WhylineUI.class.getResource("images/resume.gif"));
	public static ImageIcon PAUSE = new ImageIcon(WhylineUI.class.getResource("images/pause.png"));

	public static Icon TREE_BLANK = new Icon() {
		public int getIconHeight() { return 12; }
		public int getIconWidth() { return 12; }
		public void paintIcon(Component c, Graphics g, int x, int y) {}
	};
	
	public static Icon TREE_OPEN = new Icon() {
		public int getIconHeight() { return 12; }
		public int getIconWidth() { return 12; }
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g = g.create();
			g.setColor(getControlFrontColor());
			g.drawLine(x + 3, y + 6, x + 9, y + 6);
		}
	};

	public static Icon TREE_CLOSED = new Icon() {
		public int getIconHeight() { return 12; }
		public int getIconWidth() { return 12; }
		public void paintIcon(Component c, Graphics g, int x, int y) {
			g = g.create();
			g.setColor(getControlFrontColor());
			g.drawLine(x + 3, y + 6, x + 9, y + 6);
			g.drawLine(x + 6, y + 3, x + 6, y + 9);
		}
	};

	public static Icon TREE_COLLAPSED = TREE_BLANK;

	public static Icon TREE_EXPANDED = TREE_BLANK;

	public static Icon TREE_LEAF = new Icon() {
		public int getIconHeight() { return 12; }
		public int getIconWidth() { return 12; }
		public void paintIcon(Component c, Graphics g, int x, int y) {}
	};

	private static ImageIcon loadImage(String name) {
		
		return new ImageIcon((new ImageIcon(WhylineUI.class.getResource("images/" + name))).getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, java.awt.Image.SCALE_SMOOTH));
		
	}
	
	private static ImageIcon loadImageWithWidth(String name, int width) {
		
		ImageIcon newImage = new ImageIcon(WhylineUI.class.getResource("images/" + name));
		
		double ratio = (double)newImage.getIconHeight() / newImage.getIconWidth();
		
		int height = (int) (ratio * width);
		
		return new ImageIcon(newImage.getImage().getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
		
	}

	// Do this after initializing all of the colors.
	static {

		setDefaultColors();
		
	}

	public static void installColorsInSwingComponents() {
		
		UIManager.put("Panel.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("Panel.font", new FontUIResource(UI.getSmallFont()));
		UIManager.put("Panel.foreground", new ColorUIResource(UI.getPanelTextColor()));

		UIManager.put("Label.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("Label.font", new FontUIResource(UI.getSmallFont()));
		UIManager.put("Label.foreground", new ColorUIResource(UI.getPanelTextColor()));
		UIManager.put("Label.disabledForeground", new ColorUIResource(UI.getPanelTextColor()));
		UIManager.put("Label.disabledShadow", new ColorUIResource(UI.getPanelLightColor()));

		UIManager.put("OptionPane.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("OptionPane.foreground", new ColorUIResource(UI.getPanelTextColor()));
		UIManager.put("OptionPane.questionDialog.border.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("OptionPane.messageAreaBorder", new BorderUIResource(new EmptyBorder(10, 10, 10, 10)));
		UIManager.put("OptionPane.messageForeground", new ColorUIResource(UI.getPanelTextColor()));
		
		UIManager.put("Viewport.background", new ColorUIResource(UI.getPanelLightColor()));
		
		UIManager.put("windowBorder", UI.getPanelLightColor());
		UIManager.put("control", UI.getControlBackColor());
		UIManager.put("controlShadow", UI.getControlBackColor());
		UIManager.put("controlDkShadow", UI.getControlBackColor());
		UIManager.put("controlHighlight", UI.getControlBackColor());
		UIManager.put("controlLtHighlight", UI.getControlBackColor());
		UIManager.put("controlShadow", UI.getControlBackColor());
		UIManager.put("controlText", UI.getControlTextColor());

		ArrayList<Object> gradient = new ArrayList<Object>();
		gradient.add(0.2);
		gradient.add(0.6);
		gradient.add(UI.getControlFrontColor());
		gradient.add(UI.getControlCenterColor());
		gradient.add(UI.getControlFrontColor());
		
		UIManager.put("Button.gradient", gradient);
		UIManager.put("Button.background", new ColorUIResource(UI.getControlBackColor()));
		
		final Insets insets = new Insets(6, 4, 6, 4);
		
		Border buttonBorder = new Border() {
			public Insets getBorderInsets(Component c) { return insets; }
			public boolean isBorderOpaque() { return false; }
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				if(c.isEnabled()) {
					((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(getControlBorderColor());
					g.drawRoundRect(x, y, width - 1, height - 1, 4,4);
				}
			}
		};
		
		UIManager.put("Button.border", new BorderUIResource(buttonBorder));
		UIManager.put("Button.darkShadow", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Button.disabledText", new ColorUIResource(UI.getControlDisabledColor()));
		UIManager.put("Button.focus", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Button.foreground", new ColorUIResource(UI.getControlTextColor()));
		UIManager.put("Button.highlight", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Button.light", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Button.select", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Button.shadow", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Button.font", new FontUIResource(UI.getSmallFont()));

		UIManager.put("RootPane.questionDialogBorder", new BorderUIResource(new EmptyBorder(10, 10, 10, 10)));
		UIManager.put("RootPane.warningDialogBorder", new BorderUIResource(new EmptyBorder(10, 10, 10, 10)));
		UIManager.put("RootPane.informationDialogBorder", new BorderUIResource(new EmptyBorder(10, 10, 10, 10)));
		UIManager.put("RootPane.frameBorder", new BorderUIResource(new EmptyBorder(10, 10, 10, 10)));
		
		UIManager.put("Checkbox.select", new ColorUIResource(UI.getHighlightColor()));
				
		UIManager.put("EditorPane.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("EditorPane.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("EditorPane.caretForeground", new ColorUIResource(UI.getControlTextColor()));

		UIManager.put("FormattedTextField.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("FormattedTextField.selectionBackground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("FormattedTextField.border", new BorderUIResource(new WhylineControlBorder()));
		UIManager.put("FormattedTextField.foreground", new ColorUIResource(UI.getControlTextColor()));
		UIManager.put("FormattedTextField.background", new ColorUIResource(UI.getControlBackColor()));

		UIManager.put("TextField.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("TextField.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("TextField.border", new BorderUIResource(new WhylineControlBorder()));
		UIManager.put("TextField.foreground", new ColorUIResource(UI.getControlTextColor()));
		UIManager.put("TextField.background", new ColorUIResource(UI.getControlBackColor()));

		UIManager.put("List.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("List.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("List.focusCellHighlightBorder", new ColorUIResource(UI.getHighlightColor()));
		
		UIManager.put("RadioButton.select", new ColorUIResource(UI.getHighlightColor()));

		UIManager.put("Table.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Table.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		
		UIManager.put("TextArea.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("TextArea.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("TextArea.caretForeground", new ColorUIResource(UI.getControlTextColor()));
		
		UIManager.put("TextPane.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("TextPane.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("TextPane.caretForeground", new ColorUIResource(UI.getControlTextColor()));
		
		UIManager.put("ToggleButton.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("ToggleButton.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("ToggleButton.select", new ColorUIResource(UI.getHighlightColor())); 
		
		UIManager.put("Tree.selectionBackground", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("Tree.selectionForeground", new ColorUIResource(UI.getHighlightTextColor()));

		LineBorder tooltipBorder = new LineBorder(UI.getControlBorderColor(), 1, true) {
		    public Insets getBorderInsets(Component c)       {
		        return new Insets(3, 3, 3, 3);
		    }
		};
		
		UIManager.put("ToolTip.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("ToolTip.foreground", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("ToolTip.border", tooltipBorder);
		UIManager.put("ToolTip.backgroundInactive", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("ToolTip.foregroundInactive", new ColorUIResource(UI.getHighlightTextColor()));
		UIManager.put("ToolTip.borderInactive", tooltipBorder);
		UIManager.put("ToolTip.font", new FontUIResource(UI.getSmallFont()));
		
		ToolTipManager.sharedInstance().setInitialDelay(1000);
		ToolTipManager.sharedInstance().setDismissDelay(3000);
		ToolTipManager.sharedInstance().setReshowDelay(1000);

		UIManager.put("ToolBar.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("ToolBar.border", new BorderUIResource(new EmptyBorder(getPanelPadding(), getPanelPadding(), getPanelPadding(), getPanelPadding())));
		UIManager.put("ToolBar.darkShadow", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("ToolBar.foreground", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("ToolBar.nonrolloverBorder", new BorderUIResource(buttonBorder));
		UIManager.put("ToolBar.rolloverBorder", new BorderUIResource(buttonBorder));
		UIManager.put("ToolBar.separatorSize", new Dimension(getPanelPadding(), getPanelPadding()));
		
		UIManager.put("textHighlight", new ColorUIResource(UI.getHighlightColor()));
		
		UIManager.put("textHighlightText", new ColorUIResource(getHighlightTextColor()));
		
		UIManager.put("Menu.selectionForeground", new ColorUIResource(getHighlightTextColor()));
		UIManager.put("Menu.selectionBackground", new ColorUIResource(getHighlightColor()));

		UIManager.put("MenuItem.selectionForeground", new ColorUIResource(getHighlightTextColor()));
		UIManager.put("MenuItem.selectionBackground", new ColorUIResource(getHighlightColor()));

		UIManager.put("Tree.background", new ColorUIResource(getControlBackColor()));
		UIManager.put("Tree.foreground", new ColorUIResource(getControlTextColor()));
		UIManager.put("Tree.textBackground", new ColorUIResource(getControlBackColor()));
		UIManager.put("Tree.textForeground", new ColorUIResource(getControlTextColor()));

		UIManager.put("Tree.line", new ColorUIResource(getControlBackColor()));
		UIManager.put("Tree.selectionBackground", new ColorUIResource(getHighlightColor()));
		UIManager.put("Tree.selectionBorderColor", new ColorUIResource(getHighlightColor()));
		UIManager.put("Tree.selectionForeground", new ColorUIResource(getHighlightTextColor()));
		UIManager.put("Tree.scrollsOnExpand", true);
		UIManager.put("Tree.font", new FontUIResource(UI.getSmallFont()));
		UIManager.put("Tree.openIcon", new IconUIResource(TREE_OPEN));
		UIManager.put("Tree.closedIcon", new IconUIResource(TREE_CLOSED));
		UIManager.put("Tree.collapsedIcon", new IconUIResource(TREE_COLLAPSED));
		UIManager.put("Tree.expandedIcon", new IconUIResource(TREE_EXPANDED));
		UIManager.put("Tree.leafIcon", new IconUIResource(TREE_LEAF));

		UIManager.put("ComboBox.font", new FontUIResource(getMediumFont()));
		UIManager.put("ComboBox.background", new ColorUIResource(getControlBackColor()));
		UIManager.put("ComboBox.buttonBackground", new ColorUIResource(getControlBackColor()));
		UIManager.put("ComboBox.buttonDarkShadow", new ColorUIResource(getControlBackColor()));
		UIManager.put("ComboBox.buttonHighlight", new ColorUIResource(getControlBackColor()));
		UIManager.put("ComboBox.buttonShadow", new ColorUIResource(getControlBackColor()));
		UIManager.put("ComboBox.disabledBackground", new ColorUIResource(getControlBackColor()));
		UIManager.put("ComboBox.disabledForeground", new ColorUIResource(getControlCenterColor()));
		UIManager.put("ComboBox.foreground", new ColorUIResource(getControlTextColor()));
		UIManager.put("ComboBox.selectionForeground", new ColorUIResource(getHighlightTextColor()));
		UIManager.put("ComboBox.selectionBackground", new ColorUIResource(getHighlightColor()));

		UIManager.put("Table.background", new ColorUIResource(getControlBackColor()));
		UIManager.put("Table.foreground", new ColorUIResource(getControlTextColor()));
		UIManager.put("Table.gridColor", new ColorUIResource(getControlBackColor()));
		UIManager.put("Table.focusCellBackground", new ColorUIResource(getControlBackColor()));
		UIManager.put("Table.focusCellForeground", new ColorUIResource(getControlBackColor()));
		UIManager.put("Table.focusCellHighlightBorder", new ColorUIResource(getControlBackColor()));
		UIManager.put("Table.selectionBackground", new ColorUIResource(getHighlightColor()));
		UIManager.put("Table.selectionForeground", new ColorUIResource(getHighlightTextColor()));
		UIManager.put("Table.font", new FontUIResource(getMediumFont().deriveFont(10.0f)));
		UIManager.put("Table.scrollPaneBorder", new BorderUIResource(new EmptyBorder(2, 2, 2, 2)));

		UIManager.put("TableHeader.cellBorder", new BorderUIResource(new EmptyBorder(2, 2, 2, 2)));
		UIManager.put("TableHeader.font", new FontUIResource(getMediumFont().deriveFont(Font.BOLD)));
		UIManager.put("TableHeader.background", new ColorUIResource(getControlBackColor())); 
		UIManager.put("TableHeader.foreground", new ColorUIResource(getControlTextColor()));
		UIManager.put("TableHeader.font", new FontUIResource(getMediumFont()));

		int padding = getBorderPadding();
		UIManager.put("TabbedPane.selectedTabPadInsets", new InsetsUIResource(padding, padding, padding, padding));
		UIManager.put("TabbedPane.tabAreaInsets", new InsetsUIResource(0, padding, -padding - 1, padding));
		UIManager.put("TabbedPane.tabInsets", new InsetsUIResource(padding, padding, padding, padding));
		UIManager.put("TabbedPane.font", new FontUIResource(getLargeFont()));
		UIManager.put("TabbedPane.contentBorderInsets", new InsetsUIResource(0,0,0,0));
		UIManager.put("TabbedPane.background", new ColorUIResource(UI.getControlBackColor()));
		UIManager.put("TabbedPane.darkShadow", new ColorUIResource(UI.getPanelDarkColor()));
		UIManager.put("TabbedPane.foreground", new ColorUIResource(UI.getControlTextColor()));
		UIManager.put("TabbedPane.highlight", new ColorUIResource(UI.getControlFrontColor()));
		UIManager.put("TabbedPane.light", new ColorUIResource(UI.getControlCenterColor()));
		UIManager.put("TabbedPane.selectHighlight", new ColorUIResource(getControlBorderColor()));
		UIManager.put("TabbedPane.selected", new ColorUIResource(getControlCenterColor()));
		// This one controls the 'torn' look of a cut off tab
		UIManager.put("TabbedPane.shadow", new ColorUIResource(getPanelLightColor()));
		
		UIManager.put("Separator.background", new ColorUIResource(getPanelDarkColor()));
		UIManager.put("Separator.foreground", new ColorUIResource(getPanelDarkColor()));
		UIManager.put("Separator.highlight", new ColorUIResource(getPanelDarkColor()));
		UIManager.put("Separator.shadow", new ColorUIResource(getPanelDarkColor()));

		UIManager.put("ScrollBarUI", WhylineScrollPane.WhylineScrollBarUI.class.getName());
		UIManager.put("ScrollBar.darkShadow", new ColorUIResource(UI.getControlBackColor()));
		UIManager.put("ScrollBar.highlight", new ColorUIResource(UI.getControlBackColor()));
		UIManager.put("ScrollBar.width", SCROLL_BAR_SIZE);

		UIManager.put("SliderUI", BasicSliderUI.class.getName());		
		UIManager.put("Slider.background", new ColorUIResource(UI.getPanelLightColor()));
		UIManager.put("Slider.foreground", new ColorUIResource(UI.getControlFrontColor()));
		UIManager.put("Slider.highlight", new ColorUIResource(UI.getControlCenterColor()));
		UIManager.put("Slider.shadow", new ColorUIResource(UI.getControlFrontColor()));
		UIManager.put("Slider.focus", new ColorUIResource(UI.getControlCenterColor()));

		UIManager.put("PopupMenuSeparatorUI", PopupSeparatorUI.class.getName());
		
		// These borders with the bumps are ugly! Paint over them with the border
		UIManager.put("SplitPane.dividerSize", getPanelPadding());
		UIManager.put("SplitPaneDivider.border", new Border() {
			public Insets getBorderInsets(Component c) { return null; }
			public boolean isBorderOpaque() { return true; }
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				g.setColor(UI.getPanelLightColor());
				g.fillRect(0, 0, width, height);
			}
		});
		
		ArrayList<Object> checkboxGradient = new ArrayList<Object>();
		checkboxGradient.add(0.2);
		checkboxGradient.add(0.6);
		checkboxGradient.add(UI.getControlFrontColor());
		checkboxGradient.add(UI.getControlCenterColor());
		checkboxGradient.add(UI.getControlFrontColor());

		UIManager.put("CheckBox.background", new ColorUIResource(UI.getControlFrontColor()));
		UIManager.put("CheckBox.foreground", new ColorUIResource(UI.getControlTextColor()));
		UIManager.put("CheckBox.disabledText", new ColorUIResource(UI.getControlDisabledColor()));
		UIManager.put("CheckBox.focus", new ColorUIResource(UI.getControlBorderColor()));
		UIManager.put("CheckBox.gradient", checkboxGradient);
		UIManager.put("CheckBox.select", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("CheckBox.shadow", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("CheckBox.light", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("CheckBox.highlight", new ColorUIResource(UI.getHighlightColor()));
		UIManager.put("CheckBox.darkShadow", new ColorUIResource(UI.getHighlightColor()));
		
		setOSXFieldShortcuts();
		
	}

	public static void setOSXFieldShortcuts() {
		
		String lowercaseOSName = System.getProperty("os.name").toLowerCase();
		boolean MAC_OS_X = lowercaseOSName.startsWith("mac os x");

		if(MAC_OS_X) {
			
	        Object fieldInputMap = new UIDefaults.LazyInputMap(new Object[] {
	        		"meta C", DefaultEditorKit.copyAction,
	                "meta V", DefaultEditorKit.pasteAction,
	                "meta X", DefaultEditorKit.cutAction,
	                  "COPY", DefaultEditorKit.copyAction,
	                 "PASTE", DefaultEditorKit.pasteAction,
	                 "CUT", DefaultEditorKit.cutAction,
	                 "shift LEFT", DefaultEditorKit.selectionBackwardAction,
	                 "shift KP_LEFT", DefaultEditorKit.selectionBackwardAction,
	                 "shift RIGHT", DefaultEditorKit.selectionForwardAction,
	                 "shift KP_RIGHT", DefaultEditorKit.selectionForwardAction,
	                 "alt LEFT", DefaultEditorKit.previousWordAction,
	                 "alt KP_LEFT", DefaultEditorKit.previousWordAction,
	                 "alt RIGHT", DefaultEditorKit.nextWordAction,
	                 "alt KP_RIGHT", DefaultEditorKit.nextWordAction,
	                 "alt shift LEFT", DefaultEditorKit.selectionPreviousWordAction,
	                 "alt shift KP_LEFT", DefaultEditorKit.selectionPreviousWordAction,
	                 "alt shift RIGHT", DefaultEditorKit.selectionNextWordAction,
	                 "alt shift KP_RIGHT", DefaultEditorKit.selectionNextWordAction,
	                 "meta A", DefaultEditorKit.selectAllAction,
	                 "HOME", DefaultEditorKit.beginLineAction,
	                 "meta LEFT", DefaultEditorKit.beginLineAction,
	                 "END", DefaultEditorKit.endLineAction,
	                 "meta RIGHT", DefaultEditorKit.endLineAction,
	                 "shift HOME", DefaultEditorKit.selectionBeginLineAction,
	                 "meta shift LEFT", DefaultEditorKit.selectionBeginLineAction,
	                 "meta shift RIGHT", DefaultEditorKit.selectionEndLineAction,
	                 "shift END", DefaultEditorKit.selectionEndLineAction,
	                 "typed \010", DefaultEditorKit.deletePrevCharAction,
	                 "DELETE", DefaultEditorKit.deleteNextCharAction,
	                  //    "alt DELETE", DefaultEditorKit.deleteNextWordAction,
	                  // "alt BACKSPACE", DefaultEditorKit.deletePrevWordAction,
	                 "RIGHT", DefaultEditorKit.forwardAction,
	                 "LEFT", DefaultEditorKit.backwardAction,
	                 "UP", DefaultEditorKit.beginAction,
	                 "DOWN", DefaultEditorKit.endAction,
	                 "shift UP", DefaultEditorKit.selectionBeginAction,
	                 "shift DOWN", DefaultEditorKit.selectionEndAction,
	                 "KP_RIGHT", DefaultEditorKit.forwardAction,
	                 "KP_LEFT", DefaultEditorKit.backwardAction,
	                 "ENTER", JTextField.notifyAction,
	                 "meta shift A", "unselect"/*DefaultEditorKit.unselectAction*/,
	                 "control shift O",
	                 "toggle-componentOrientation"/*DefaultEditorKit.toggleComponentOrientation*/
	        	});
	
	        UIManager.put("TextField.focusInputMap", fieldInputMap);
	        UIManager.put("TextArea.focusInputMap", fieldInputMap);
	        UIManager.put("TextPane.focusInputMap", fieldInputMap);

		}

	}
	
	public static class PopupSeparatorUI extends BasicPopupMenuSeparatorUI {
	    public static ComponentUI createUI( JComponent c ) { return new PopupSeparatorUI(); }
	    public void paint( Graphics g, JComponent c ) {
	    	Dimension s = c.getSize();
			g.setColor(UI.getControlBorderColor());
			int y = getPanelPadding() / 2;
			g.drawLine(0, y, s.width, y);
	    }
	    public Dimension getPreferredSize( JComponent c ) { return new Dimension( 0, getPanelPadding()); }
	}

}