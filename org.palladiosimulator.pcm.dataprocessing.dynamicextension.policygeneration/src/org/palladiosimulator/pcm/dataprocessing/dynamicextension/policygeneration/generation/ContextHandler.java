package org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.generation;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import org.palladiosimulator.pcm.dataprocessing.dataprocessing.DataSpecification;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.DynamicSpecification;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.MainLoader.ModelLoader;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.util.subject.Organisation;

public class ContextHandler {
	private DataSpecification dataContainer;
	private DynamicSpecification dynamicContainer;

	public ContextHandler(String pathDynamic, String pathData) {
		var modelloader = new ModelLoader(pathDynamic, pathData);
		this.dataContainer = modelloader.loadDataSpecification();
		this.dynamicContainer = modelloader.loadDynamicModel();

	}

	public void createContext() {
		var typeDefinition =  new TypeDefinition();
		var contextGeneration = new ContextGeneration(dataContainer,dynamicContainer);
		var scenarioDefinition = new ScenarioDefinition();
		try (var writer = new PrintWriter(new File("/dev/null"), Charset.forName("UTF-8"))) {
			var time = System.currentTimeMillis();
			typeDefinition.createTypes(dynamicContainer, dataContainer, writer);
			List<String> listEnsembleNames = contextGeneration.generateRelatedContexts(dataContainer, writer);
			typeDefinition.createRootEnsemble(writer, listEnsembleNames);

			scenarioDefinition.writeScenario(writer, dynamicContainer.getSubjectContainer().getSubject().parallelStream()
					.filter(Organisation.class::isInstance).map(Organisation.class::cast).collect(Collectors.toList()));
			System.out.println(System.currentTimeMillis() - time);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
