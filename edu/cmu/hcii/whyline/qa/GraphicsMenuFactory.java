package edu.cmu.hcii.whyline.qa;

import java.util.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.io.*;
import edu.cmu.hcii.whyline.trace.*;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;
import edu.cmu.hcii.whyline.util.*;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

/**
	 * Just holds the common state between the question generation methods, so we don't have to pass it around through invocations.
	 * Better than one big method, because we can reuse these methods elsewhere.
	 * 
	 * @author Andrew J. Ko
	 *
	 */
public class GraphicsMenuFactory {
	
	private final Asker asker;
	private final Trace trace;
	private final SortedSet<GraphicalEventAppearance> renderEvents;
			
	public GraphicsMenuFactory(Asker asker, SortedSet<GraphicalEventAppearance> renderEvents) {
	
		this.asker = asker;
		this.trace = asker.getTrace();
		this.renderEvents = renderEvents;
		
	}
	
	public static QuestionMenu getQuestionMenu(Asker asker, SortedSet<GraphicalEventAppearance> renderEvents) {
		
		return (new GraphicsMenuFactory(asker, renderEvents)).getQuestionMenu();
		
	}
		
	/**
	 * Finds objects that indirectly invoked the output under the mouse by inspecting the call stack of each render event.
	 */
	private long[] findControlEntities() {

		// These are ordered by the order of rendering; the stuff on the "bottom" of the screen should come last, the stuff on the "top" should come first.
		ArrayList<EntityRender> entityRenders = new ArrayList<EntityRender>();

		// Add all entities, keeping the most recent render event for each.
		for(GraphicalEventAppearance render : renderEvents) {

			// Get the call stack for this render event.
			CallStack stack = render.renderer.getCallStack();

			// Go through the call stack, updating data for familiar entities that we find.
			int distance = stack.getDepth();
			for(CallStackEntry entry : stack) {

				MethodInfo method = entry.getMethod();
				int invocationID = entry.getInvocationID();
				if(invocationID >= 0) {
					
					QualifiedClassName classOfInstanceCalled = trace.getInvocationClassInvokedOn(invocationID);
					boolean classIsReferenced = trace.classIsReferencedInFamiliarSourceFile(classOfInstanceCalled);
					long entityID = trace.getInvocationInstanceID(invocationID);
					// If its familiar, add the entity, and if necessary, update its distance from the render event.
					if(entityID > 0 && classIsReferenced) {

						// Find the corresponding object for this entity ID, if it exists.
						EntityRender entityRender = null;
						for(EntityRender temp : entityRenders)
							if(temp.entityID == entityID) entityRender = temp; 
						// If we haven't made one yet, make it.
						if(entityRender == null) {
							entityRender = new EntityRender(entityID, render, distance);
							entityRenders.add(entityRender);
						}
						// Did the render event we found occur later than the most recent one in our list?
						if(entityRender.event.renderer.getEventID() < render.renderer.getEventID()) {
							entityRender.event = render;
							entityRender.distance = distance;
						}

					}
				}
				distance--;
				
			}
			
		}
		
		// Now, sort the entities, placing the most recently rendered events earlier, and the entities more directly responsible earlier.
		Collections.sort(entityRenders, new Comparator<EntityRender>() {
			public int compare(EntityRender one,EntityRender two) {
				// If they don't have the same event, order by the later event.
				if(one.event.renderer.getEventID() != two.event.renderer.getEventID()) return -one.event.compareTo(two.event);
				// If they DO have the same event, order by the call stack distance to rendering the event.
				else return one.distance - two.distance; 
			}
		});

		// Create an array to pass to the primitive questions
		long[] entityIDs = new long[entityRenders.size()];
		int index = 0;
		for(EntityRender pair : entityRenders)
			entityIDs[index++] = pair.entityID;

		return entityIDs;
		
	}
	
	/**
	 * Finds objects that affected parameters used to render the output by following data dependencies of each parameter.
	 * @return 
	 */
	private List<FieldUse> findUpstreamOutputAffectingFields() {

		if(renderEvents.isEmpty()) return new ArrayList<FieldUse>(0);

		DataEntitySearchState state = new DataEntitySearchState();
		return state.fieldUses;
				
	}
	
