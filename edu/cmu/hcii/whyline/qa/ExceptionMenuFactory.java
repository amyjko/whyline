package edu.cmu.hcii.whyline.qa;

public class ExceptionMenuFactory {

	public static QuestionMenu getQuestionMenuForException(Asker asker, int exceptionEventID) {
		
		QuestionMenu questionMenu = new QuestionMenu(asker, "Questions about exceptions that were thrown.", "questions");
		QuestionMenu whyDidMenu = new QuestionMenu(asker, "Questions about exceptions that were thrown.", "why did...");
		questionMenu.addMenu(whyDidMenu);
		whyDidMenu.addQuestion(new WhyDidEventOccur(asker, exceptionEventID, asker.getTrace().getHTMLDescription(exceptionEventID) + ""));
		
		return questionMenu;
		
	}

}
