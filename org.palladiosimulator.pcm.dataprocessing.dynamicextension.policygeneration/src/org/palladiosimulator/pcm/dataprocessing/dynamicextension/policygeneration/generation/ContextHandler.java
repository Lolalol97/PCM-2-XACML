package org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.generation;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.pcm.dataprocessing.dataprocessing.DataSpecification;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.DynamicSpecification;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.MainLoader.ModelLoader;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.handlers.SampleHandler;
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
		var typeDefinition = new TypeDefinition();
		var contextGeneration = new ContextGeneration(dataContainer, dynamicContainer);
		var scenarioDefinition = new ScenarioDefinition();
		var globalList = new HashMap<String, Double>();
		try (var writer = new PrintWriter(new File("/dev/null"), Charset.forName("UTF-8"))) {
			long[] kList = { 10, 10, 100, 1000, 10000, 100000, 200000, 400000, 600000, 800000, 1000000, 2000000,
					5000000 };
			for (long k : kList) {
				System.gc();
				var modelloader = new ModelLoader(SampleHandler.PATH_DYNAMIC, SampleHandler.PATH_DATA);
				this.dataContainer = modelloader.loadDataSpecification();
				this.dynamicContainer = modelloader.loadDynamicModel();
//				var x = dataContainer.getRelatedCharacteristics().get(0).getCharacteristics().getOwnedCharacteristics().get(0);
				var x = dataContainer.getRelatedCharacteristics().get(0);
				var list = new ArrayList<Long>();
				for (int i = 0; i < k; i++)
//					dataContainer.getRelatedCharacteristics().get(0).getCharacteristics().getOwnedCharacteristics().add(EcoreUtil.copy(x));
					dataContainer.getRelatedCharacteristics().add(EcoreUtil.copy(x));
				for (int i = 0; i < 10; i++) {
					var time = System.currentTimeMillis();
					typeDefinition.createTypes(dynamicContainer, dataContainer, writer);
					List<String> listEnsembleNames = contextGeneration.generateRelatedContexts(dataContainer, writer);
					typeDefinition.createRootEnsemble(writer, listEnsembleNames);

					scenarioDefinition.writeScenario(writer,
							dynamicContainer.getSubjectContainer().getSubject().parallelStream()
									.filter(Organisation.class::isInstance).map(Organisation.class::cast)
									.collect(Collectors.toList()));
					list.add(System.currentTimeMillis() - time);
				}
				globalList.put(Long.toString(k), list.stream().mapToLong(e -> e.longValue()).average().getAsDouble());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try (var writer = new PrintWriter(new File("/home/majuwa/scalability.csv"), Charset.forName("UTF-8"))) {
			globalList.entrySet().forEach(e -> writer.println(e.getKey() + ";" + e.getValue()));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
