package edu.cmu.hcii.whyline.analysis;

import java.io.*;
import java.util.*;

import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.PersistentState;
import edu.cmu.hcii.whyline.ui.events.*;
import edu.cmu.hcii.whyline.util.IntegerVector;
import gnu.trove.TObjectIntHashMap;

/**
 * @author Andrew J. Ko
 *
 */
public class Usage {

	private final Trace trace;

	private final ArrayList<AbstractUIEvent<?>> events = new ArrayList<AbstractUIEvent<?>>(100);
	
	private SortedMap<String,TObjectIntHashMap<String>> methodTimes = new TreeMap<String,TObjectIntHashMap<String>>();
	private SortedSet<String> methodNames = new TreeSet<String>();
	private Map<String,List<Visit>> visitsByKey = new HashMap<String,List<Visit>>();
	
	private final TObjectIntHashMap<String> distances;
	
	private class Visit {
		
		private final MethodInfo method;
		private final ArrayList<AbstractUIEvent<?>> events = new ArrayList<AbstractUIEvent<?>>();
		
		public Visit(MethodInfo method) {
			
			this.method = method;
			
		}

		public void include(AbstractUIEvent<?> event) { events.add(event); }

		public boolean contains(long l) {

			if(events.isEmpty()) return false;
			
			long min = events.get(0).getTime();
			long max = events.get(events.size() - 1).getTime();
			
			return min <= l && max >= l;
		
		}
		
		public String toString() { return "visited "+ method.getQualifiedNameAndDescriptor(); }
		
	}
	
	public Usage(Trace trace, File folder, MethodInfo buggyMethod) throws IOException {
		
		this.trace = trace;

		MethodDependencyGraph graph = new MethodDependencyGraph(trace);
		distances = graph.getMethodDistancesToMethod(buggyMethod);

		generateUsageStatistics(folder);
		
	}

	private int median(IntegerVector distances) {
		
		// Sort the distances and find the median.
		distances.sortInAscendingOrder();
		int size = distances.size();
		int median = 
			size % 2 == 0 ?
					(distances.get((size-1) / 2) + distances.get((size-1) / 2 + 1)) / 2 :
						distances.get(size / 2);
		return median;
		
	}
	
	private void generateUsageStatistics(File folder) throws IOException {
		
		File[] logs = folder.listFiles();
		
		try {
		
			for(File log : logs) 
				if(log.isFile())
					analyzeUsageLog(log);
			
		} catch(IOException e) {
			e.printStackTrace();
		}

		// Save a csv with the time series data
		FileWriter series = new FileWriter(new File(folder, "series.csv"));
		series.write("id");
		// Write all of the minute headers.
		for(int i = 0; i < 30; i++)
			series.write(","  + String.valueOf(i));
		series.write('\n');
		// For each log file...
		for(String key : methodTimes.keySet()) {
			series.write(key);
			
			List<Visit> visits = visitsByKey.get(key);

			double[] averageDistanceEachMinute = computeAggregateDistanceByMinute(visits);
			for(int i = 0; i < averageDistanceEachMinute.length; i++) {
				double averageDistanceInMinute = averageDistanceEachMinute[i];
				series.write("," + (averageDistanceInMinute < 0 ? "" : averageDistanceInMinute));
			}
			series.write('\n');
		}
		series.close();
		
		
		// Save a csv with the aggregate statistics.
		FileWriter writer = new FileWriter(new File(folder, "results.csv"));

		// Write the header labels
		writer.write("id");
		writer.write(",min");
		writer.write(",median");
		writer.write(",median10");
		for(String method : methodNames) {
			String distance = distances.containsKey(method) ? String.valueOf(distances.get(method)) : "?";
			writer.write("," + method + "(" + distance + ")");
		}
		writer.write('\n');
		
		// For each log file...
		for(String key : methodTimes.keySet()) {

			// Write the log file name
			writer.write(key);

			// What methods were visited in the last five minutes?
			List<Visit> visits = visitsByKey.get(key);
			
			// Compute last time
			long lastTime = -1;
			for(Visit visit : visits) {
				if(visit.events.size() > 0)
					lastTime = visit.events.get(visit.events.size() - 1).getTime();
			}

			// How many minutes
			long endThreshold = 5 * 1000 * 60;
			
			Set<String> methodsVisitedInLastTenMinutes = new HashSet<String>();
			for(Visit visit : visits) {
				if(visit.events.size() > 0 && lastTime - visit.events.get(0).getTime() < endThreshold)
					methodsVisitedInLastTenMinutes.add(visit.method.getQualifiedNameAndDescriptor());
			}
			
			TObjectIntHashMap<String> times = methodTimes.get(key);

			// Write the min, median, median 10
			int min = Integer.MAX_VALUE;
			IntegerVector orderedDistances = new IntegerVector(methodNames.size());
			IntegerVector orderedDistancesInLastTenMinutes = new IntegerVector(methodNames.size());
			for(String method : methodNames) {
				// If the method has a distance and the user read it, use it to compute the min and medians
				if(distances.containsKey(method) && times.get(method) > 0) {
					int distance = distances.get(method);
					min = Math.min(min, distance);
					orderedDistances.append(distance);
					if(methodsVisitedInLastTenMinutes.contains(method))
						orderedDistancesInLastTenMinutes.append(distance);
				}
			}
			int median = orderedDistances.size() == 0 ? -1 : median(orderedDistances);
			int median10 = orderedDistancesInLastTenMinutes.size() == 0 ? -1 : median(orderedDistancesInLastTenMinutes);
			
			writer.write("," + min + "," + median + "," + median10);
						
			// Write the method times for each method...
			for(String method : methodNames) {
				
				writer.write(",");
				writer.write(String.valueOf(times.get(method)));
								
			}

			// Finish the row
			writer.write('\n');

		}
		
		writer.close();
		
	}
	
