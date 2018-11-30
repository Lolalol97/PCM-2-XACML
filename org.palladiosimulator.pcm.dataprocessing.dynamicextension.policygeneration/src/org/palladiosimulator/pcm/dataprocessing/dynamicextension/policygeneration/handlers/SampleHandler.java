package org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.MainLoader.ModelLoader;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.generation.ContextHandler;
import org.eclipse.jface.dialogs.MessageDialog;

public class SampleHandler extends AbstractHandler {
//	private static final String PATH_DYNAMIC = "/home/majuwa/git/Trust4.0/UC-Combined/uc-combined.dynamicextension";
//	private static final String PATH_DATA = "/home/majuwa/git/Trust4.0/UC-Combined/uc-combined.dataprocessing";
//	private static final String PATH_DYNAMIC = "/home/majuwa/git/Trust4.0/UC3/uc3.dynamicextension";
//	private static final String PATH_DATA = "/home/majuwa/git/Trust4.0/UC3/uc3.dataprocessing";
	private static final String PATH_DYNAMIC = "/home/majuwa/git/Trust4.0/UC-Shift/uc-combined.dynamicextension";
	private static final String PATH_DATA = "/home/majuwa/git/Trust4.0/UC-Shift/uc-combined.dataprocessing";
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		var x = new ContextHandler(PATH_DYNAMIC, PATH_DATA);
		x.createContext();
		return null;
	}
}
