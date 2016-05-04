package edu.cmu.hcii.whyline.qa;

public class ThreadBlock extends ExplanationBlock {

	private final int threadID;
	
	private int maxDepth = 0;
	private ExplanationBlock deepestBlock;
	
	public ThreadBlock(Answer answer, int threadID) {

		// We don't want to set a block for this, so we pass null.
		super(answer, answer.getTrace().getThreadFirstEventID(threadID));

		// But it does have an event.
		this.threadID = threadID;

	}

	public ThreadBlock getThreadBlock() { return this; }
	
	public int getThreadID() { return threadID; }
	
	protected void updateMaxDepth(ExplanationBlock block) {

		int depth = block.getBlockDepth();
		if(depth > maxDepth) {
			maxDepth = depth;
			deepestBlock = block;
		}
		
	}
	
	public int getMaxDepth() { return maxDepth; }
	
	public ExplanationBlock getDeepestBlock() { return deepestBlock; }
	
}
