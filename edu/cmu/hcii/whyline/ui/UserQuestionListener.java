
package edu.cmu.hcii.whyline.ui;

import edu.cmu.hcii.whyline.qa.Question;


/**
 * @author Andrew J. Ko
 *
 */
public interface UserQuestionListener {

	public void questionChanged(Question<?> newQuestion);
	
}
