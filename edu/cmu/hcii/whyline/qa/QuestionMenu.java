package edu.cmu.hcii.whyline.qa;

import java.util.ArrayList;

import javax.swing.*;

import edu.cmu.hcii.whyline.ui.*;
import edu.cmu.hcii.whyline.ui.components.*;
import edu.cmu.hcii.whyline.ui.events.AbstractUIEvent;
import edu.cmu.hcii.whyline.ui.events.LoggedAction;
import edu.cmu.hcii.whyline.util.Named;

public class QuestionMenu implements Comparable<QuestionMenu> {

	private final ArrayList<Object> items = new ArrayList<Object>();
	
	private final Asker asker;
	private final String description;
	private String label;
	private final Named subject;
	private QuestionMenu parent = null;
	
	public static int MENU_ITEM_HEIGHT = 0;

	public QuestionMenu(Asker asker, String description, String label, Named subject) {

		this.asker = asker;
		this.subject = subject;
		
		String displayName = subject== null ? null : subject.getDisplayName(true, -1);
		
		if(subject != null)
			label = label.replace("%", displayName);
		
		this.label = label;

		if(subject != null)
			description = description.replace("%", displayName);

		this.description = description;

	}
	
	public QuestionMenu(Asker asker, String description, String label) {

		this(asker, description, label, null);
		
	}

	public Asker getAsker() { return asker; }

	/**
	 * HTML tags are allowed, but don't add the <html> prefix.
	 */
	public void setLabel(String newLabel) {
		
		this.label = newLabel;
		
	}

	public String getDescription() { return description; }
	
	public String getMenuLabel() { return label; }
	
	public Named getSubject() { return subject; }
	
	public int getNumberOfItems() { return items.size(); }
	
	public Iterable<Object> getItems() { return items; }
	
	public void addQuestion(Question<?> question) { items.add(question); }

	public void insertQuestion(Question<?> question) { items.add(0, question); }

	public void insertSeparator() { items.add(0, null); }

	public void addItemsOf(QuestionMenu menu) {

		items.addAll(menu.items);
		
	}

	public void addMenu(QuestionMenu menu) {
		
		assert menu != this : "Can't add menu to itself.";
		assert menu.parent == null : "" + this + " is already in " + parent;
		items.add(menu);
		menu.parent = this;
		
	}
		
	public void addMaker(QuestionMenuMaker maker) {
		
		items.add(maker);
		
	}
	
	public void addSeparator() {
		
		items.add(null);
		
	}
	
	public void addMessage(String message) {

		items.add(message);
		
	}

	public WhylinePopup generatePopupMenu() {

		if(MENU_ITEM_HEIGHT <= 0)
			MENU_ITEM_HEIGHT = (new MessageItem(null, "Get me the height!")).getPreferredSize().height;

		WhylinePopup menu = new WhylinePopup(getMenuLabel());
		for(Object item : items) {

			if(item == null) menu.addSeparator();
			else if(item instanceof QuestionMenu) { if(((QuestionMenu)item).getNumberOfItems() > 0) menu.add(((QuestionMenu)item).generateMenu()); }
			else if(item instanceof Question) menu.add(new QuestionItem((Question<?>)item, this));
			else if(item instanceof String) menu.add(new MessageItem(null, (String)item));
			else throw new RuntimeException("Not adding " + item + "of type " + item.getClass());
			
		}
		
		menu.setFont(UI.getMediumFont());
		
		return menu;
				
	}
	
	public Menu generateMenu() {
		
		Menu menu = new Menu(getMenuLabel(), this);
		return menu;
		
	}

	public int compareTo(QuestionMenu menu) { return getMenuLabel().compareTo(menu.getMenuLabel()); }
	
	public String toString() { return "questions about \"" + subject.getDisplayName(false, -1) + "\""; }
	
	public static class Menu extends WhylineMenu {

		protected QuestionMenu menu;
		private boolean itemsAdded = false;
		
		public Menu(String name, QuestionMenu menu) {
			
			super("<html>" + name + "</html>");

			this.menu = menu;
			
			setFont(UI.getMediumFont());
			
			if(menu != null)
				setToolTipText("<html>" + menu.getDescription());

		}
		
		public String getDescription() { return menu.getDescription(); }
		
		public Named getSubject() { return menu.getSubject(); }
		