	private static class FieldUse {
		
		int useID;
		long objectID;
		FieldInfo field;
		
		public FieldUse(int useID, long objectID, FieldInfo field) {
			
			this.useID = useID;
			this.objectID = objectID;
			this.field = field;
			
		}
		
	}
	
	private class DataEntitySearchState {
		
		ArrayList<FieldUse> fieldUses = new ArrayList<FieldUse>();
		TIntHashSet dependencies = new TIntHashSet();
		TIntHashSet newDependencies = new TIntHashSet();
		TIntHashSet visited = new TIntHashSet();

		public DataEntitySearchState() {
			
			GraphicalEventAppearance render = renderEvents.last();

			// Always skip the graphics context
			for(int arg = 1; arg < render.event.getNumberOfArgumentProducers(); arg++) {

				String name = render.event.getArgumentName(arg);
				Invoke invoke = (Invoke) trace.getInstruction(render.event.getEventID());
				QualifiedClassName argumentType = invoke.getMethodInvoked().getParsedDescriptor().getTypeOfArgumentNumber(arg - 1);
				if(argumentType == QualifiedClassName.INT)
					continue;

				Value value = trace.getOperandStackValue(render.event.getEventID(), arg);
				if(value.hasEventID()) {
							
					dependencies.add(value.getEventID());

					while(dependencies.size() > 0) {
					
						// Go through each dependency waiting to be analyzed and find its root dependencies.
						TIntIterator iterator = dependencies.iterator();
						while(iterator.hasNext()) {
							
							int eventID = iterator.next();

							Instruction inst = trace.getInstruction(eventID);
							
							visited.add(eventID);

							boolean isFieldUse = inst instanceof GETFIELD;
							QualifiedClassName fieldClassname = isFieldUse ? ((GETFIELD)inst).getFieldref().getClassname() : null;
							// HAAAAAAAAAAAACK!
							boolean isProjectClass = fieldClassname != null && !fieldClassname.getText().startsWith("java")  && !fieldClassname.getText().startsWith("sun");
							
							// Add operand stack dependencies if this isn't a field reference, or if it is, only if the field is part of the program (and not the SDK)
							if(!isFieldUse || isProjectClass) {
								for(Value vp : trace.getOperandStackDependencies(eventID))
									if(vp != null && vp.getEventID() >= 0)
										handleDependency(vp.getEventID());
							}
			
							int heapDependencyID = trace.getHeapDependency(eventID);
							if(heapDependencyID >= 0 && !visited.contains(heapDependencyID))
								handleDependency(heapDependencyID);
						
							IntegerVector objectDependencies = trace.getUnrecordedInvocationDependencyIDs(eventID);
							if(objectDependencies != null) {
								for(int i = 0; i < objectDependencies.size(); i++)
									handleDependency(objectDependencies.get(i));
							}
							
						}
						
						// Now that we've gone through these, make the new ones the next to iterate through and clear the new dependency set.
						TIntHashSet temp = newDependencies;
						dependencies.clear();
						newDependencies = dependencies;
						dependencies = temp;
						
					}
					
				}
							
			}			
			
		}
	
		public void handleDependency(int eventID) {
			
			if(visited.contains(eventID))
				return;
			
			visited.add(eventID);
			
			// Add uses of familiar, public or settable fields
			Instruction inst = trace.getInstruction(eventID); 
			if(inst instanceof GETFIELD) {
				FieldInfo field = trace.resolveFieldReference(((FieldrefContainer)inst).getFieldref());
				if(field != null) {
					boolean referenced = trace.classIsReferencedInFamiliarSourceFile(inst.getClassfile().getInternalName());
					boolean isPublic = field.isPublic() || !field.getSetters().isEmpty();
					if(referenced && isPublic) {
	
						long objectID = trace.getOperandStackValue(eventID, 0).getLong();
						
						// Is this a more recent use of this object and value?
						int existingUse = -1;
						for(int i = 0; i < fieldUses.size(); i++) {
							FieldUse use = fieldUses.get(i);
							if(use.objectID == objectID && use.field == field) {
								existingUse = i;
								break;
							}							
						}
						
						FieldUse use = new FieldUse(eventID, objectID, field);
						if(existingUse >= 0) {
							if(fieldUses.get(existingUse).useID < eventID)
								fieldUses.set(existingUse, use);
						}
						else
							fieldUses.add(use);
						
					}
				}
			}

			newDependencies.add(eventID);
			
		}

	}
	
