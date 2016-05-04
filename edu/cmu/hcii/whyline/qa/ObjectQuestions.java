/**
 * 
 */
package edu.cmu.hcii.whyline.qa;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.bytecode.QualifiedClassName;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;

public class ObjectQuestions {

	private final SortedMap<String, SortedSet<Object>> questions;
	
	private final ObjectState object;
	private final QuestionMenu menu;
	
	public ObjectQuestions(ObjectState object, QuestionMenu menu) {
		
		this.object = object;
		this.menu = menu;
		
		// Put lower case first, then upper case, sorting within case.
		questions = new TreeMap<String, SortedSet<Object>>(new Comparator<String>() {
			public int compare(String s1, String s2) {
				boolean s1Lower = Character.isLowerCase(s1.charAt(0));
				boolean s2Lower = Character.isLowerCase(s2.charAt(0));
				if(s1Lower) {
					if(s2Lower) return s1.compareTo(s2);
					else return -1;
				}
				else {
					if(s2Lower) return 1;
					else return s1.compareTo(s2);
				}
			}
		});
		
	}
	
	public void addFieldQuestions(QuestionMenuMaker m, FieldInfo field, boolean important) {

		// Find the least specific class
		QualifiedClassName base = object.getTrace().getClassIDs().getBaseClassOf(field.getTypeName());
		String name = base.getSimpleName();

		// Strip any brackets
		int brackets = name.indexOf('[');
		if(brackets >= 0) name = name.substring(0, brackets);
		
		// Is this in camel case? Find the last hump (a capital letter preceded by a lower case letter).
		int latestHump = -1;
		for(int i = 0; i < name.length(); i++) {
			if(Character.isUpperCase(name.charAt(i)))
				latestHump = i;
		}
		// If we found a hump and its followed by a lower case latter, use it!
		if(latestHump >= 0 && latestHump + 1 < name.length() && Character.isLowerCase(name.charAt(latestHump + 1)))
			name = name.substring(latestHump);
		
		// Pluralize it if it isn't already.
		if(!name.endsWith("s")) name = name + "s";
		getQuestionSet(name).add(m);

	}

	private SortedSet<Object> getQuestionSet(String type) {
	
		SortedSet<Object> set = questions.get(type);
		if(set == null) {
			set = new TreeSet<Object>();
			questions.put(type, set);
		}
		return set;
	
	}

	public void addPrimitiveOutputQuestion(Question<?> q) { getQuestionSet("primitive").add(q); }
	
	public int getNumberOfQuestions() { 
		
		int total = 0;
		for(SortedSet<Object> set : questions.values())
			total += set.size();
		return total;

	}
	
	public QuestionMenu createMenu() {

		int count = getNumberOfQuestions();
		boolean group = count > 10;

		SortedSet<Object> singletons = new TreeSet<Object>();

		boolean lastLower = true;
		for(String type : questions.keySet()) {

			if(lastLower && Character.isUpperCase(type.charAt(0)))
				menu.addSeparator();
			lastLower = Character.isLowerCase(type.charAt(0));
			SortedSet<Object> set = questions.get(type);
			if(set.size() == 1 && group) singletons.add(set.first());
			else add(set, "<i>" + type + "</i>", group);
			
		}

		if(singletons.size() > 0) 
			menu.addSeparator();
		
		// We only add these if we didn't add them above.
		if(group)
			add(singletons, "<b>other</b> fields", true);

		return menu;
		
	}
	
	private void add(SortedSet<Object> set, String name, boolean group) {
		
		String description = "Questions about the <b>current value</b> of " + name + " of " + object.getDisplayName(true, -1);
		
		if(!set.isEmpty()) {
		
			QuestionMenu questions = group ? new QuestionMenu(menu.getAsker(), description,name) : menu;
			
			for(Object m : set) {
				if(m instanceof QuestionMenu) {
					if(((QuestionMenu)m).getNumberOfItems() > 0) 
						questions.addMenu((QuestionMenu)m);
				}
				else if(m instanceof Question)
					questions.addQuestion((Question<?>)m);
				else if(m instanceof QuestionMenuMaker)
					questions.addMaker((QuestionMenuMaker)m);
			}
			
			if(questions != menu)
				menu.addMenu(questions);
			
		}

	}
	
}