		public void addItemsIfNecessary(boolean make) {
			
			if(itemsAdded) return;
			itemsAdded = true;

			removeAll();
			
			int maxHeight = 800;
			int totalHeight = 0;
			
			JMenu menuToAddTo = this;
			
			for(Object item : menu.getItems()) {

				totalHeight += MENU_ITEM_HEIGHT;
				if(totalHeight > maxHeight - MENU_ITEM_HEIGHT) {

					menuToAddTo = new MoreMenu();
					add(menuToAddTo);
					totalHeight = 0;
					
				}

				if(item == null) addSeparator();
				else if(item instanceof QuestionMenu) { if(((QuestionMenu)item).getNumberOfItems() > 0) menuToAddTo.add(((QuestionMenu)item).generateMenu()); }
				else if(item instanceof Question) menuToAddTo.add(new QuestionItem((Question<?>)item, menu));
				else if(item instanceof String) menuToAddTo.add(new MessageItem(this, (String)item));
				// Just make a place holder
				else if(item instanceof QuestionMenuMaker) menuToAddTo.add(new MakerMenu(menu.getAsker(), (QuestionMenuMaker)item));
				else throw new RuntimeException("Not adding " + item + "!");
				
			}

			validate();
			repaint();

			// Hide it and show it in order to update the contents.
			if(getPopupMenu().isShowing()) {
			
				getPopupMenu().setVisible(false);
				getPopupMenu().setVisible(true);
				
			}
			
		}

	}
	
	private static boolean makerMaking = false;
	private static Object waiter = new Integer(0);

	public static class MakerMenu extends Menu {
		
		private final Asker asker;
		private final QuestionMenuMaker maker;
		private boolean addedNote = false;
		private Thread makerThread;
		
		public MakerMenu(Asker asker, QuestionMenuMaker maker) {

			super(maker.getMenuLabel(), null);
			
			this.asker = asker;
			this.maker = maker;
			
			add(new MessageItem(this, "hover to gather questions..."));

		}
		
		public String getDescription() { return menu == null ? "Questions about (still making menu...)" : menu.description; }

		public Named getSubject() { return maker.getSubject(); }
				
		public void addItemsIfNecessary(boolean itemSelected) {
						
			if(menu != null) {
				super.addItemsIfNecessary(true);
				return;
			}
			
			// Before we can let the superclass do this, we have to compute the menus first.
			if(itemSelected) {

				if(!addedNote) {
					removeAll();
					add(new MessageItem(this, "gathering questions..."));
					addedNote = true;
				}

				if(maker.wasCanceled())
					maker.uncancel();
				
				// Only allow one maker to make at a time to prevent concurrent operations on the trace.
				makerThread = new Thread() {
					public void run() {
	
						synchronized(waiter) {

							asker.processing(true);

							makerMaking = true;
							menu = maker.make();
							makerMaking = false;
							if(maker.wasCanceled()) {
								menu = null;
							}
							else  {
								if(menu.getNumberOfItems() == 0)
									menu.addMessage("no questions");
								addItemsIfNecessary(true);
							}
							waiter.notifyAll();

							asker.processing(false);

						}
							
					}
				};

				makerThread.start();

			}

		}

		public void cancel() {
			
			if(menu != null) {}
			else if(makerThread == null) {}
			else maker.cancel();
			
		}
		
		public String toString() { return "maker menu for " + getSubject(); }
		
	}
	
	public static class MoreMenu extends WhylineMenu {
		
		public MoreMenu() {
			
			super("");
			
			setText("<html><i>more...</i></html>");
			setFont(UI.getMediumFont());
			
		}
		
	}
	
	public static class QuestionItem extends WhylineMenuItem {

		private final QuestionMenu menu;
		private final Question<?> question;
				
		public QuestionItem(Question<?> q, QuestionMenu menu) {
			
			super("");

			this.menu = menu;
			this.question = q;

			setText("<html>" + q.getQuestionText());

			addActionListener(new LoggedAction((WhylineUI)question.asker) {
				protected AbstractUIEvent<?> act() { 
					question.asker.answer(question);
					return null;
				}
			});
			
			setToolTipText("<html>" + q.getQuestionExplanation());
			
		}
		
		public Object getSubject() { return question.getSubject(); }
		public Question<?> getQuestion() { return question; }

	}
		
	public static class MessageItem extends WhylineMenuItem {
		
		private final Menu menu;

		public MessageItem(Menu menu, String message) {
			
			super("<html><i>" + message + "</i></html>");
		
			this.menu = menu;
			setEnabled(false);
			setOpaque(true);
			
		}
		
		public Menu getParentMenu() { return menu; }
		
	}
	
}
