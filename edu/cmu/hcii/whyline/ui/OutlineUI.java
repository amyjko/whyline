package edu.cmu.hcii.whyline.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.*;

import javax.swing.JToolBar;
import javax.swing.tree.*;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.source.*;
import edu.cmu.hcii.whyline.trace.nodes.*;
import edu.cmu.hcii.whyline.ui.components.*;

/**
 * Source file browser for a trace.
 * 
 * @author Andrew J. Ko
 *
 */
public class OutlineUI extends HeadlinedTreePanel<PackageNode> {

	private final Map<String,PackageNode> packages = new HashMap<String,PackageNode>();
	private final Map<JavaSourceFile,FileNode> source = new HashMap<JavaSourceFile,FileNode>();
	private final Map<MethodInfo,MethodNode> methods = new HashMap<MethodInfo,MethodNode>();
	private final Map<Classfile,ClassNode> classes = new HashMap<Classfile,ClassNode>();
	
	public OutlineUI(WhylineUI whylineUI) {
		
		super("source", whylineUI);
		
		WhylineToolbar searchToolbar = new WhylineToolbar(JToolBar.HORIZONTAL);
		searchToolbar.add(new SearchFieldUI(whylineUI));

		add(searchToolbar, BorderLayout.SOUTH);
				
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), 0));

	}
	
	public void showFile(FileInterface file) {

		if(!(file instanceof JavaSourceFile)) return;
		
		final PackageNode p = packages.get(((JavaSourceFile)file).getPackageName());
		final FileNode f = source.get(file);
		
		if(p == null || f == null) {
			clearSelection();
			return;
		}

		show(new TreePath(new Object[] { getRoot(), p, f }), true);
		
	}

	public void showMethod(MethodInfo method) {
		
		if(method.getClassfile().getSourceFile() == null) return;

		final PackageNode p = packages.get(method.getClassfile().getSourceFile().getPackageName());
		final FileNode f = source.get(method.getClassfile().getSourceFile());
		final ClassNode c = classes.get(method.getClassfile());
		final MethodNode m = methods.get(method);

		if(p == null || f == null || c == null || m == null) {
			clearSelection();
			return;
		}

		show(new TreePath(new Object[] { getRoot(), p, f, c, m }), true);
		
	}

	public void addFamiliarSource() {
		
		for(JavaSourceFile file : whylineUI.getTrace().getAllSourceFiles())
			addSourceWithoutUpdating(file);
		updateTree();

	}
		
	public void addSource(JavaSourceFile file) {

		addSourceWithoutUpdating(file);
		updateTree();
		
	}
	
	private void addSourceWithoutUpdating(JavaSourceFile file) {

		if(file == null) return;
		
		if(source.containsKey(file)) return;
		
		String packageName = file.getPackageName();
		PackageNode pack = packages.get(packageName);
		if(pack == null) {
			pack = new PackageNode(packageName);
			packages.put(packageName, pack);
			getRoot().addChild(pack);
		}
		FileNode f = new FileNode(file);
		source.put(file, f);
		pack.addChild(f);
		for(Classfile c : file.getClassfiles()) {
			ClassNode clazz = new ClassNode(c);
			classes.put(c, clazz);
			f.addChild(clazz);
			for(MethodInfo method : c.getDeclaredMethods()) {
				MethodNode m = new MethodNode(method);
				clazz.addChild(m);
				methods.put(method, m);
			}
		}
		
	}

	protected void handleSelection() {

		TreePath path = getSelectionPath();
		
		if(path == null) return;
		Object selection = path.getLastPathComponent();
		
		if(selection instanceof FileNode)
			whylineUI.selectFile(((FileNode)selection).file, true, UI.OUTLINE_UI);
		else if(selection instanceof ClassNode)
			whylineUI.selectClass(((ClassNode)selection).classfile, true, UI.OUTLINE_UI);
		else if(selection instanceof MethodNode)
			whylineUI.selectMethod(((MethodNode)selection).method, true, UI.OUTLINE_UI);

	}

	protected void handleExpansion() {}

	protected boolean isLeaf(Node<?> node) { return node instanceof MethodNode; }
	
}