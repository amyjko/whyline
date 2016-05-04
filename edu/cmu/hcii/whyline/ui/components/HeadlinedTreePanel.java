package edu.cmu.hcii.whyline.ui.components;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.JTree;
import javax.swing.event.*;
import javax.swing.tree.*;

import edu.cmu.hcii.whyline.trace.nodes.ClassNode;
import edu.cmu.hcii.whyline.trace.nodes.DynamicNode;
import edu.cmu.hcii.whyline.trace.nodes.Node;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;

/**
 * @author Andrew J. Ko
 *
 */
public abstract class HeadlinedTreePanel<T> extends HeadlinedPanel {

	private  final WhylineScrollPane scroller;

	private  final JTree tree;
	
	private final TreeSelectionListener selectionListener = new TreeSelectionListener() {
		public void valueChanged(TreeSelectionEvent e) {
			final Node<?> selection = getSelectedNode();
			if(selection != null) {
				if(selection.update()) {
						updateTree();
				}
				handleSelection();
			}
		}
    };

	private final TreeExpansionListener expansionListener = new TreeExpansionListener() {
		public void treeExpanded(TreeExpansionEvent e) {
			handleExpansion();
		}
		public void treeCollapsed(TreeExpansionEvent e) {}
    };

	private final Node<T> root = new  Node<T>() {
		public int compareTo(Node<T> o) { return 0; }
		public String toString() { return "root"; }
		public boolean isLeaf() { return false; }
		protected void determineChildren() {}
		protected boolean performUpdate() { return false; }
	};

	private final TreeModel model = new TreeModel() {
		public Object getChild(Object parent, int index) { return ((Node<?>)parent).get(index); }
		public int getChildCount(Object parent) { return ((Node<?>)parent).getNumberOfChildren();  }
		public Object getRoot() { return root; }
		public int getIndexOfChild(Object parent, Object child) { return ((Node<?>)parent).indexOf(child); }
		public boolean isLeaf(Object node) { return ((Node<?>)node).isLeaf(); }
		public void valueForPathChanged(TreePath path, Object newValue) {}
		public void addTreeModelListener(TreeModelListener l) { listeners.add(l); }
		public void removeTreeModelListener(TreeModelListener l) { listeners.remove(l); }
	};

	private final List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
	
	public HeadlinedTreePanel(String header, WhylineUI whylineUI) {
		
		super(header, whylineUI);
		
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
		    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		    	Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	    		c.setForeground(sel ? UI.getHighlightTextColor() : UI.getControlTextColor());
	    		// Some strange problem with using html in a tree cell renderer where the attributes of the html
	    		// aren't reset from cell to cell, causing problems with the colors. I reset them by setting the text to non-html here.
	    		String text = ((javax.swing.JLabel)c).getText();
	    		((javax.swing.JLabel)c).setText("");
		    	if(value instanceof ClassNode)
		    		c.setFont(UI.getMediumFont().deriveFont(Font.BOLD));
		    	else
		    		c.setFont(UI.getMediumFont());
	    		((javax.swing.JLabel)c).setText(text);
		    	return c;
		    }
		};
		renderer.setBackground(UI.getControlBackColor());
		renderer.setForeground(UI.getControlTextColor());
		renderer.setBorder(null);
		renderer.setFont(UI.getMediumFont());

		tree = new JTree(model);
		tree.setRootVisible(false);
		tree.setEditable(false);
		tree.setScrollsOnExpand(true);
		tree.setExpandsSelectedPaths(true);
		tree.setFocusable(false);
		tree.setOpaque(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.putClientProperty("JTree.lineStyle", "None");
		tree.setCellRenderer(renderer);
		
		tree.addTreeSelectionListener(selectionListener);
		tree.addTreeExpansionListener(expansionListener);
	    
		scroller = new WhylineScrollPane(tree);
		scroller.setBorder(new WhylineControlBorder());
		
		setContent(scroller);
	
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), UI.getDefaultInfoPaneHeight(whylineUI)));
		
	}

	public final Node<T> getRoot() { return root; }
	
	/**
	 * Updates the tree based on the model's state, retaining the current selection and selection expansion state, if the corresponding model elements still exist.
	 * Before updating the tree, it updates the selected node to display its curent value.
	 */
	protected final void updateTree() {
		
		// Before updating the tree and losing all of the interactive state, remember the selection and its expansion state
		Node<?> selectedNode = getSelectedNode();
		TreePath selectionPath = getSelectionPath();
		boolean selectionIsExpanded = selectionPath == null ? false : tree.isExpanded(selectionPath);

		// Before we do this, update the selected node, assuming state has changed that prompted this call.
		if(selectedNode != null)
			selectedNode.update();

		tree.removeTreeSelectionListener(selectionListener);
		for(TreeModelListener listener : listeners)
			listener.treeStructureChanged(new TreeModelEvent(this, new TreePath(root)));
		tree.addTreeSelectionListener(selectionListener);
		tree.clearSelection();

		// Select and update the selected node.
		if(selectionPath != null) 
			show(selectionPath, selectionIsExpanded);
		
	}
	
	public final void clearSelection() {
		
		tree.clearSelection();
		tree.scrollRowToVisible(0);

	}
	
	public final Node<?> getSelectedNode() {
		
		if(getSelectionPath() == null) return null;
		Object selection = getSelectionPath().getLastPathComponent();
		return selection instanceof Node ? (Node<?>)selection : null;
		
	}
	
	public final DynamicNode<?> getSelectedDynamicNode() {
		
		Node<?> node = getSelectedNode();
		return node == null ? null : node instanceof DynamicNode ? (DynamicNode<?>)node : null;
		
	}

	public final void show(TreePath path, boolean expandLast) {

		// Stop listening for a moment, since we're doing this programmatically.
		tree.removeTreeSelectionListener(selectionListener);
		
		// Expand everything in the path except for the final item, then select the final item and scroll to it.
		tree.expandPath(expandLast ? path : path.getParentPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.repaint();
		
		// Resume listening to the user's actions.
		tree.addTreeSelectionListener(selectionListener);

	}
	
	protected abstract void handleSelection();
	
	protected abstract void handleExpansion();

	public TreePath getSelectionPath() { return tree.getSelectionPath(); }

}