	private double[] computeAggregateDistanceByMinute(List<Visit> visits) {

		// First time? Last time?
		long firstTime = -1;
		long lastTime = -1;
		for(Visit visit : visits) {
			if(visit.events.size() > 0) {
				if(firstTime < 0) firstTime = visit.events.get(0).getTime();
				lastTime = visit.events.get(visit.events.size() - 1).getTime();
			}
		}
		
		// -1 represents no methods visited
		int minutes = Math.min(30, (int) (lastTime - firstTime) / (1000 * 60) + 1);
		double[] averageDistances = new double[Math.max(minutes, 30)];
		Arrays.fill(averageDistances, -1);
		
		// Loop through all 30 minutes of time.
		double lastAverage = -1; // for propagating to next minutes if no activity.
		double lastMin = -1;
		double minDistance = -1;
		for(int index = 0; index < minutes; index++) {

			// Find all visits that intersect this period
			double totalDistance = 0;
			double totalVisits = 0;
			for(Visit visit : visits) {
				if(visit.contains(index * 1000 * 60 + firstTime) && distances.containsKey(visit.method.getQualifiedNameAndDescriptor())) {
					int distance = distances.get(visit.method.getQualifiedNameAndDescriptor());
					totalDistance += distance;
					totalVisits++;
					if(minDistance < 0) minDistance = distance;
					else minDistance = Math.min(minDistance, distance);
				}
			}
			lastMin = minDistance;
			if(totalVisits > 0) {
				lastAverage = totalDistance / totalVisits;
			}
			averageDistances[index] = lastMin;
						
		}
		
		return averageDistances;
	
	}

	private void analyzeUsageLog(File log) throws IOException {

		String key = log.getName();
		
		if(!log.getName().endsWith(".log")) {
			System.err.println("Skipping " + log.getName());
			return;
		}
		
		System.err.println("Reading  " + log.getName() + "\n");

		PersistentState state = new PersistentState(null, trace, log);
		
		List<AbstractUIEvent<?>> events = new ArrayList<AbstractUIEvent<?>>();

		int count = 0;
		for(String entry : state.getLog()) {
			
			String[] args = entry.split(":");
			if(args.length > 0) {
			
				UIEventKind kind = UIEventKind.fromType(args[0]);
				if(kind != null) {
					AbstractUIEvent<?> event = kind.create(trace, args);
					events.add(event);
				}			
				
			}
			count++;
			
		}
		
		TObjectIntHashMap<String> times = new TObjectIntHashMap<String>();
		
		long lastTime = events.get(events.size() - 1).getTime();
		
		IntegerVector distanceHistory = new IntegerVector(events.size());
		MethodInfo lastMethodVisited = null;
		
		int distanceSumInLastMinute = 0;
		int visitCountInLastMinute = 0;
		long lastMinute = events.get(0).getTime();
		
		int distanceAggregationPeriod = 3000;
		
		Visit visit = null;
		ArrayList<Visit> visits = new ArrayList<Visit>();
		
		// Analyze lines. Duration of hover = time of next hover - time of hover. 
		LineHover lastLineEvent = null;
		for(AbstractUIEvent<?> event : events) {
			
			if(event instanceof LineHover || event instanceof NoLineHover) {

				if(lastLineEvent != null) {
					int time = (int) (event.getTime() - lastLineEvent.getTime());
					Line line = lastLineEvent.getEntity();
					if(line != null && line.getFile() instanceof JavaSourceFile) {
						MethodInfo method = ((JavaSourceFile)line.getFile()).getMethodOfLine(line);
						if(method != null) {

							if(visit != null)
								visit.include(event);

							if(visit == null || method != lastMethodVisited) {
								visit = new Visit(method);								
								visits.add(visit);
							}

							lastMethodVisited = method;

							String name = method.getQualifiedNameAndDescriptor();
							methodNames.add(name);
							int total = times.containsKey(key) ? times.get(key) : 0;
							times.put(name, total + time);
							
						}
					}
				}
				
				lastLineEvent = event instanceof LineHover ? (LineHover)event : null;

			}
			
		}
		
		visitsByKey.put(key, visits);
		
		methodTimes.put(key, times);
		
	}
	
}