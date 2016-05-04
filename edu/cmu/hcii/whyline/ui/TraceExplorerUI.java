package edu.cmu.hcii.whyline.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTree;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;
import edu.cmu.hcii.whyline.trace.Trace;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;
import edu.cmu.hcii.whyline.util.Util;

/**
 * @author Andrew J. Ko
 *
 */
public class TraceExplorerUI extends WhylinePanel implements Scrollable {
	
	private final Trace trace;
	
	private final JTree tree;

	private class MethodNode implements TreeNode {
		
		private final ThreadNode thread;
		private final MethodInfo method;
		private  int count;
		
		public MethodNode(ThreadNode thread, MethodInfo method) { 
			
			this.thread= thread;
			this.method = method;
			
		}
		
		public Enumeration<TreeNode> children() { return null; }
		public int getIndex(TreeNode node) { return 0; }
		public boolean getAllowsChildren() { return false; }
		public TreeNode getChildAt(int childIndex) { return null; }
		public int getChildCount() { return 0; }
		public TreeNode getParent() { return thread; }
		public boolean isLeaf() { return true; }
	
		public String toString() { return Util.fillOrTruncateString(Integer.toString(count), 10) + Util.fillOrTruncateString(method.getClassfile().getInternalName().getNameWithDots(), 30) + "     " + method.getJavaName(); }
		
	}
	
	private class ThreadNode implements TreeNode {
		
		private final int id;
		
		private MethodNode[] sort = null;
		
		private Map<MethodInfo,MethodNode> methods = new HashMap<MethodInfo,MethodNode>();
		
		private int percentDone;
		
		public ThreadNode(int threadID) { 
			
			this.id = threadID; 
			
			Thread counter = new Thread() {
				public void run() {
					Trace.ThreadIterator eventIDs = trace.getThreadIteratorAt(trace.getThreadFirstEventID(id));
					int count = 0;
					while(eventIDs.hasNextInThread()) {
						synchronized(this) {
							int eventID = eventIDs.nextInThread();
							Instruction inst = trace.getInstruction(eventID);
							MethodNode mNode = methods.get(inst.getMethod());
							if(mNode == null) {
								mNode = new MethodNode(ThreadNode.this, inst.getMethod());
								methods.put(inst.getMethod(), mNode);
								sort = null;
							}
							mNode.count++;
							count++;
							if(count % 10000 == 0) {
								percentDone = (count * 100) / trace.getNumberOfEventsInThread(id);
								repaint();
								sort();
							}
						}
							
					}
					 percentDone = 100;
					 SwingUtilities.invokeLater(new Runnable() {
						 public void run() { model.reload(); repaint(); }
					 });
				}
			};
			counter.start();
			
		}

		private synchronized MethodNode[] getSort() {
			if(sort == null) sort();
			return sort;
		}
		
		private synchronized void sort() {
			synchronized(this) {
				sort = new MethodNode[methods.size()];
				methods.values().toArray(sort);
				Arrays.sort(sort, new Comparator<MethodNode>() {
					public int compare(MethodNode o1, MethodNode o2) {
						return o2.count - o1.count;
					}
				});
			}
		}
		
		public Enumeration<MethodNode> children() { 
			return new Enumeration<MethodNode>() {
				int i = 0;
				public boolean hasMoreElements() { return i < getSort().length; }
				public MethodNode nextElement() { return getSort()[i++]; }
			};
		}
		public int getIndex(TreeNode node) {
			for(int i = 0; i < getSort().length; i++)
				if(getSort()[i] == node) return i;
			return -1;
		}

		public boolean getAllowsChildren() { return true; }
		public MethodNode getChildAt(int childIndex) { return getSort()[childIndex]; }
		public int getChildCount() { return getSort().length; }
		public TreeNode getParent() { return root; }
		public boolean isLeaf() { return false; }
		
		public String toString() { return Util.fillOrTruncateString(Integer.toString(trace.getNumberOfEventsInThread(id)), 10) + trace.getThreadName(id) + (percentDone < 100 ? " (" + percentDone + "%)" : ""); }
		
	}
	
	private final TreeNode root = new TreeNode() {

		private final gnu.trove.TIntObjectHashMap<ThreadNode> threads = new gnu.trove.TIntObjectHashMap<ThreadNode>();
		
		public Enumeration<TreeNode> children() {
			return null;
		}

		public int getIndex(TreeNode node) { return ((ThreadNode)node).id; }

		public boolean getAllowsChildren() { return true; }
		public TreeNode getChildAt(int id) { 
		
			ThreadNode node = threads.get(id);
			if(node == null) {
				node = new ThreadNode(id);
				threads.put(id, node);
			}
			return node;
			
		}
		public int getChildCount() { return trace.getNumberOfThreads(); }
		public TreeNode getParent() { return null; }
		public boolean isLeaf() { return false; }

		public String toString() { return "events"; }

	};

	
	private final DefaultTreeModel model = new DefaultTreeModel(root);

	public TraceExplorerUI(Trace trace) {
	
		this.trace = trace;
	
		this.tree = new JTree(model);
		tree.setShowsRootHandles(false);
		tree.setFont(UI.getFixedFont());

		setLayout(new BorderLayout());
		
		add(tree, BorderLayout.CENTER);
		
	}

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 100; }
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 100; }

	public Dimension getPreferredScrollableViewportSize() { return null; }

	public boolean getScrollableTracksViewportHeight() { return false; }
	public boolean getScrollableTracksViewportWidth() { return false; }

}