	public QuestionMenu getQuestionMenu() {
		
		asker.processing(true);

		RenderEvent primitive = renderEvents.isEmpty() ? null : renderEvents.last().event;

		if(primitive == null)
			return null;
		
		String name = primitive.getDisplayName(false, -1);
		
		QuestionMenu questions = new QuestionMenu(asker, "Questions paint under the mouse.", "why did");
		
		// Generate a properties menu for the top most render event under the mouse.
		if(primitive != null) {
			questions.addMenu(getWhyDidQuestionsAboutPrimitive(primitive));
		}
				
		List<FieldUse> fieldUses = findUpstreamOutputAffectingFields();
		
		if(fieldUses.size() > 0) {

			QuestionMenu dataDependencies = 
				new QuestionMenu(asker,
						"Questions about object fields that affected the appearance of this <b>" + name +"</b>", 
						"<b>fields</b> affecting this"); 

			// group the fields
			SortedMap<String,List<FieldUse>> fieldsByType = new TreeMap<String,List<FieldUse>>();
			for(FieldUse use : fieldUses) {
				String type = use.field.getTypeName().getSimpleName();
				List<FieldUse> list = fieldsByType.get(type);
				if(list == null) {
					list = new ArrayList<FieldUse>();
					fieldsByType.put(type, list);
				}
				list.add(use);
			}

			for(String type : fieldsByType.keySet()) {

				QuestionMenu group = new QuestionMenu(asker, "Fields of type " + type, "<i>" + type + "s</i>");
				for(FieldUse use : fieldsByType.get(type)) {
	
					ObjectState obj  = trace.getObjectNode(use.objectID);
					QuestionMenu menu = getQuestionsAboutField(asker, obj, use.field, primitive.getEventID());
					group.addMenu(menu);
					menu.setLabel("<i>" + obj.getDisplayName(false, -1) + "</i>'s <b>" + use.field.getDisplayName(true, -1) + "</b>...");
					
				}
				dataDependencies.addMenu(group);
				
			}
			
			if(dataDependencies.getNumberOfItems() > 0)
				questions.addMenu(dataDependencies);
						
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Add questions about invokers of this output
		////////////////////////////////////////////////////////////////////////////////////////////////////

		QuestionMenu controlDependencyQuestions = 
			new QuestionMenu(asker, 
					"Questions about objects that were responsible for drawing this <b>%</b>",
					"<b>objects</b> rendering this", primitive);
		
		//  For each event under the mouse, find the set of instances of a familiar type. Collect the types in a set.
		long[] entityIDs = findControlEntities();
		for(long entityID : entityIDs) {

			ObjectState obj = trace.getObjectNode(entityID);
			controlDependencyQuestions.addMenu(getWhyDidQuestionsAboutObject(obj));
			
		}
		
		if(controlDependencyQuestions.getNumberOfItems() > 0) {
			questions.addMenu(controlDependencyQuestions);
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Add question about effects of input.
		////////////////////////////////////////////////////////////////////////////////////////////////////

		QuestionMenu whydidnt = new QuestionMenu(asker, "Why didn't questions about input and windows", "<b>windows</b>");
		
//		whydidnt.addQuestion(new WhyDidntInputAffectOutput(asker));		

		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Add questions about window types.
		////////////////////////////////////////////////////////////////////////////////////////////////////
		QuestionMenu windows = getWindowQuestions(asker);
		if(windows.getNumberOfItems() > 0) {
			whydidnt.addItemsOf(windows);
		}

		if(whydidnt.getNumberOfItems() > 0) {
			questions.addSeparator();
			questions.addMenu(whydidnt);
		}

		asker.processing(false);

		return questions;

	}
	
	public static QuestionMenu getWindowQuestions(Asker asker) {

		QuestionMenu questions = new QuestionMenu(asker, "Questions about windows that didn't appear.", "<b>windows</b> that didn't appear...");
		
		SortedSet<Classfile> windowClasses = asker.getTrace().getConcreteWindowClasses();
		if(windowClasses.size() > 0) {
			for(Classfile c : windowClasses) {
			
				questions.addQuestion(new WhyDidntWindowAppear(asker, c, "appear"));
			
			}
		}

		return questions;
		
	}
	
	private static class EntityRender {
		
		final long entityID;
		GraphicalEventAppearance event;
		int distance;
		
		public EntityRender(long entityID, GraphicalEventAppearance event, int distance) {
			
			this.entityID = entityID;
			this.event = event;
			this.distance = distance;
			
		}
		
	}

	private String pair(String argument, String value) {
		
		return "<b>" + argument + "</b> = " + value;
		
	}
	
	private QuestionMenu getWhyDidQuestionsAboutPrimitive(RenderEvent renderEvent) {

		QuestionMenu questions = new QuestionMenu(asker, "Questions about arguments used by this %", "<b>properties</b> of this <b>%</b>", renderEvent);

		// This graphics event depends on the arguments passed to it (skipping the graphics context, which we do below)
		for(int argumentIndex = 1; argumentIndex < renderEvent.getNumberOfArgumentProducers(); argumentIndex++) {
			
			Value value = renderEvent.getArgument(argumentIndex);
			questions.addItemsOf(
				makeArgumentMenu(asker, 
					renderEvent.getArgumentName(argumentIndex), 
					value, value.getDisplayName(true)));
			
		}

		// It also depends on the state of the graphics context.
		SetPaintEvent paint = renderEvent.getGraphicsState().getLatestPaintChange();
		if(paint != null)
			questions.addItemsOf(makeArgumentMenu(asker, "color", paint.getPaintProducedEvent(), Util.format(paint.getPaint(), true)));

		SetFontEvent font = renderEvent.getGraphicsState().getLatestFontChange();
		if(font != null)
			questions.addItemsOf(makeArgumentMenu(asker, "font", font.getFontProducedEvent(), Util.format(font.getFont(), true)));
		
		SetStrokeEvent stroke = renderEvent.getGraphicsState().getLatestStrokeChange();
		if(stroke != null) 
			questions.addItemsOf(makeArgumentMenu(asker, "stroke", stroke.getStrokeProducedEvent(), Util.format(stroke.getStroke(), true)));
		
		SetCompositeEvent composite = renderEvent.getGraphicsState().getLatestCompositeChange();
		if(composite != null) 
			questions.addItemsOf(makeArgumentMenu(asker, "composite", composite.getCompositeProducedEvent(), Util.format(composite.getComposite(), true)));

		return questions;
		
	}
	
	private static QuestionMenu makeArgumentMenu(Asker asker, String argument, Value value, String valueDescription) {
		
		QuestionMenu questions = new QuestionMenu(asker, "Questions about this " + argument, argument, value);
		questions.addQuestion(new WhyDidArgumentHaveValue(asker, value, argument, " = <b>" + valueDescription + "</b>"));

//		questions.addQuestion(new WhyDidntArgumentChange(asker, value, "not <b>change</b>"));

		// Originally, there were going to by why did and why didn't questions about primitive arguments, but I determined
		// that this question was infeasible. I've left the structure the same, in case I add it later. For now, callers of this method
		// are recommended to add the items of this menu returned.
		return questions;
		
	}
			
	private QuestionMenu getWhyDidQuestionsAboutObject(final ObjectState object) {
		
		final Trace trace = asker.getTrace();

		Classfile classfile = trace.getClassfileByName(trace.getClassnameOfObjectID(object.getObjectID()));

		ObjectQuestions questions = new ObjectQuestions(object, new QuestionMenu(asker, "Questions about <b>%</b>", "%", object));

		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Determine all publicly modifiable output affecting fields
		////////////////////////////////////////////////////////////////////////////////////////////////////

		SortedSet<FieldInfo> publiclyModifiableOutputAffectingFields = trace.getPublicOutputAffectingFieldsOf(classfile);
				
		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Add questions for each field based on its current value
		////////////////////////////////////////////////////////////////////////////////////////////////////

		for(final FieldInfo field : publiclyModifiableOutputAffectingFields) {

			// Defer the creation of this question until later since it's expensive to find the field's current value.
			QuestionMenuMaker maker = new QuestionMenuMaker(asker) {
				public String getMenuLabel() { return "<b>" + field.getDisplayName(true, -1) + "</b>"; }
				public Named getSubject() { return field; }
				public QuestionMenu make() { return getQuestionsAboutField(asker, object, field, asker.getCurrentScope().getInputEventID()); }
			};
			questions.addFieldQuestions(maker, field, true);
			
		}
							
		////////////////////////////////////////////////////////////////////////////////////////////////////
		// Now that we've filled the object menu with field questions, create the object menu
		////////////////////////////////////////////////////////////////////////////////////////////////////
		
		QuestionMenu menu = questions.createMenu();

		// Put some questions at the top above the ones we just created.
		if(menu.getNumberOfItems() > 0)
			menu.insertSeparator();
		menu.insertQuestion(new WhyDidObjectGetCreated(asker, object, "<b>get created</b>"));

		// Add questions about output invoking methods.
		if(classfile != null) {
			List<Question<?>> methodQuestions = new ArrayList<Question<?>>();
			SortedSet<MethodInfo> methods = trace.getPublicOutputInvokingMethodsOf(classfile);
			for(MethodInfo method : methods) {
				
				if(method.isVirtual() && !method.isInstanceInitializer() && trace.classIsReferencedInFamiliarSourceFile(classfile.getInternalName()))
					methodQuestions.add(new WhyDidntMethodExecute(asker, method, object.getObjectID(), "execute"));
				
			}
	
			if(methodQuestions.size() > 0) {
				menu.addSeparator();
				if(methodQuestions.size() > 10) {
					QuestionMenu callsMenu = new QuestionMenu(asker, "Questions about output invoking methods", "methods...");
					for(Question<?> q : methodQuestions)
						callsMenu.addQuestion(q);
					menu.addMenu(callsMenu);
				}
				else {
					for(Question<?> q : methodQuestions)
						menu.addQuestion(q);
				}
			}
		}

		return menu;
		
	}
	
	private  QuestionMenu getQuestionsAboutField(Asker asker, ObjectState object, FieldInfo field, int beforeID) {
		
		Trace trace = object.getTrace();
		
		QuestionMenu menu = new QuestionMenu(asker, "Questions about the field  <b>%</b>","%", field);

		// If we find an assignment, find a description of the value assigned.
		int assignmentID = trace.findFieldAssignmentBefore(field, object.getObjectID(), beforeID);

		String valueString = "unknown";
		Value value = null;
		
		// If we found an assignment or instantiation
		if(assignmentID >= 0) {
			Instruction assignment = trace.getInstruction(assignmentID);
			if(assignment instanceof PUTFIELD) {
				value = trace.getDefinitionValueSet(assignmentID);
				if(value instanceof TraceValue)
					assignmentID = ((TraceValue)value).getEventID();
				valueString = value.getDisplayName(true);
			}
			else if(assignment instanceof NEW) {
				
				Object defaultValue = field.getDefaultValue();
				valueString = "" + defaultValue;
				
			}
			else assert false : "What do I do with " + assignment + "?";
		}
		
		// Add the question about the current value
		menu.addQuestion(new WhyDidFieldHaveValue(asker, object, field, " = " + valueString));

		// Add a question about why it didn't have a different value
		menu.addQuestion(new WhyDidntFieldChange(asker, object, field, value, "change"));
		
		// If the field points to an object, add questions about the object.
		if(value != null && value.isObject() && value.getLong() > 0) {
		
			menu.addSeparator();
			// Get all the questions for the object, but instead of using a separate menu,
			// add them to the existing menu for this field value.
			menu.addMenu(getWhyDidQuestionsAboutObject(trace.getObjectNode(value.getLong())));

		}

		return menu;

	}
	
}