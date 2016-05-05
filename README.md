[![No Maintenance Intended](http://unmaintained.tech/badge.svg)](http://unmaintained.tech/)

# Whyline for Java

The Whyline for Java is an interactive debugging tool that lets you ask "why" questions about program output.

To see a demo, check out [the brief YouTube video](https://www.youtube.com/watch?v=t6gVZ-qZ4sI).

## History

This was the primary work of my Ph.D. thesis and amounted to about a year of development, testing, debugging, usability testing, and evaluation in 2007 and 2008, culminating in these publications:

* [Asking and Answering Questions about the Causes of Software Behavior](http://faculty.washington.edu/ajko/papers/Ko2008Dissertation.pdf). Ko, A.J. (2008). Carnegie Mellon University, Dissertation.
* [Finding Causes of Program Output with the Java Whyline](http://dl.acm.org/citation.cfm?id=1518942). Ko, A.J. and Myers, B.A. (2009). ACM Conference on Human Factors in Computing Systems (CHI), 1569-1578.A user interface for debugging that supports why and why not questions that enables developers to localize faults signficantly faster than breakpoint debuggers.
* [Extracting and Answering Why and Why Not Questions about Java Program Output](http://dl.acm.org/citation.cfm?id=1824761). Ko, A.J. and Myers, B.A. (2010). ACM Transactions on Software Engineering and Methodology, 22(2), Article 4. An application of static and dynamic program slicing, precise call graphs, reachability analyses, and value provenance that enables developers to localize faults through why and why not questions.

Because of my academic job search in 2008, my position as a professor at the University of Washington, and the academic culture of doing _new_ work, rather than incrementing on old work, I moved on to other projects. That means that the code has likely experienced serious bitrot and could require significant work to make it executable again.

That said, at the time of writing this readme, it's 2016, eight years later, and I'm on sabbatical. It's about time I open source this thing and let the community more easily access and replicate the work!

## Architecture

The best way to learn the architecture of the tool is to read the articles above. Here are the highest level architectural concepts to get you started:

* Users start the Whyline launcher and select a compiled program to debug
* The Whyline instruments all of the bytecode in the program to record an execution history
* The Whyline launches the instrumented program, recording the history to disk
* After the program halts, the Whyline loads the history, generating a UI to represent the history
* The user can click on program output and code to ask "why" and "why not" questions about the output.
* The Whyline answers questions by peforming static and dynamic analyses on the code and execution history.

I spent some time documenting design rationale in comments in the trickier components (mostly because I have a practice of self-explaining the more complicated elements of implementations in comments), but if there's anything you're having trouble understanding, write me, and I'll patch the code with a comment.

## Support

Unfortunately, because I've long since moved on to other projects, I cannot support this code or develop it further. Fork it, patch it, extend it: do whatever you like with it. It's here for the public good as an archive for future generations of developer tool developers. I'd love to see what you do with it! I love to hear stories about how people are building upon the work.

That said, if you find that things are critically broken and can be fixed with some simple changes, submit a pull request. I'll review all requests eventually and merge them, so that others can continue to play with the code.
